package org.toilelibre.libe.curl;

import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.config.NamedElementChain;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.util.function.Consumer;

class HttpClientBuilder extends org.apache.hc.client5.http.impl.classic.HttpClientBuilder {

    private Consumer<HttpContext> contextTester;

    public static HttpClientBuilder create() {
        return new HttpClientBuilder();
    }

    public void setContextTester(Consumer<HttpContext> contextTester) {
        this.contextTester = contextTester;
    }

    @Override
    protected void customizeExecChain(NamedElementChain<ExecChainHandler> execChainDefinition) {
        execChainDefinition.addLast(
                (request, scope, chain) -> {
                    if (contextTester != null) {
                        contextTester.accept(scope.clientContext);
                    }
                    return chain.proceed(request, scope);
                },
                "context-tester"
        );
    }
}
