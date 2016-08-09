package org.toilelibre.libe.curl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.toilelibre.libe.curl.Curl.CurlException;
import org.toilelibre.libe.curl.pem.PemObject;
import org.toilelibre.libe.curl.pem.PemReader;

enum CertFormat {
    PEM ( (inputStream, passwordAsCharArray) -> {
        try {
            final KeyStore keyStore = KeyStore.getInstance ("JKS");
            final PemReader pemReader = new PemReader (new InputStreamReader (inputStream));
            PKCS8EncodedKeySpec privateKeySpec = null;
            Certificate publicCertificate = null;
            PemObject pemObject;
            while ((pemObject = pemReader.readPemObject ()) != null) {
                switch (pemObject.getType ()) {
                    case "PRIVATE KEY" :
                        privateKeySpec = new PKCS8EncodedKeySpec (pemObject.getContent ());
                        break;
                    case "CERTIFICATE" :
                        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                        publicCertificate = certificateFactory.generateCertificate(new ByteArrayInputStream (pemObject.getContent ()));
                        break;
                    default :
                        break;
                }
            }
            pemReader.close ();

            final KeyFactory keyFactory = KeyFactory.getInstance ("RSA");
            final PrivateKey privateKey = keyFactory.generatePrivate (privateKeySpec);
            
            keyStore.load (null);
            keyStore.setCertificateEntry ("cert-alias", publicCertificate);
            keyStore.setKeyEntry ("key-alias", privateKey, passwordAsCharArray, new Certificate [] { publicCertificate });
            return keyStore;
        } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException | InvalidKeySpecException e) {
            throw new CurlException (e);
        }
    }), P12 ( (inputStream, passwordAsCharArray) -> {
        try {
            final KeyStore keyStore = KeyStore.getInstance ("pkcs12");
            keyStore.load (inputStream, passwordAsCharArray);
            return keyStore;
        } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException e) {
            throw new CurlException (e);
        }
    }), DER ( (inputStream, passwordAsCharArray) -> {
        try {
            return KeyStore.getInstance ("pkcs12");
        } catch (final KeyStoreException e) {
            throw new CurlException (e);
        }
    }), ENG ( (inputStream, passwordAsCharArray) -> {
        try {
            return KeyStore.getInstance ("pkcs12");
        } catch (final KeyStoreException e) {
            throw new CurlException (e);
        }
    });

    @FunctionalInterface
    static interface KeystoreFromFileGenerator {
        KeyStore generate (InputStream inputStream, char [] passwordAsCharArray);
    }

    private KeystoreFromFileGenerator generator;

    CertFormat (final KeystoreFromFileGenerator generator1) {
        this.generator = generator1;
    }

    public KeystoreFromFileGenerator getGenerator () {
        return this.generator;
    }

    public KeyStore generateKeyStoreFromFileAndPassword (final InputStream inputStream, final char [] passwordAsCharArray) {
        return this.generator.generate (inputStream, passwordAsCharArray);
    }
}
