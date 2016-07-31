package org.toilelibre.libe.curl;

import java.io.File;
import java.io.FileInputStream;
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

    private static void handleSSLParams (final CommandLine commandLine, final HttpClientBuilder executor) throws CurlException {
        final SSLContextBuilder builder = new SSLContextBuilder ();

        if (commandLine.hasOption (Arguments.TRUST_INSECURE.getOpt ())) {
            HttpClientProvider.sayTrustInsecure (builder);
        }
        final CertFormat format = commandLine.hasOption (Arguments.CERT_TYPE.getOpt ()) ? CertFormat.valueOf (commandLine.getOptionValue (Arguments.CERT_TYPE.getOpt ()).toUpperCase ()) : CertFormat.PEM;

        if (commandLine.hasOption (Arguments.CERT.getOpt ())) {
            final String [] credentials = commandLine.getOptionValue (Arguments.CERT.getOpt ()).split (":");
            HttpClientProvider.addClientCredentials (builder, format, credentials [0], credentials.length > 1 ? credentials [1] : null);
        }

        try {
            final SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory (builder.build ());
            executor.setSSLSocketFactory (sslSocketFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new CurlException (e);
        }
    }

    private static void addClientCredentials (final SSLContextBuilder builder, final CertFormat certFormat, final String filePath, final String password) throws CurlException {
        try {
            final File fileObject = HttpClientProvider.getFile (filePath);
            final KeyStore keyStore = HttpClientProvider.generateKeyStore (certFormat, fileObject, password == null ? null : password.toCharArray ());
            builder.loadKeyMaterial (keyStore, password == null ? null : password.toCharArray ());
        } catch (GeneralSecurityException | IOException | CertificateException e) {
            throw new CurlException (e);
        }
    }

    private static KeyStore generateKeyStore (final CertFormat certFormat, final File fileObject, final char [] passwordAsCharArray) throws KeyStoreException, NoSuchAlgorithmException, java.security.cert.CertificateException, FileNotFoundException, IOException, CurlException, CertificateException {
        return certFormat.generateKeyStoreFromFileAndPassword (new FileInputStream (fileObject), passwordAsCharArray);
    }

    private static File getFile (final String filePath) {
        final File file = new File (filePath);
        if (file.exists ()) {
            return file;
        }
        return new File (System.getProperty ("user.dir") + File.separator + filePath);
    }

    private static void sayTrustInsecure (final SSLContextBuilder builder) throws CurlException {
        try {
            builder.loadTrustMaterial (null, new TrustSelfSignedStrategy ());
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new CurlException (e);
        }

    }

    private static HttpClientBuilder handleAuthMethod (final CommandLine commandLine, HttpClientBuilder executor, final String hostname) {
        if (commandLine.getOptionValue (Arguments.AUTH.getOpt ()) != null) {
            final String [] authValue = commandLine.getOptionValue (Arguments.AUTH.getOpt ()).toString ().split ("(?<!\\\\):");
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
}
