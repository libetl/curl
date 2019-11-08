package org.toilelibre.libe.curl;

import org.toilelibre.libe.curl.Curl.*;
import org.toilelibre.libe.curl.DerReader.*;
import org.toilelibre.libe.curl.PemReader.*;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.spec.*;
import java.util.*;
import java.util.logging.*;

enum CertFormat {

    DER ( (kind, content, passwordAsCharArray) -> {
        try {
            if (kind == Kind.CERTIFICATE) {
                final CertificateFactory certificateFactory = CertificateFactory.getInstance ("X.509");
                return Collections.singletonList (certificateFactory.generateCertificate (new ByteArrayInputStream (content)));
            }
            if (kind == Kind.PRIVATE_KEY) {
                final DerReader derReader = new DerReader (content);
                final Asn1Object asn1 = derReader.read ();
                final KeyFactory keyFactory = KeyFactory.getInstance ("RSA");
                final KeySpec keySpec = asn1.getKeySpec ();
                return Collections.singletonList (keyFactory.generatePrivate (keySpec));
            }
            return null;
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            throw new CurlException (e);
        }
    }), ENG ( (content, kind, passwordAsCharArray) -> {
        try {
            return KeyStore.getInstance ("pkcs12");
        } catch (final KeyStoreException e) {
            throw new CurlException (e);
        }
    }), JKS ( (kind, content, passwordAsCharArray) -> {
        try {
            return CertFormat.readFromKeystoreType ("jks", content, kind, passwordAsCharArray);
        } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException | UnrecoverableKeyException e) {
            throw new CurlException (e);
        }
    }), P12 ( (kind, content, passwordAsCharArray) -> {
        try {
            return CertFormat.readFromKeystoreType ("pkcs12", content, kind, passwordAsCharArray);
        } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException | UnrecoverableKeyException e) {
            throw new CurlException (e);
        }
    }), PEM ( (kind, content, passwordAsCharArray) -> {
        PemReader pemReader = null;
        final List<Object> result = new ArrayList<> ();
        try {
            pemReader = new PemReader (new InputStreamReader (new ByteArrayInputStream (content)));
            PKCS8EncodedKeySpec privateKeySpec = null;
            PemObject pemObject;
            while ((pemObject = pemReader.readPemObject ()) != null) {
                final Kind readKind = Kind.fromValue (pemObject.getType ());
                if (kind != readKind) {
                    continue;
                }
                switch (kind) {
                    case PRIVATE_KEY :
                        privateKeySpec = new PKCS8EncodedKeySpec (pemObject.getContent ());
                        final KeyFactory keyFactory = KeyFactory.getInstance ("RSA");
                        result.add (keyFactory.generatePrivate (privateKeySpec));
                        break;
                    case CERTIFICATE :
                        final CertificateFactory certificateFactory = CertificateFactory.getInstance ("X.509");
                        result.add (certificateFactory.generateCertificate (new ByteArrayInputStream (pemObject.getContent ())));
                        break;
                    default:
                        break;
                }
            }
            return result;
        } catch (NoSuchAlgorithmException | CertificateException | IOException | InvalidKeySpecException e) {
            throw new CurlException (e);
        } finally {
            if (pemReader != null) {
                try {
                    pemReader.close ();
                } catch (final IOException e) {
                    logProblemWithPemReader (e);
                }
            }
        }
    });

    private static Logger LOGGER = Logger.getLogger (AfterResponse.class.getName ());
    private KeystoreFromFileGenerator generator;

    CertFormat (final KeystoreFromFileGenerator generator1) {
        this.generator = generator1;
    }

    @SuppressWarnings ("unchecked")
    <T> List<T> generateCredentialsFromFileAndPassword (final Kind kind, final byte [] content, final char [] passwordAsCharArray) {
        return (List<T>) this.generator.generate (kind, content, passwordAsCharArray);
    }

    private static void logProblemWithPemReader (IOException e) {
        LOGGER.log (Level.WARNING, "Problem with PEM reader", e);
    }

    @FunctionalInterface
    interface KeystoreFromFileGenerator {
        Object generate (Kind kind, byte [] content, char [] passwordAsCharArray);
    }

    enum Kind {
        CERTIFICATE, PRIVATE_KEY;
        static Kind fromValue (final String value) {
            try {
                return Kind.valueOf (value.toUpperCase ().replace (' ', '_'));
            } catch (final IllegalArgumentException iae) {
                return null;
            }
        }
    }

    private static List<Object> readFromKeystoreType (final String type, final byte [] content, final Kind kind, final char [] passwordAsCharArray) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
        final KeyStore keyStore = KeyStore.getInstance (type);
        keyStore.load (new ByteArrayInputStream (content), passwordAsCharArray);
        final Enumeration<String> aliases = keyStore.aliases ();
        final List<Object> result = new ArrayList<> ();
        while (aliases.hasMoreElements ()) {
            final String alias = aliases.nextElement ();
            if ((keyStore.getCertificate (alias) != null) && (kind == Kind.CERTIFICATE)) {
                result.add (keyStore.getCertificate (alias));
            }
            if ((keyStore.getKey (alias, passwordAsCharArray) != null) && (kind == Kind.PRIVATE_KEY)) {
                result.add (keyStore.getKey (alias, passwordAsCharArray));
            }
        }
        return result;
    }
}
