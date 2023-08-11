package org.toilelibre.libe.curl;

import org.apache.commons.cli.CommandLine;
import org.apache.hc.client5.http.auth.AuthSchemeFactory;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.NTCredentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.SystemDefaultCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

final class AuthMethodHandler {

    private static final AuthScope ANY = new AuthScope(null, null, -1, null, null);

    static HttpClientBuilder handleAuthMethod (final CommandLine commandLine, HttpClientBuilder executor,
                                               final String hostname) {
        if (commandLine.getOptionValue (Arguments.AUTH.getOpt ()) != null) {
            final String[] authValue = commandLine.getOptionValue (Arguments.AUTH.getOpt ()).split ("(?<!\\\\):");
            if (commandLine.hasOption (Arguments.NTLM.getOpt ())) {
                final String[] userName = authValue[0].split ("\\\\");
                final SystemDefaultCredentialsProvider systemDefaultCredentialsProvider =
                        new SystemDefaultCredentialsProvider ();
                systemDefaultCredentialsProvider.setCredentials (ANY, new NTCredentials(userName[1],
                        authValue[1].toCharArray(), hostname, userName[0]));
                return (HttpClientBuilder) executor.setDefaultCredentialsProvider (systemDefaultCredentialsProvider);
            }
            try {
                final BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider ();
                final URI targetUri = URI.create (commandLine.getArgs ()[0]);
                basicCredentialsProvider.setCredentials (new AuthScope (HttpHost.create (
                                targetUri.toURL ().getProtocol () + "://" + targetUri.getAuthority ())),
                        new UsernamePasswordCredentials (authValue[0], authValue.length > 1 ? authValue[1].toCharArray() : null));
                return (HttpClientBuilder) executor.setDefaultCredentialsProvider (basicCredentialsProvider);
            } catch (URISyntaxException | MalformedURLException e) {
                throw new Curl.CurlException(e);
            }
        }
        return executor;
    }
}
