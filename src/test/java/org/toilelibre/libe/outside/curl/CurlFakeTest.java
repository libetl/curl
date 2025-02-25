package org.toilelibre.libe.outside.curl;

import org.apache.hc.client5.http.auth.AuthExchange;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.VersionInfo;
import org.junit.Test;
import org.toilelibre.libe.curl.Curl;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

/**
 * This is a cheap test suite because this does not need any web server to run.
 * It mocks the connection backend of apache httpclient to proceed to some assertions.
 * If you need to write a fast test you can start here.
 *
 * Keep in mind that the same fake response will be sent. Don't assert anything on
 * the response as it does not make any sense.
 */
public class CurlFakeTest {

    @Test
    public void curlVerifyHost () {
        this.curl ("https://put.anything.in.this.url:1337",
                context ->
                    assertEquals ("https://put.anything.in.this.url:1337",
                            (((Map<HttpHost, AuthExchange>)
                                    context.getAttribute ("http.auth.exchanges"))
                                    .keySet().iterator().next()).toString())
                );
    }

    @Test
    public void curlVerifyHeader () {
        this.curl ("-H 'Titi: toto' https://put.anything.in.this.url:1337",
                context ->
                        assertEquals ("toto",
                                ((HttpRequest)
                                        context.getAttribute ("http.request"))
                                        .getLastHeader ("Titi").getValue ()));
    }

    @Test
    public void curlVerifyUserAgent () {
        this.curl ("-H 'User-Agent: toto' https://put.anything.in.this.url:1337",
                context ->
                        assertEquals ("toto",
                                ((HttpRequest)
                                        context.getAttribute ("http.request"))
                                        .getLastHeader ("User-Agent").getValue ()));
    }

    @Test
    public void curlVerifyUserAgentAndAOptionTogether () {
        this.curl ("-H 'User-Agent: toto' -A titi https://put.anything.in.this.url:1337",
                context ->
                        assertEquals ("toto",
                                ((HttpRequest)
                                        context.getAttribute ("http.request"))
                                        .getLastHeader ("User-Agent").getValue ()));
    }

    @Test
    public void curlVerifyAOptionAlone () {
        this.curl ("-A titi https://put.anything.in.this.url:1337",
                context ->
                        assertEquals ("titi",
                                ((HttpRequest)
                                        context.getAttribute ("http.request"))
                                        .getLastHeader ("User-Agent").getValue ()));
    }

    @Test
    public void curlWithDefaultUserAgent () {
        this.curl ("https://put.anything.in.this.url:1337",
                context ->
                        assertEquals (Curl.class.getPackage ().getName () + "/" + Curl.getVersion () +
                                        VersionInfo.getSoftwareInfo (", Apache-HttpClient",
                                                "org.apache.http.client", CurlFakeTest.class),
                                ((HttpRequest) context.getAttribute ("http.request"))
                                        .getLastHeader ("User-Agent").getValue ()));
    }

    private ClassicHttpResponse curl (final String requestCommand, Consumer<HttpContext> assertions) {
        return org.toilelibre.libe.curl.Curl.curl (requestCommand,
                Curl.CurlArgumentsBuilder.CurlJavaOptions.with ().mockedNetworkAccess ()
                        .contextTester(httpContext -> assertions.accept(httpContext)).build());
    }

}
