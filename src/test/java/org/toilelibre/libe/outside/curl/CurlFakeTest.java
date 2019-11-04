package org.toilelibre.libe.outside.curl;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.VersionInfo;
import org.junit.Test;
import org.toilelibre.libe.curl.Curl;
import org.toilelibre.libe.curl.Version;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
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
                    assertEquals(URI.create("https://put.anything.in.this.url:1337"),
                            ((HttpRequestBase)((HttpRequestWrapper)
                                    context.getAttribute("http.request")).getOriginal()).getURI())
                );
    }

    @Test
    public void curlVerifyHeader () {
        this.curl ("-H 'Titi: toto' https://put.anything.in.this.url:1337",
                context ->
                        assertEquals("toto",
                                ((HttpRequestWrapper)
                                        context.getAttribute("http.request"))
                                        .getLastHeader("Titi").getValue ()));
    }

    @Test
    public void curlVerifyUserAgent () {
        this.curl ("-H 'User-Agent: toto' https://put.anything.in.this.url:1337",
                context ->
                        assertEquals("toto",
                                ((HttpRequestWrapper)
                                        context.getAttribute("http.request"))
                                        .getLastHeader("User-Agent").getValue ()));
    }

    @Test
    public void curlVerifyUserAgentAndAOptionTogether () {
        this.curl ("-H 'User-Agent: toto' -A titi https://put.anything.in.this.url:1337",
                context ->
                        assertEquals("toto",
                                ((HttpRequestWrapper)
                                        context.getAttribute("http.request"))
                                        .getLastHeader("User-Agent").getValue ()));
    }

    @Test
    public void curlVerifyAOptionAlone () {
        this.curl ("-A titi https://put.anything.in.this.url:1337",
                context ->
                        assertEquals("titi",
                                ((HttpRequestWrapper)
                                        context.getAttribute("http.request"))
                                        .getLastHeader("User-Agent").getValue ()));
    }

    @Test
    public void curlWithDefaultUserAgent () {
        this.curl ("https://put.anything.in.this.url:1337",
                context ->
                        assertEquals(Curl.class.getPackage ().getName () + "/" + Version.VERSION +
                                        VersionInfo.getUserAgent (", Apache-HttpClient",
                                                "org.apache.http.client", CurlFakeTest.class),
                                ((HttpRequestWrapper)
                                        context.getAttribute("http.request"))
                                        .getLastHeader("User-Agent").getValue ()));
    }

    private HttpResponse curl (final String requestCommand, Consumer<HttpContext> assertions) {
        return org.toilelibre.libe.curl.Curl.curl (requestCommand,
                Curl.CurlArgumentsBuilder.CurlJavaOptions.with().connectionManager(
                        new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create ()
                                .register ("https", new FakeConnectionSocketFactory (assertions))
                                .register ("http", new FakeConnectionSocketFactory (assertions))
                                .build(),
                                host -> new InetAddress[] {InetAddress.getLoopbackAddress ()})).build ());
    }

    private static class FakeConnectionSocketFactory implements ConnectionSocketFactory {


        private static final byte[] OK_COMPUTER = ("HTTP/1.0 200 OK\n" +
                        "Content-Type: text/html; charset=UTF-8\n" +
                        "Referrer-Policy: no-referrer\n" +
                        "Content-Length: 222\n" +
                        "Date: Sun, 02 Jun 2019 15:31:37 GMT\n" +
                        "\n" +
                        "\n" +
                        "<!DOCTYPE html>\n" +
                        "<html lang=en>\n" +
                        "<head>\n" +
                        "<meta charset=utf-8>\n" +
                        "<meta name=viewport content=\"initial-scale=1, minimum-scale=1, width=device-width\">\n" +
                        "<title>OK Computer</title>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "<h1>OK Computer</h1>\n" +
                        "</body>\n" +
                        "</html>\n").getBytes();

        private final Consumer<HttpContext> assertions;

        public FakeConnectionSocketFactory(Consumer<HttpContext> assertions) {
            this.assertions = assertions;
        }

        @Override
        public Socket createSocket(HttpContext context) {

            return new Socket(){
                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(OK_COMPUTER);
                }
                @Override
                public OutputStream getOutputStream() {
                    return new ByteArrayOutputStream();
                }
            };
        }

        @Override
        public Socket connectSocket(int connectTimeout, Socket sock,
                                    HttpHost host,
                                    InetSocketAddress remoteAddress,
                                    InetSocketAddress localAddress,
                                    HttpContext context) {
            assertions.accept(context);
            return sock;
        }
    }
}
