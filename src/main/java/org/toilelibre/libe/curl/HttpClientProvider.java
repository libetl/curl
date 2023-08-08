package org.toilelibre.libe.curl;

import org.apache.commons.cli.CommandLine;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpProcessor;
import org.toilelibre.libe.curl.Curl.CurlException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.toilelibre.libe.curl.AuthMethodHandler.handleAuthMethod;
import static org.toilelibre.libe.curl.HttpRequestProvider.getConnectionConfig;
import static org.toilelibre.libe.curl.HttpRequestProvider.getRoutePlanner;

final class HttpClientProvider {

    static HttpClient prepareHttpClient (final CommandLine commandLine,
                                         List<BiFunction<HttpRequest, Supplier<ClassicHttpResponse>, ClassicHttpResponse>> additionalInterceptors,
                                         HttpClientConnectionManager connectionManager,
                                         Consumer<HttpContext> contextTester) throws CurlException {
        HttpClientBuilder executor = HttpClientBuilder.create ();

        if (!commandLine.hasOption (Arguments.COMPRESSED.getOpt ())){
            executor.disableContentCompression ();
        }

        final HttpClientConnectionManager chosenConnectionManager = connectionManager != null
          ? connectionManager
          : PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(
                  SSLMaterialCreator.buildConnectionFactory (commandLine)
                ).setConnectionConfigResolver (
                   route -> getConnectionConfig (commandLine)
                ).build();

        executor.setConnectionManager (chosenConnectionManager);
        executor.setRoutePlanner (getRoutePlanner(commandLine));

        final String hostname;
        try {
            hostname = InetAddress.getLocalHost ().getHostName ();
        } catch (final UnknownHostException e1) {
            throw new Curl.CurlException (e1);
        }

        executor = handleAuthMethod (commandLine, executor, hostname);

        if (! commandLine.hasOption (Arguments.FOLLOW_REDIRECTS.getOpt ())) {
            executor.disableRedirectHandling ();
        }

        InterceptorsBinder.handleInterceptors (commandLine, executor, additionalInterceptors);

        executor.setContextTester(contextTester);

        return executor.build ();
    }
}
