package org.toilelibre.libe.curl;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;

final class HttpClientProvider {

    static HttpClient prepareHttpClient(final CommandLine commandLine) throws CurlException {
        HttpClientBuilder executor = HttpClientBuilder.create();

        final String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e1) {
            throw new RuntimeException(e1);
        }

        executor = HttpClientProvider.handleAuthMethod(commandLine, executor, hostname);

        if (!commandLine.hasOption(Arguments.FOLLOW_REDIRECTS.getOpt())) {
            executor.disableRedirectHandling();
        }
        HttpClientProvider.handleSSLParams(commandLine, executor);
        return executor.build();
    }

    private static void handleSSLParams(final CommandLine commandLine, final HttpClientBuilder executor) throws CurlException {
        final SSLContextBuilder builder = new SSLContextBuilder();

        if (commandLine.hasOption(Arguments.TRUST_INSECURE.getOpt())) {
            sayTrustInsecure(builder);
        }
        if (commandLine.hasOption(Arguments.CERT.getOpt())) {
            final String[] credentials = commandLine.getOptionValue(Arguments.CERT.getOpt()).split(":");
            addClientCredentials(builder, "pkcs12", credentials[0], credentials.length > 1 ? credentials[1] : null);

        }
        
        try {
            final SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(builder.build());
            executor.setSSLSocketFactory(sslSocketFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new CurlException(e);
        }
    }

    private static void addClientCredentials(final SSLContextBuilder builder, final String algorithm, final String filePath, final String password) throws CurlException {
        try {
            final KeyStore keyStore = KeyStore.getInstance(algorithm);
            final File fileObject = getFile(filePath);
            keyStore.load(new FileInputStream(fileObject), password.toCharArray());
            builder.loadKeyMaterial(keyStore, password.toCharArray());
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException | CertificateException | IOException e) {
            throw new CurlException(e);
        }
    }

    private static File getFile(final String filePath) {
        final File file = new File(filePath);
        if (file.exists()) {
            return file;
        }
        return new File(System.getProperty("user.dir") + File.separator + filePath);
    }

    private static void sayTrustInsecure(final SSLContextBuilder builder) throws CurlException {
        try {
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new CurlException(e);
        }

    }

    private static HttpClientBuilder handleAuthMethod(final CommandLine commandLine, HttpClientBuilder executor, final String hostname) {
        if (commandLine.getOptionValue(Arguments.AUTH.getOpt()) != null) {
            final String[] authValue = commandLine.getOptionValue(Arguments.AUTH.getOpt()).toString().split("(?<!\\\\):");
            if (commandLine.hasOption(Arguments.NTLM.getOpt())) {
                final String[] userName = authValue[0].split("\\\\");
                final SystemDefaultCredentialsProvider systemDefaultCredentialsProvider = new SystemDefaultCredentialsProvider();
                systemDefaultCredentialsProvider.setCredentials(AuthScope.ANY, new NTCredentials(userName[1], authValue[1], hostname, userName[0]));
                executor = executor.setDefaultCredentialsProvider(systemDefaultCredentialsProvider);
            } else {
                final BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
                basicCredentialsProvider.setCredentials(new AuthScope(HttpHost.create(URI.create(commandLine.getArgs()[0]).getHost())),
                        new UsernamePasswordCredentials(authValue[0], authValue[1]));
                executor = executor.setDefaultCredentialsProvider(basicCredentialsProvider);
            }
        }
        return executor;
    }
}
