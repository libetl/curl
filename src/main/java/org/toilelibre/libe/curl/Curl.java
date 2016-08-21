package org.toilelibre.libe.curl;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpResponse;

public class Curl {

    public static CurlArgumentsBuilder curl () {
        return new CurlArgumentsBuilder ();
    }

    public static String $ (final String requestCommand) throws CurlException {
        try {
            return IOUtils.toString (Curl.curl (requestCommand).getEntity ().getContent ());
        } catch (final IOException e) {
            throw new CurlException (e);
        }
    }

    public static HttpResponse curl (final String requestCommand) throws CurlException {
        final CommandLine commandLine = ReadArguments.getCommandLineFromRequest (requestCommand);
        try {
            return HttpClientProvider.prepareHttpClient (commandLine).execute (HttpRequestProvider.prepareRequest (commandLine));
        } catch (final IOException e) {
            throw new CurlException (e);
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
    
    public static class CurlArgumentsBuilder {
        
        private final StringBuilder curlCommand = new StringBuilder ("curl ");
        
        CurlArgumentsBuilder () {}
        
        public HttpResponse run (String url) throws CurlException {
            curlCommand.append (url + " ");
            return Curl.curl (curlCommand.toString ());
        }

        public String $ (String url) throws CurlException {
            curlCommand.append (url + " ");
            return Curl.$ (curlCommand.toString ());
        }

    }

}
