package org.toilelibre.libe.curl;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpResponse;

public class Curl {

    private Curl () {
    }

    public static String $ (final String requestCommand) throws CurlException {
        try {
            return IOUtils.toString (Curl.curlAsync (requestCommand).get ().getEntity ().getContent ());
        } catch (final IOException | UnsupportedOperationException | InterruptedException | ExecutionException e) {
            throw new CurlException (e);
        }
    }

    public static CompletableFuture<String> $Async (final String requestCommand) throws CurlException {
        return Curl.curlAsync (requestCommand).thenApply ( (httpResponse) -> IOUtils.quietToString (httpResponse.getEntity ()));
    }

    public static CurlArgumentsBuilder curl () {
        return new CurlArgumentsBuilder ();
    }

    public static HttpResponse curl (final String requestCommand) throws CurlException {
        try {
            return Curl.curlAsync (requestCommand).get ();
        } catch (InterruptedException | ExecutionException e) {
            throw new CurlException (e);
        }
    }

    public static CompletableFuture<HttpResponse> curlAsync (final String requestCommand) throws CurlException {
        return CompletableFuture.<HttpResponse> supplyAsync ( () -> {
            final CommandLine commandLine = ReadArguments.getCommandLineFromRequest (requestCommand);
            try {
                final HttpResponse response = HttpClientProvider.prepareHttpClient (commandLine).execute (HttpRequestProvider.prepareRequest (commandLine));
                AfterResponse.handle (commandLine, response);
                return response;
            } catch (final IOException | IllegalArgumentException e) {
                throw new CurlException (e);
            }
        }).toCompletableFuture ();
    }

    public static class CurlArgumentsBuilder {

        private final StringBuilder curlCommand = new StringBuilder ("curl ");

        CurlArgumentsBuilder () {
        }

        public String $ (final String url) throws CurlException {
            this.curlCommand.append (url).append (" ");
            return Curl.$ (this.curlCommand.toString ());
        }

        public CompletableFuture<String> $Async (final String url) throws CurlException {
            this.curlCommand.append (url).append (" ");
            return Curl.$Async (this.curlCommand.toString ());
        }

        public HttpResponse run (final String url) throws CurlException {
            this.curlCommand.append (url).append (" ");
            return Curl.curl (this.curlCommand.toString ());
        }

        public CompletableFuture<HttpResponse> runAsync (final String url) throws CurlException {
            this.curlCommand.append (url).append (" ");
            return Curl.curlAsync (this.curlCommand.toString ());
        }

    }

    public static class CurlException extends RuntimeException {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        CurlException (final Throwable arg0) {
            super (arg0);
        }
    }

}
