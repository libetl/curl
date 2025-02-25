package org.toilelibre.libe.curl;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.io.ConnectionEndpoint;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.io.LeaseRequest;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.EndpointDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.ProtocolVersion;
import org.apache.hc.core5.http.impl.BasicEndpointDetails;
import org.apache.hc.core5.http.impl.io.HttpRequestExecutor;
import org.apache.hc.core5.http.io.HttpClientConnection;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

class MockNetworkAccess implements HttpClientConnectionManager {
    public static HttpClientConnection mockConnection = new HttpClientConnection() {
        @Override
        public boolean isConsistent() {
            return true;
        }

        @Override
        public void sendRequestHeader(ClassicHttpRequest request) {

        }

        @Override
        public void terminateRequest(ClassicHttpRequest request) {

        }

        @Override
        public void sendRequestEntity(ClassicHttpRequest request) {

        }

        @Override
        public ClassicHttpResponse receiveResponseHeader() {
            ClassicHttpResponse response = new BasicClassicHttpResponse(200);
            response.setEntity(new StringEntity("{\"status\":\"ok\"}"));
            response.setHeader(new BasicHeader("Content-Type", "application/json"));
            return response;
        }

        @Override
        public void receiveResponseEntity(ClassicHttpResponse response) {

        }

        @Override
        public boolean isDataAvailable(Timeout timeout) {
            return true;
        }

        @Override
        public boolean isStale() {
            return false;
        }

        @Override
        public void flush() {

        }

        @Override
        public void close() {

        }

        @Override
        public EndpointDetails getEndpointDetails() {
            return new BasicEndpointDetails(
                    InetSocketAddress.createUnresolved("localhost", 80),
                    InetSocketAddress.createUnresolved("localhost", 80),
                    null,
                    Timeout.ofSeconds(60)

            );
        }

        @Override
        public SocketAddress getLocalAddress() {
            return getEndpointDetails().getLocalAddress();
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return getEndpointDetails().getRemoteAddress();
        }

        @Override
        public ProtocolVersion getProtocolVersion() {
            try {
                return ProtocolVersion.parse("http/2");
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public SSLSession getSSLSession() {
            return null;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public Timeout getSocketTimeout() {
            return getEndpointDetails().getSocketTimeout();
        }

        @Override
        public void setSocketTimeout(Timeout timeout) {

        }

        @Override
        public void close(CloseMode closeMode) {

        }
    };
    @Override
    public LeaseRequest lease(String s, HttpRoute httpRoute, Timeout timeout, Object o) {
        return new LeaseRequest() {
            @Override
            public boolean cancel() {
                return true;
            }

            public ConnectionEndpoint get(Timeout timeout) {
                return new ConnectionEndpoint() {
                    @Override
                    public ClassicHttpResponse execute(String id, ClassicHttpRequest request, RequestExecutor requestExecutor, HttpContext context) throws IOException, HttpException {
                        return requestExecutor.execute(request, mockConnection, context);
                    }

                    @Override
                    public ClassicHttpResponse execute(String s,
                                                       ClassicHttpRequest classicHttpRequest,
                                                       HttpRequestExecutor httpRequestExecutor,
                                                       HttpContext httpContext) {
                        return null;
                    }

                    @Override
                    public boolean isConnected() {
                        return false;
                    }

                    @Override
                    public void setSocketTimeout(Timeout timeout) {

                    }

                    @Override
                    public void close(CloseMode closeMode) {

                    }

                    @Override
                    public void close() throws IOException {

                    }
                };
            }
        };
    }

    @Override
    public void release(ConnectionEndpoint connectionEndpoint, Object o, TimeValue timeValue) {

    }

    @Override
    public void connect(ConnectionEndpoint connectionEndpoint, TimeValue timeValue, HttpContext httpContext) throws IOException {
    }

    @Override
    public void upgrade(ConnectionEndpoint connectionEndpoint, HttpContext httpContext) throws IOException {

    }

    @Override
    public void close(CloseMode closeMode) {

    }

    @Override
    public void close() throws IOException {

    }
}
