package org.toilelibre.libe.curl;

import org.apache.commons.cli.*;
import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.impl.client.*;

import java.net.*;

final class AuthMethodHandler {

    static HttpClientBuilder handleAuthMethod (final CommandLine commandLine, HttpClientBuilder executor,
                                               final String hostname) {
        if (commandLine.getOptionValue (Arguments.AUTH.getOpt ()) != null) {
            final String[] authValue = commandLine.getOptionValue (Arguments.AUTH.getOpt ()).split ("(?<!\\\\):");
            if (commandLine.hasOption (Arguments.NTLM.getOpt ())) {
                final String[] userName = authValue[0].split ("\\\\");
                final SystemDefaultCredentialsProvider systemDefaultCredentialsProvider =
                        new SystemDefaultCredentialsProvider ();
                systemDefaultCredentialsProvider.setCredentials (AuthScope.ANY, new NTCredentials (userName[1],
                        authValue[1], hostname, userName[0]));
                return executor.setDefaultCredentialsProvider (systemDefaultCredentialsProvider);
            }
            final BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider ();
            basicCredentialsProvider.setCredentials (new AuthScope (HttpHost.create (URI.create (commandLine.getArgs ()[0]).getHost ())), new UsernamePasswordCredentials (authValue[0], authValue.length > 1 ? authValue[1] : null));
            return executor.setDefaultCredentialsProvider (basicCredentialsProvider);
        }
        return executor;
    }
}
