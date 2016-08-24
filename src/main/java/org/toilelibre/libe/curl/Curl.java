package org.toilelibre.libe.curl;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpResponse;

public class Curl {

    private Curl () {
    }

    public static String $ (final String requestCommand) throws CurlException {
        try {
            return IOUtils.toString (Curl.curl (requestCommand).getEntity ().getContent ());
        } catch (final IOException e) {
            throw new CurlException (e);
        }
    }

    public static CurlArgumentsBuilder curl () {
        return new CurlArgumentsBuilder ();
    }

    public static HttpResponse curl (final String requestCommand) throws CurlException {
        final CommandLine commandLine = ReadArguments.getCommandLineFromRequest (requestCommand);
        try {
            return HttpClientProvider.prepareHttpClient (commandLine).execute (HttpRequestProvider.prepareRequest (commandLine));
        } catch (final IOException | IllegalArgumentException e) {
            throw new CurlException (e);
        }
    }

    public static class CurlArgumentsBuilder {

        private final StringBuilder curlCommand = new StringBuilder ("curl ");

        CurlArgumentsBuilder () {
        }

        public String $ (final String url) throws CurlException {
            this.curlCommand.append (url + " ");
            return Curl.$ (this.curlCommand.toString ());
        }

        public HttpResponse run (final String url) throws CurlException {
            this.curlCommand.append (url + " ");
            return Curl.curl (this.curlCommand.toString ());
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
