package org.toilelibre.libe.curl;

import org.apache.commons.cli.*;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.conn.*;
import org.apache.http.impl.client.*;
import org.toilelibre.libe.curl.Curl.*;

import java.net.*;
import java.util.*;
import java.util.function.*;

import static org.toilelibre.libe.curl.AuthMethodHandler.*;
import static org.toilelibre.libe.curl.SSLMaterialCreator.*;

final class HttpClientProvider {

    static HttpClient prepareHttpClient (final CommandLine commandLine,
                                         List<BiFunction<HttpRequest, Supplier<HttpResponse>, HttpResponse>> additionalInterceptors,
                                         HttpClientConnectionManager connectionManager) throws CurlException {
        HttpClientBuilder executor = HttpClientBuilder.create ();

        if (!commandLine.hasOption (Arguments.COMPRESSED.getOpt())){
            executor.disableContentCompression();
        }

        if (connectionManager != null) {
            executor.setConnectionManager (connectionManager);
        }

        final String hostname;
        try {
            hostname = InetAddress.getLocalHost ().getHostName ();
        } catch (final UnknownHostException e1) {
            throw new RuntimeException (e1);
        }

        executor = handleAuthMethod (commandLine, executor, hostname);

        if (! commandLine.hasOption (Arguments.FOLLOW_REDIRECTS.getOpt ())) {
            executor.disableRedirectHandling ();
        }

        handleSSLParams (commandLine, executor);
        InterceptorsBinder.handleInterceptors (commandLine, executor, additionalInterceptors);
        return executor.build ();
    }
}
