package org.toilelibre.libe.curl;

import org.apache.commons.cli.*;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static org.toilelibre.libe.curl.Curl.CurlArgumentsBuilder.CurlJavaOptions.*;
import static org.toilelibre.libe.curl.UglyVersionDisplay.*;

public final class Curl {

    private static final CurlArgumentsBuilder.CurlJavaOptions NO_JAVA_OPTIONS = with().build();
    private static final Curl DEFAULT = new Curl(NO_JAVA_OPTIONS);
    private final CurlArgumentsBuilder.CurlJavaOptions curlJavaOptions;

    public Curl(CurlArgumentsBuilder.CurlJavaOptions curlJavaOptions) {
        this.curlJavaOptions = curlJavaOptions;
    }

    public static String $(final String requestCommand) throws CurlException {
        return $(requestCommand, NO_JAVA_OPTIONS);
    }

    public static CompletableFuture<String> $Async(final String requestCommand) throws CurlException {
        return $Async(requestCommand, NO_JAVA_OPTIONS);
    }

    public static CompletableFuture<String> $Async(final String requestCommand,
                                                   CurlArgumentsBuilder.CurlJavaOptions curlJavaOptions) throws CurlException {
        return Curl.curlAsync(requestCommand, curlJavaOptions).thenApply((httpResponse) -> IOUtils.quietToString(httpResponse.getEntity()));
    }

    public static CurlArgumentsBuilder curl() {
        return new CurlArgumentsBuilder();
    }

    public static CompletableFuture<ClassicHttpResponse> curlAsync(final String requestCommand) throws CurlException {
        return curlAsync(requestCommand, NO_JAVA_OPTIONS);
    }

    public static String $(final String requestCommand, CurlArgumentsBuilder.CurlJavaOptions curlJavaOptions) throws CurlException {
        try {
            return IOUtils.quietToString(DEFAULT.curl_(requestCommand, curlJavaOptions).getEntity());
        } catch (final UnsupportedOperationException e) {
            throw new CurlException(e);
        }
    }

    public static CompletableFuture<ClassicHttpResponse> curlAsync(final String requestCommand,
                                                                   CurlArgumentsBuilder.CurlJavaOptions curlJavaOptions) throws CurlException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return DEFAULT.curl_(requestCommand, curlJavaOptions);
            } catch (IllegalArgumentException e) {
                throw new CurlException(e);
            }
        }).toCompletableFuture();
    }

    public static ClassicHttpResponse curl(final String requestCommand) throws CurlException {
        return DEFAULT.curl_(requestCommand, null);
    }

    public ClassicHttpResponse curl_(final String requestCommand) throws CurlException {
        return this.curl_(requestCommand, null);
    }

    public static ClassicHttpResponse curl (final String requestCommand,
                                      CurlArgumentsBuilder.CurlJavaOptions curlJavaOptions) throws CurlException {
        return DEFAULT.curl_(requestCommand, curlJavaOptions);
    }

    public ClassicHttpResponse curl_ (final String requestCommand,
                                     CurlArgumentsBuilder.CurlJavaOptions curlJavaOptions) throws CurlException {
        try {
            final CurlArgumentsBuilder.CurlJavaOptions usedJavaOptions = curlJavaOptions != null
                    ? curlJavaOptions
                    : this.curlJavaOptions;
            final CommandLine commandLine = ReadArguments.getCommandLineFromRequest (requestCommand,
                    usedJavaOptions.getPlaceHolders (), usedJavaOptions.simpleArgsParsing ());
            stopAndDisplayVersionIfThe (commandLine.hasOption (Arguments.VERSION.getOpt ()));
            final ClassicHttpResponse response = (ClassicHttpResponse)
                    HttpClientProvider.prepareHttpClient (commandLine, usedJavaOptions.getInterceptors (),
                            usedJavaOptions.getConnectionManager (),
                            usedJavaOptions.getHttpClientCustomizer (),
                            usedJavaOptions.getContextTester ()).execute (
                            HttpRequestProvider.prepareRequest (commandLine));
            AfterResponse.handle (commandLine, response);
            return response;
        } catch (final IOException | IllegalArgumentException e) {
            throw new CurlException (e);
        }
    }

    public static String getVersion () {
        return Version.NUMBER;
    }

    public static String getVersionWithBuildTime () {
        return Version.NUMBER + " (Build time : " + Version.BUILD_TIME + ")";
    }

    public static class CurlArgumentsBuilder {

        private final StringBuilder curlCommand = new StringBuilder ("curl ");
        private CurlJavaOptions curlJavaOptions = with ().build ();

        public static class CurlJavaOptions {
            private final List<BiFunction<HttpRequest, Supplier<ClassicHttpResponse>, ClassicHttpResponse>> interceptors;
            private final List<String> placeHolders;
            private final HttpClientConnectionManager connectionManager;
            private final Consumer<HttpContext> contextTester;
            private final Consumer<org.apache.hc.client5.http.impl.classic.HttpClientBuilder> httpClientCustomizer;
            private final boolean simpleArgsParsing;
            private CurlJavaOptions (Builder builder) {
                interceptors = builder.interceptors;
                placeHolders = builder.placeHolders;
                connectionManager = builder.connectionManager;
                contextTester = builder.contextTester;
                httpClientCustomizer = builder.httpClientCustomizer;
                simpleArgsParsing = builder.simpleArgsParsing;
            }

            public static Builder with () {
                return new Builder ();
            }

            public List<BiFunction<HttpRequest, Supplier<ClassicHttpResponse>, ClassicHttpResponse>> getInterceptors () {
                return interceptors;
            }

            public List<String> getPlaceHolders () {
                return placeHolders;
            }

            public HttpClientConnectionManager getConnectionManager () {
                return connectionManager;
            }

            public Consumer<HttpContext> getContextTester () {
                return contextTester;
            }

            public Consumer<HttpClientBuilder> getHttpClientCustomizer () {
                return httpClientCustomizer;
            }

            public boolean simpleArgsParsing () {
                return simpleArgsParsing;
            }

            public static final class Builder {
                private List<BiFunction<HttpRequest, Supplier<ClassicHttpResponse>, ClassicHttpResponse>> interceptors
                        = new ArrayList<> ();
                private List<String> placeHolders;
                private HttpClientConnectionManager connectionManager;

                private Consumer<HttpContext> contextTester;
                private Consumer<org.apache.hc.client5.http.impl.classic.HttpClientBuilder> httpClientCustomizer;
                private boolean simpleArgsParsing = false;

                private Builder () {
                }

                public Builder interceptor (BiFunction<HttpRequest, Supplier<ClassicHttpResponse>, ClassicHttpResponse> val) {
                    interceptors.add (val);
                    return this;
                }

                public Builder placeHolders (List<String> val) {
                    placeHolders = val;
                    return this;
                }

                public Builder connectionManager (HttpClientConnectionManager val) {
                    connectionManager = val;
                    return this;
                }

                public Builder mockedNetworkAccess () {
                    connectionManager = new MockNetworkAccess();
                    return this;
                }

                public Builder contextTester (Consumer<HttpContext> val) {
                    contextTester = val;
                    return this;
                }

                public Builder httpClientCustomizer (Consumer<org.apache.hc.client5.http.impl.classic.HttpClientBuilder> val) {
                    httpClientCustomizer = val;
                    return this;
                }

                public Builder simpleArgsParsing () {
                    simpleArgsParsing = true;
                    return this;
                }

                public CurlJavaOptions build () {
                    return new CurlJavaOptions (this);
                }
            }
        }

        CurlArgumentsBuilder () {
        }

        public CurlArgumentsBuilder javaOptions (CurlJavaOptions curlJavaOptions) {
            this.curlJavaOptions = curlJavaOptions;
            return this;
        }

        public String $ (final String url) throws CurlException {
            this.curlCommand.append (url).append (" ");
            return Curl.$ (this.curlCommand.toString (), curlJavaOptions);
        }

        public CompletableFuture<String> $Async (final String url) throws CurlException {
            this.curlCommand.append (url).append (" ");
            return Curl.$Async (this.curlCommand.toString (), curlJavaOptions);
        }

        public HttpResponse run (final String url) throws CurlException {
            this.curlCommand.append (url).append (" ");
            return DEFAULT.curl (this.curlCommand.toString (), curlJavaOptions);
        }

        public CompletableFuture<ClassicHttpResponse> runAsync (final String url) throws CurlException {
            this.curlCommand.append (url).append (" ");
            return Curl.curlAsync (this.curlCommand.toString (), curlJavaOptions);
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

}
