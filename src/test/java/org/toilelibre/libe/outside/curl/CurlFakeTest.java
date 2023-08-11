package org.toilelibre.libe.outside.curl;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.auth.AuthExchange;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.io.ManagedHttpClientConnectionFactory;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.core5.util.VersionInfo;
import org.junit.Test;
import org.toilelibre.libe.curl.Curl;

import java.io.*;
import java.net.*;
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
                Curl.CurlArgumentsBuilder.CurlJavaOptions.with ().httpClientBuilder(HttpClients.custom().setConnectionManager(
                        new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
                                .register("https", new FakeConnectionSocketFactory())
                                .register("http", new FakeConnectionSocketFactory())
                                .build(),
                                PoolConcurrencyPolicy.LAX,
                                PoolReusePolicy.FIFO,
                                TimeValue.ofSeconds(60),
                                new DefaultSchemePortResolver(),
                                new DnsResolver() {
                                    @Override
                                    public InetAddress[] resolve(String host) throws UnknownHostException {
                                        return new InetAddress[] {InetAddress.getLoopbackAddress ()};
                                    }

                                    @Override
                                    public String resolveCanonicalHostname(String host) throws UnknownHostException {
                                        return "localhost";
                                    }
                                },
                                new ManagedHttpClientConnectionFactory())))
                        .contextTester(httpContext -> assertions.accept(httpContext)).build());
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
                        "</html>\n").getBytes ();

        public FakeConnectionSocketFactory () {}


        @Override
        public Socket createSocket(org.apache.hc.core5.http.protocol.HttpContext context) throws IOException {
            return new Socket (){
                @Override
                public InputStream getInputStream () {
                    return new ByteArrayInputStream (OK_COMPUTER);
                }
                @Override
                public OutputStream getOutputStream () {
                    return new ByteArrayOutputStream ();
                }
            };
        }

        @Override
        public Socket connectSocket(TimeValue connectTimeout, Socket socket, org.apache.hc.core5.http.HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, org.apache.hc.core5.http.protocol.HttpContext context) throws IOException {
            return socket;
        }

        @Override
        public Socket connectSocket(Socket socket, org.apache.hc.core5.http.HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, Timeout connectTimeout, Object attachment, org.apache.hc.core5.http.protocol.HttpContext context) throws IOException {
            return ConnectionSocketFactory.super.connectSocket(socket, host, remoteAddress, localAddress, connectTimeout, attachment, context);
        }
    }
}
