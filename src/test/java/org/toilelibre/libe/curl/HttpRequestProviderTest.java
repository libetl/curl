package org.toilelibre.libe.curl;

import org.apache.commons.cli.CommandLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpRequestProviderTest {

    @Test
    public void curlWithoutVerbAndWithoutDataShouldBeTransformedAsGetRequest () {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest (
                "curl -H'Accept: application/json' http://localhost/user/byId/1", Collections.emptyList());

        //when
        HttpUriRequest request = HttpRequestProvider.prepareRequest(commandLine);

        //then
        assertTrue(request instanceof HttpGet);
    }

    @Test
    public void curlWithAPlaceholder () {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest (
                "curl -H $curl_placeholder_0 http://localhost/user/byId/1",
                Collections.singletonList("Accept: application/json"));

        //when
        HttpUriRequest request = HttpRequestProvider.prepareRequest(commandLine);

        //then
        assertTrue(request instanceof HttpGet);
    }


    @Test
    public void curlWithoutVerbAndWithDataShouldBeTransformedAsPostRequest () {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest (
                "curl -H'Accept: application/json' -d'{\"id\":1,\"name\":\"John Doe\"}' http://localhost/user/",
                Collections.emptyList());

        //when
        HttpUriRequest request = HttpRequestProvider.prepareRequest(commandLine);

        //then
        assertTrue(request instanceof HttpPost);
    }

    @Test
    public void proxyWithAuthentication() {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest (
                "http://httpbin.org/get -x http://87.98.174.157:3128/ -U user:password",
                Collections.emptyList());

        //when
        HttpUriRequest request = HttpRequestProvider.prepareRequest (commandLine);

        //then
        assertEquals (request.getFirstHeader ("Proxy-Authorization").getValue (),
                "Basic dXNlcjpwYXNzd29yZA==");
    }

    @Test
    public void proxyWithAuthentication2 () {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest (
                "-x http://localhost:80/ -U jack:insecure http://www.google.com/",
                Collections.emptyList());

        //when
        HttpUriRequest request = HttpRequestProvider.prepareRequest (commandLine);

        //then
        assertEquals (request.getFirstHeader ("Proxy-Authorization").getValue (),
                "Basic amFjazppbnNlY3VyZQ==");
        assertEquals(((HttpGet)request).getConfig().getProxy().toString(), "http://localhost:80");
    }

    @Test
    public void proxyWithAuthentication3 () {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest (
                "-x http://jack:insecure@localhost:80/ http://www.google.com/",
                Collections.emptyList());

        //when
        HttpUriRequest request = HttpRequestProvider.prepareRequest (commandLine);

        //then
        assertEquals (request.getFirstHeader ("Proxy-Authorization").getValue (),
                "Basic amFjazppbnNlY3VyZQ==");
        assertEquals(((HttpGet)request).getConfig().getProxy().toString(), "http://localhost:80");
    }
}
