package org.toilelibre.libe.curl;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.toilelibre.libe.curl.Curl.curl;

public class HttpRequestProviderTest {

    @Test
    public void curlWithoutVerbAndWithoutDataShouldBeTransformedAsGetRequest () {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest (
                "curl -H'Accept: application/json' http://localhost/user/byId/1");

        //when
        HttpUriRequest request = HttpRequestProvider.prepareRequest(commandLine);

        //then
        assertTrue(request instanceof HttpGet);
    }


    @Test
    public void curlWithoutVerbAndWithDataShouldBeTransformedAsPostRequest () {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest (
                "curl -H'Accept: application/json' -d'{\"id\":1,\"name\":\"John Doe\"}' http://localhost/user/");

        //when
        HttpUriRequest request = HttpRequestProvider.prepareRequest(commandLine);

        //then
        assertTrue(request instanceof HttpPost);
    }

    @Test
    public void proxyWithAuthentication() {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest (
                "http://httpbin.org/get -x http://87.98.174.157:3128/ -U user:password");

        //when
        HttpUriRequest request = HttpRequestProvider.prepareRequest (commandLine);

        //then
        assertEquals (request.getFirstHeader ("Proxy-Authenticate").getValue (),
                "Basic dXNlcjpwYXNzd29yZA==");
    }
}
