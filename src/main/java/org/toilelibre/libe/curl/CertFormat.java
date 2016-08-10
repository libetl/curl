package org.toilelibre.libe.curl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Enumeration;

import org.toilelibre.libe.curl.Curl.CurlException;
import org.toilelibre.libe.curl.PemReader.PemObject;

enum CertFormat {

	PEM((inputStream, kind, passwordAsCharArray) -> {
		PemReader pemReader = null;
		try {
			pemReader = new PemReader(new InputStreamReader(inputStream));
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
					return keyFactory.generatePrivate(privateKeySpec);
				case CERTIFICATE:
					final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
					return certificateFactory.generateCertificate(new ByteArrayInputStream(pemObject.getContent()));
				default:
					return null;
				}
			}
			return null;
		} catch (NoSuchAlgorithmException | CertificateException | IOException | InvalidKeySpecException e) {
			throw new CurlException(e);
		} finally {
			if (pemReader != null)
				try {
					pemReader.close();
				} catch (IOException e) {
				}
		}
	}), P12((inputStream, kind, passwordAsCharArray) -> {
		try {
			final KeyStore keyStore = KeyStore.getInstance("pkcs12");
			keyStore.load(inputStream, passwordAsCharArray);
			Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				if (keyStore.getCertificate(alias) != null && kind == Kind.CERTIFICATE) {
					return keyStore.getCertificate(alias);
				}
				if (keyStore.getKey(alias, passwordAsCharArray) != null && kind == Kind.PRIVATE_KEY) {
					return keyStore.getKey(alias, passwordAsCharArray);
				}
			}
			return keyStore;
		} catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException
				| UnrecoverableKeyException e) {
			throw new CurlException(e);
		}
	}), DER((inputStream, kind, passwordAsCharArray) -> {
		try {
			return KeyStore.getInstance("pkcs12");
		} catch (final KeyStoreException e) {
			throw new CurlException(e);
		}
	}), ENG((inputStream, kind, passwordAsCharArray) -> {
		try {
			return KeyStore.getInstance("pkcs12");
		} catch (final KeyStoreException e) {
			throw new CurlException(e);
		}
	});

	@FunctionalInterface
	interface KeystoreFromFileGenerator {
		Object generate(InputStream inputStream, Kind kind, char[] passwordAsCharArray);
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

	public KeystoreFromFileGenerator getGenerator() {
		return this.generator;
	}

	@SuppressWarnings("unchecked")
	public <T> T generateCredentialsFromFileAndPassword(final InputStream inputStream, final Kind kind,
			final char[] passwordAsCharArray) {
		return (T) this.generator.generate(inputStream, kind, passwordAsCharArray);
	}
}
