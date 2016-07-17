package org.toilelibre.libe.curl;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

public class Curl {

    public static String $ (final String requestCommand) {
        try {
            return IOUtils.toString(Curl.curl (requestCommand).getEntity ().getContent ());
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }

    public static HttpResponse curl (final String requestCommand) {
        final CommandLine commandLine = ReadArguments.getCommandLineFromRequest (requestCommand);
        return Curl.sendRequestAndReturnResponse (HttpRequestProvider.prepareRequest (commandLine), HttpClientProvider.prepareHttpClient (commandLine));
    }

    private static HttpResponse sendRequestAndReturnResponse (final HttpUriRequest request, final HttpClient executor) {
        try {
            return executor.execute (request);
        } catch (final IOException e) {
            throw new RuntimeException (e);
        }
    }

}
