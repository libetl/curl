package org.toilelibre.libe.curl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.toilelibre.libe.curl.Curl.CurlException;
import org.toilelibre.libe.curl.DerReader.Asn1Object;
import org.toilelibre.libe.curl.PemReader.PemObject;

enum CertFormat {

	PEM((kind, content, passwordAsCharArray) -> {
		PemReader pemReader = null;
		List<Object> result = new ArrayList<>();
		try {
			pemReader = new PemReader(new InputStreamReader (new ByteArrayInputStream(content)));
			PKCS8EncodedKeySpec privateKeySpec = null;
			PemObject pemObject;
			while ((pemObject = pemReader.readPemObject()) != null) {
				Kind readKind = Kind.fromValue(pemObject.getType());
				if (kind != readKind)
					continue;
				switch (kind) {
				case PRIVATE_KEY:
					privateKeySpec = new PKCS8EncodedKeySpec(pemObject.getContent());
					final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
					result.add(keyFactory.generatePrivate(privateKeySpec));
					break;
				case CERTIFICATE:
					final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
					result.add(certificateFactory.generateCertificate(new ByteArrayInputStream(pemObject.getContent())));
					break;
				default:break;
				}
			}
			return result;
		} catch (NoSuchAlgorithmException | CertificateException | IOException | InvalidKeySpecException e) {
			throw new CurlException(e);
		} finally {
			if (pemReader != null)
				try {
					pemReader.close();
				} catch (IOException e) {
				}
		}
	}), P12((kind, content, passwordAsCharArray) -> {
		try {
			return readFromKeystoreType ("pkcs12", content, kind, passwordAsCharArray);
		} catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException
				| UnrecoverableKeyException e) {
			throw new CurlException(e);
		}
	}), JKS((kind, content, passwordAsCharArray) -> {
		try {
			return readFromKeystoreType ("jks", content, kind, passwordAsCharArray);
		} catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException
				| UnrecoverableKeyException e) {
			throw new CurlException(e);
		}
	}), DER((kind, content, passwordAsCharArray) -> {		
		try {
			if (kind == Kind.CERTIFICATE){
				CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
				return Arrays.asList(certificateFactory.generateCertificate(new ByteArrayInputStream (content)));
			}
			if (kind == Kind.PRIVATE_KEY){
                DerReader derReader = new DerReader (content);
                Asn1Object asn1 = derReader.read();
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                KeySpec keySpec = asn1.getKeySpec();
				return Arrays.asList(keyFactory.generatePrivate(keySpec));
			}
		    return null;
	} catch (CertificateException | NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
		throw new CurlException(e);
	}
	}), ENG((content, kind, passwordAsCharArray) -> {
		try {
			return KeyStore.getInstance("pkcs12");
		} catch (final KeyStoreException e) {
			throw new CurlException(e);
		}
	});

	@FunctionalInterface
	interface KeystoreFromFileGenerator {
		Object generate(Kind kind, byte[] content, char[] passwordAsCharArray);
	}

	enum Kind {
		PRIVATE_KEY, CERTIFICATE;
		static Kind fromValue(String value) {
			try {
				return Kind.valueOf(value.toUpperCase().replace(' ', '_'));
			} catch (IllegalArgumentException iae) {
				return null;
			}
		}
	}

	private KeystoreFromFileGenerator generator;

	CertFormat(final KeystoreFromFileGenerator generator1) {
		this.generator = generator1;
	}

	private static List<Object> readFromKeystoreType(String type, byte[] content, Kind kind, char[] passwordAsCharArray) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
		final KeyStore keyStore = KeyStore.getInstance(type);
		keyStore.load(new ByteArrayInputStream (content), passwordAsCharArray);
		Enumeration<String> aliases = keyStore.aliases();
		List<Object> result = new ArrayList<>();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			if (keyStore.getCertificate(alias) != null && kind == Kind.CERTIFICATE) {
				result.add(keyStore.getCertificate(alias));
			}
			if (keyStore.getKey(alias, passwordAsCharArray) != null && kind == Kind.PRIVATE_KEY) {
				result.add(keyStore.getKey(alias, passwordAsCharArray));
			}
		}
		return result;
	}

	public KeystoreFromFileGenerator getGenerator() {
		return this.generator;
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> generateCredentialsFromFileAndPassword(final Kind kind, final byte[] content,
			final char[] passwordAsCharArray) {
		return (List<T>) this.generator.generate(kind, content, passwordAsCharArray);
	}
}
