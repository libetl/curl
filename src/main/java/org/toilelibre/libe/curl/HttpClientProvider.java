package org.toilelibre.libe.curl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.security.cert.CertificateException;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.toilelibre.libe.curl.CertFormat.Kind;
import org.toilelibre.libe.curl.Curl.CurlException;

final class HttpClientProvider {

    static HttpClient prepareHttpClient (final CommandLine commandLine) throws CurlException {
        HttpClientBuilder executor = HttpClientBuilder.create ();

        final String hostname;
        try {
            hostname = InetAddress.getLocalHost ().getHostName ();
        } catch (final UnknownHostException e1) {
            throw new RuntimeException (e1);
        }

        executor = HttpClientProvider.handleAuthMethod (commandLine, executor, hostname);

        if (!commandLine.hasOption (Arguments.FOLLOW_REDIRECTS.getOpt ())) {
            executor.disableRedirectHandling ();
        }

        HttpClientProvider.handleSSLParams (commandLine, executor);
        return executor.build ();
    }

    private static void addClientCredentials (final SSLContextBuilder builder, final CertFormat certFormat, final String caCertFilePath, final String certFilePath, final CertFormat keyFormat, final String keyFilePath, final String password) throws CurlException {
        try {
            final File caCertFileObject = caCertFilePath == null ? null : HttpClientProvider.getFile (caCertFilePath);
            final File certFileObject = HttpClientProvider.getFile (certFilePath);
            final File keyFileObject = HttpClientProvider.getFile (keyFilePath);
            final KeyStore keyStore = HttpClientProvider.generateKeyStore (certFormat, caCertFileObject, certFileObject, keyFormat, keyFileObject, password == null ? null : password.toCharArray ());
            builder.loadKeyMaterial (keyStore, password == null ? null : password.toCharArray ());
        } catch (GeneralSecurityException | IOException | CertificateException e) {
            throw new CurlException (e);
        }
    }

    private static KeyStore generateKeyStore (final CertFormat certFormat, final File caCertFileObject, final File certFileObject, final CertFormat keyFormat, final File keyFileObject, final char [] passwordAsCharArray)
            throws KeyStoreException, NoSuchAlgorithmException, java.security.cert.CertificateException, FileNotFoundException, IOException, CurlException, CertificateException {
        final List<Certificate> caCertificatesNotFiltered = caCertFileObject == null ? Collections.emptyList () : certFormat.generateCredentialsFromFileAndPassword (Kind.CERTIFICATE, IOUtils.toByteArray (caCertFileObject), passwordAsCharArray);
        final List<Certificate> caCertificatesFiltered = caCertificatesNotFiltered.stream ().filter ( (certificate) -> (certificate instanceof X509Certificate) && (((X509Certificate) certificate).getBasicConstraints () != -1)).collect (Collectors.toList ());
        final List<Certificate> certificates = certFormat.generateCredentialsFromFileAndPassword (Kind.CERTIFICATE, IOUtils.toByteArray (certFileObject), passwordAsCharArray);
        final List<PrivateKey> privateKeys = keyFormat.generateCredentialsFromFileAndPassword (Kind.PRIVATE_KEY, IOUtils.toByteArray (keyFileObject), passwordAsCharArray);

        final KeyStore keyStore = KeyStore.getInstance ("JKS");
        keyStore.load (null);
        final Certificate [] certificatesAsArray = certificates.toArray (new Certificate [certificates.size ()]);
        IntStream.range (0, certificates.size ()).forEach (i -> HttpClientProvider.setCertificateEntry (keyStore, certificates, i));
        IntStream.range (0, caCertificatesFiltered.size ()).forEach (i -> HttpClientProvider.setCaCertificateEntry (keyStore, certificates, i));
        IntStream.range (0, privateKeys.size ()).forEach (i -> HttpClientProvider.setPrivateKeyEntry (keyStore, privateKeys, passwordAsCharArray, certificatesAsArray, i));
        return keyStore;

    }

    private static File getFile (final String filePath) {
        final File file = new File (filePath);
        if (file.exists ()) {
            return file;
        }
        return new File (System.getProperty ("user.dir") + File.separator + filePath);
    }

    private static HttpClientBuilder handleAuthMethod (final CommandLine commandLine, HttpClientBuilder executor, final String hostname) {
        if (commandLine.getOptionValue (Arguments.AUTH.getOpt ()) != null) {
            final String [] authValue = commandLine.getOptionValue (Arguments.AUTH.getOpt ()).split ("(?<!\\\\):");
            if (commandLine.hasOption (Arguments.NTLM.getOpt ())) {
                final String [] userName = authValue [0].split ("\\\\");
                final SystemDefaultCredentialsProvider systemDefaultCredentialsProvider = new SystemDefaultCredentialsProvider ();
                systemDefaultCredentialsProvider.setCredentials (AuthScope.ANY, new NTCredentials (userName [1], authValue [1], hostname, userName [0]));
                executor = executor.setDefaultCredentialsProvider (systemDefaultCredentialsProvider);
            } else {
                final BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider ();
                basicCredentialsProvider.setCredentials (new AuthScope (HttpHost.create (URI.create (commandLine.getArgs () [0]).getHost ())), new UsernamePasswordCredentials (authValue [0], authValue [1]));
                executor = executor.setDefaultCredentialsProvider (basicCredentialsProvider);
            }
        }
        return executor;
    }

    private static void handleSSLParams (final CommandLine commandLine, final HttpClientBuilder executor) throws CurlException {
        final SSLContextBuilder builder = new SSLContextBuilder ();
        builder.useProtocol (HttpClientProvider.protocolFromCommandLine (commandLine));

        if (commandLine.hasOption (Arguments.TRUST_INSECURE.getOpt ())) {
            HttpClientProvider.sayTrustInsecure (builder);
        }
        final CertFormat certFormat = commandLine.hasOption (Arguments.CERT_TYPE.getOpt ()) ? CertFormat.valueOf (commandLine.getOptionValue (Arguments.CERT_TYPE.getOpt ()).toUpperCase ()) : CertFormat.PEM;
        final CertFormat keyFormat = commandLine.hasOption (Arguments.KEY.getOpt ()) ? commandLine.hasOption (Arguments.KEY_TYPE.getOpt ()) ? CertFormat.valueOf (commandLine.getOptionValue (Arguments.KEY_TYPE.getOpt ()).toUpperCase ()) : CertFormat.PEM : certFormat;

        if (commandLine.hasOption (Arguments.CERT.getOpt ())) {
            final String [] credentials = commandLine.getOptionValue (Arguments.CERT.getOpt ()).split (":");
            final String cacert = commandLine.getOptionValue (Arguments.CA_CERT.getOpt ()) == null ? null : commandLine.getOptionValue (Arguments.CA_CERT.getOpt ());
            final String key = commandLine.getOptionValue (Arguments.KEY.getOpt ()) == null ? credentials [0] : commandLine.getOptionValue (Arguments.KEY.getOpt ());
            HttpClientProvider.addClientCredentials (builder, certFormat, cacert, credentials [0], keyFormat, key, credentials.length > 1 ? credentials [1] : "");
        }

        try {
            final SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory (builder.build ());

            executor.setSSLSocketFactory (sslSocketFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new CurlException (e);
        }
    }

    private static String protocolFromCommandLine (final CommandLine commandLine) {
        if (commandLine.hasOption (Arguments.TLS_V1.getOpt ())) {
            return "TLSv1";
        }
        if (commandLine.hasOption (Arguments.TLS_V10.getOpt ())) {
            return "TLSv1.0";
        }
        if (commandLine.hasOption (Arguments.TLS_V11.getOpt ())) {
            return "TLSv1.1";
        }
        if (commandLine.hasOption (Arguments.TLS_V12.getOpt ())) {
            return "TLSv1.2";
        }
        if (commandLine.hasOption (Arguments.SSL_V2.getOpt ())) {
            return "SSLv2";
        }
        if (commandLine.hasOption (Arguments.SSL_V3.getOpt ())) {
            return "SSLv3";
        }
        return "TLS";
    }

    private static void sayTrustInsecure (final SSLContextBuilder builder) throws CurlException {
        try {
            builder.loadTrustMaterial (null, new TrustSelfSignedStrategy ());
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new CurlException (e);
        }

    }

    private static void setCaCertificateEntry (final KeyStore keyStore, final List<Certificate> certificates, final int i) {
        try {
            keyStore.setCertificateEntry ("ca-cert-alias-" + i, certificates.get (i));
        } catch (final KeyStoreException e) {
            throw new CurlException (e);
        }
    }

    private static void setCertificateEntry (final KeyStore keyStore, final List<Certificate> certificates, final int i) {
        try {
            keyStore.setCertificateEntry ("cert-alias-" + i, certificates.get (i));
        } catch (final KeyStoreException e) {
            throw new CurlException (e);
        }
    }

    private static void setPrivateKeyEntry (final KeyStore keyStore, final List<PrivateKey> privateKeys, final char [] passwordAsCharArray, final Certificate [] certificatesAsArray, final int i) {
        try {
            keyStore.setKeyEntry ("key-alias-" + i, privateKeys.get (i), passwordAsCharArray, certificatesAsArray);
        } catch (final KeyStoreException e) {
            throw new CurlException (e);
        }
    }
}
