package org.toilelibre.libe.curl;

import org.apache.commons.cli.CommandLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

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
}
