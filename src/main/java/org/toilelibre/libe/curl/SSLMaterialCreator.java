package org.toilelibre.libe.curl;

import org.apache.commons.cli.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.*;
import org.apache.http.ssl.SSLContextBuilder;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.*;
import java.util.*;
import java.util.stream.*;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;
import static org.apache.http.conn.ssl.SSLConnectionSocketFactory.*;
import static org.toilelibre.libe.curl.Arguments.*;
import static org.toilelibre.libe.curl.IOUtils.*;

final class SSLMaterialCreator {

    private final static Map<Map<String, List<String>>, SSLConnectionSocketFactory> cachedSSLFactoriesForPerformance =
            new HashMap<> ();

    static void handleSSLParams (final CommandLine commandLine, final HttpClientBuilder executor) throws Curl.CurlException {

        Map<String, List<String>> input = inputExtractedFrom (commandLine);
        final SSLConnectionSocketFactory foundInCache = cachedSSLFactoriesForPerformance.get (input);

        if (foundInCache != null) {
            executor.setSSLSocketFactory (foundInCache);
            return;
        }

        final SSLContextBuilder builder = new SSLContextBuilder ();
        builder.setProtocol (protocolFromCommandLine (commandLine));

        if (commandLine.hasOption (TRUST_INSECURE.getOpt ())) {
            sayTrustInsecure (builder);
        }

        final CertFormat certFormat = commandLine.hasOption (CERT_TYPE.getOpt ()) ?
                CertFormat.valueOf (commandLine.getOptionValue (CERT_TYPE.getOpt ()).toUpperCase ()) :
                CertFormat.PEM;
        final SSLMaterialCreator.CertPlusKeyInfo.Builder certAndKeysBuilder =
                SSLMaterialCreator.CertPlusKeyInfo.newBuilder ()
                        .cacert (commandLine.getOptionValue (CA_CERT.getOpt ()))
                        .certFormat (certFormat)
                        .keyFormat (commandLine.hasOption (KEY.getOpt ()) ?
                                commandLine.hasOption (KEY_TYPE.getOpt ()) ?
                                        CertFormat.valueOf (commandLine.getOptionValue (KEY_TYPE.getOpt ()).toUpperCase ()) : CertFormat.PEM : certFormat);


        if (commandLine.hasOption (CERT.getOpt ())) {
            final String entireOption = commandLine.getOptionValue (CERT.getOpt ());
            final int certSeparatorIndex = getSslSeparatorIndex (entireOption);
            final String cert = certSeparatorIndex == - 1 ? entireOption : entireOption.substring (0, certSeparatorIndex);
            certAndKeysBuilder.cert (cert)
                    .certPassphrase (certSeparatorIndex == - 1 ? "" : entireOption.substring (certSeparatorIndex + 1))
                    .key (cert);
        }

        if (commandLine.hasOption (KEY.getOpt ())) {
            final String entireOption = commandLine.getOptionValue (KEY.getOpt ());
            final int keySeparatorIndex = getSslSeparatorIndex (entireOption);
            final String key = keySeparatorIndex == - 1 ? entireOption : entireOption.substring (0, keySeparatorIndex);
            certAndKeysBuilder.key (key)
                    .keyPassphrase (keySeparatorIndex == - 1 ? "" : entireOption.substring (keySeparatorIndex + 1));
        }
        if (commandLine.hasOption (CERT.getOpt ()) || commandLine.hasOption (KEY.getOpt ())) {
            addClientCredentials (builder, certAndKeysBuilder.build ());
        }

        try {
            final SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory (builder.build (),
                    commandLine.hasOption (TRUST_INSECURE.getOpt ()) ? NoopHostnameVerifier.INSTANCE :
                            getDefaultHostnameVerifier ());
            cachedSSLFactoriesForPerformance.put (input, sslSocketFactory);
            executor.setSSLSocketFactory (sslSocketFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new Curl.CurlException (e);
        }
    }

    private static Map<String, List<String>> inputExtractedFrom (CommandLine commandLine) {
        return Stream.of (TRUST_INSECURE, CERT_TYPE, CA_CERT, KEY, KEY_TYPE,
                CERT, TLS_V1, TLS_V10, TLS_V11, TLS_V12, SSL_V2, SSL_V3)
                .filter (option ->
                            commandLine.getOptionValues (option.getOpt ()) != null ||
                                    commandLine.hasOption (option.getOpt ())
                )
                .collect (toMap (Option::getOpt, option ->
                        asList(ofNullable(commandLine.getOptionValues (option.getOpt ()))
                        .orElse (new String[] {"true"}))));
    }

    private static void addClientCredentials (final SSLContextBuilder builder,
                                              final SSLMaterialCreator.CertPlusKeyInfo certPlusKeyInfo) throws Curl.CurlException {
        try {
            final String keyPassword = certPlusKeyInfo.getKeyPassphrase () == null ?
                    certPlusKeyInfo.getCertPassphrase () : certPlusKeyInfo.getKeyPassphrase ();
            final KeyStore keyStore = generateKeyStore (certPlusKeyInfo);
            builder.loadKeyMaterial (keyStore, keyPassword == null ? null : keyPassword.toCharArray ());
        } catch (GeneralSecurityException | IOException e) {
            throw new Curl.CurlException (e);
        }
    }

    private static KeyStore generateKeyStore (final SSLMaterialCreator.CertPlusKeyInfo certPlusKeyInfo)
            throws KeyStoreException, NoSuchAlgorithmException, java.security.cert.CertificateException, IOException,
            Curl.CurlException {
        final CertFormat certFormat = certPlusKeyInfo.getCertFormat ();
        final File caCertFileObject = certPlusKeyInfo.getCacert () == null ? null : getFile (certPlusKeyInfo.getCacert ());
        final File certFileObject = getFile (certPlusKeyInfo.getCert ());
        final CertFormat keyFormat = certPlusKeyInfo.getKeyFormat ();
        final File keyFileObject = getFile (certPlusKeyInfo.getKey ());
        final char[] certPasswordAsCharArray = certPlusKeyInfo.getCertPassphrase () == null ? null :
                certPlusKeyInfo.getCertPassphrase ().toCharArray ();
        final char[] keyPasswordAsCharArray = certPlusKeyInfo.getKeyPassphrase () == null ? certPasswordAsCharArray :
                certPlusKeyInfo.getKeyPassphrase ().toCharArray ();
        final List<java.security.cert.Certificate> caCertificatesNotFiltered = caCertFileObject == null ?
                Collections.emptyList () :
                certFormat.generateCredentialsFromFileAndPassword (CertFormat.Kind.CERTIFICATE,
                        IOUtils.toByteArray (caCertFileObject), keyPasswordAsCharArray);
        final List<java.security.cert.Certificate> caCertificatesFiltered =
                caCertificatesNotFiltered.stream ().filter ((certificate) -> (certificate instanceof X509Certificate) && (((X509Certificate) certificate).getBasicConstraints () != - 1)).collect (toList ());
        final List<java.security.cert.Certificate> certificates =
                certFormat.generateCredentialsFromFileAndPassword (CertFormat.Kind.CERTIFICATE,
                        IOUtils.toByteArray (certFileObject), certPasswordAsCharArray);
        final List<PrivateKey> privateKeys =
                keyFormat.generateCredentialsFromFileAndPassword (CertFormat.Kind.PRIVATE_KEY,
                        IOUtils.toByteArray (keyFileObject), keyPasswordAsCharArray);

        final KeyStore keyStore = KeyStore.getInstance ("JKS");
        keyStore.load (null);
        final java.security.cert.Certificate[] certificatesAsArray =
                certificates.toArray (new java.security.cert.Certificate[0]);
        IntStream.range (0, certificates.size ()).forEach (i -> setCertificateEntry (keyStore, certificates, i));
        IntStream.range (0, caCertificatesFiltered.size ()).forEach (i -> setCaCertificateEntry (keyStore,
                certificates, i));
        IntStream.range (0, privateKeys.size ()).forEach (i -> setPrivateKeyEntry (keyStore, privateKeys,
                keyPasswordAsCharArray, certificatesAsArray, i));
        return keyStore;
    }

    private static int getSslSeparatorIndex (String entireOption) {
        return entireOption.matches ("^[A-Za-z]:\\\\") && entireOption.lastIndexOf (':') == 1 ? - 1 :
                entireOption.lastIndexOf (':');
    }

    private static String protocolFromCommandLine (final CommandLine commandLine) {
        if (commandLine.hasOption (TLS_V1.getOpt ())) {
            return "TLSv1";
        }
        if (commandLine.hasOption (TLS_V10.getOpt ())) {
            return "TLSv1.0";
        }
        if (commandLine.hasOption (TLS_V11.getOpt ())) {
            return "TLSv1.1";
        }
        if (commandLine.hasOption (TLS_V12.getOpt ())) {
            return "TLSv1.2";
        }
        if (commandLine.hasOption (SSL_V2.getOpt ())) {
            return "SSLv2";
        }
        if (commandLine.hasOption (SSL_V3.getOpt ())) {
            return "SSLv3";
        }
        return "TLS";
    }

    private static void sayTrustInsecure (final SSLContextBuilder builder) throws Curl.CurlException {
        try {
            builder.loadTrustMaterial (null, (chain, authType) -> true);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new Curl.CurlException (e);
        }
    }

    private static void setCaCertificateEntry (final KeyStore keyStore,
                                               final List<java.security.cert.Certificate> certificates, final int i) {
        try {
            keyStore.setCertificateEntry ("ca-cert-alias-" + i, certificates.get (i));
        } catch (final KeyStoreException e) {
            throw new Curl.CurlException (e);
        }
    }

    private static void setCertificateEntry (final KeyStore keyStore,
                                             final List<java.security.cert.Certificate> certificates, final int i) {
        try {
            keyStore.setCertificateEntry ("cert-alias-" + i, certificates.get (i));
        } catch (final KeyStoreException e) {
            throw new Curl.CurlException (e);
        }
    }

    private static void setPrivateKeyEntry (final KeyStore keyStore, final List<PrivateKey> privateKeys,
                                            final char[] passwordAsCharArray, final Certificate[] certificatesAsArray
            , final int i) {
        try {
            keyStore.setKeyEntry ("key-alias-" + i, privateKeys.get (i), passwordAsCharArray, certificatesAsArray);
        } catch (final KeyStoreException e) {
            throw new Curl.CurlException (e);
        }
    }

    static class CertPlusKeyInfo {

        private final CertFormat certFormat;
        private final CertFormat keyFormat;
        private final String cert;
        private final String certPassphrase;
        private final String cacert;
        private final String key;
        private final String keyPassphrase;

        private CertPlusKeyInfo (Builder builder) {
            certFormat = builder.certFormat;
            keyFormat = builder.keyFormat;
            cert = builder.cert;
            certPassphrase = builder.certPassphrase;
            cacert = builder.cacert;
            key = builder.key;
            keyPassphrase = builder.keyPassphrase;
        }

        static Builder newBuilder () {
            return new Builder ();
        }

        CertFormat getCertFormat () {
            return certFormat;
        }

        CertFormat getKeyFormat () {
            return keyFormat;
        }

        String getCert () {
            return cert;
        }

        String getCertPassphrase () {
            return certPassphrase;
        }

        String getCacert () {
            return cacert;
        }

        String getKey () {
            return key;
        }

        String getKeyPassphrase () {
            return keyPassphrase;
        }


        static final class Builder {
            private CertFormat certFormat;
            private CertFormat keyFormat;
            private String cert;
            private String certPassphrase;
            private String cacert;
            private String key;
            private String keyPassphrase;

            private Builder () {}

            Builder certFormat (CertFormat val) {
                certFormat = val;
                return this;
            }

            Builder keyFormat (CertFormat val) {
                keyFormat = val;
                return this;
            }

            Builder cert (String val) {
                cert = val;
                return this;
            }

            Builder certPassphrase (String val) {
                certPassphrase = val;
                return this;
            }

            Builder cacert (String val) {
                cacert = val;
                return this;
            }

            Builder key (String val) {
                key = val;
                return this;
            }

            Builder keyPassphrase (String val) {
                keyPassphrase = val;
                return this;
            }

            CertPlusKeyInfo build () {
                return new CertPlusKeyInfo (this);
            }
        }
    }
}
