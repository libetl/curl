package org.toilelibre.libe.curl;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

class InterceptorsBinder {

    private static final BiFunction<HttpRequest, Supplier<HttpResponse>, HttpResponse> EXAMPLE
            = ((request, responseSupplier) -> responseSupplier.get());

    private static Type EXAMPLE_TYPE;

    static {
        try {
            EXAMPLE_TYPE = InterceptorsBinder.class.getDeclaredField("EXAMPLE").getGenericType();
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static void handleInterceptors(CommandLine commandLine, HttpClientBuilder executor, List<BiFunction<HttpRequest, Supplier<HttpResponse>, HttpResponse>> additionalInterceptors) {
        final List<BiFunction<HttpRequest, Supplier<HttpResponse>, HttpResponse>> interceptors =
                concat(stream(ofNullable(commandLine.getOptionValues(Arguments.INTERCEPTOR.getOpt())).orElse(new String[0]))
                        .map(methodName -> {
                            final Class<?> targetClass;
                            try {
                                targetClass = Class.forName(methodName.split("::")[0]);
                            } catch (ClassNotFoundException e) {
                                return null;
                            }
                            Object newInstance;
                            try {
                                newInstance = targetClass.newInstance();
                            } catch (InstantiationException | IllegalAccessException e) {
                                newInstance = null;
                            }
                            final Object finalNewInstance = newInstance;
                            try {
                                final BiFunction<HttpRequest, Supplier<HttpResponse>, HttpResponse> candidate =
                                        stream(targetClass.getDeclaredFields()).filter(f ->
                                                EXAMPLE_TYPE.equals(f.getGenericType()))
                                                .findFirst()
                                                .map(f -> {
                                                    try {
                                                        f.setAccessible(true);
                                                        return (BiFunction<HttpRequest, Supplier<HttpResponse>, HttpResponse>)
                                                                f.get(finalNewInstance);
                                                    } catch (IllegalAccessException e) {
                                                        return null;
                                                    }
                                                }).orElse(null);
                                if (candidate != null) return candidate;
                                final Method targetMethod = stream(targetClass.getDeclaredMethods()).filter(m ->
                                        methodName.split("::")[1].equals(m.getName())).findFirst().orElse(null);
                                if (targetMethod == null) return null;
                                return (BiFunction<HttpRequest, Supplier<HttpResponse>, HttpResponse>)
                                        (request, subsequentCall) -> {
                                            try {
                                                return (HttpResponse) targetMethod.invoke(finalNewInstance,
                                                        request,
                                                        subsequentCall);
                                            } catch (IllegalAccessException | InvocationTargetException e) {
                                                throw new Curl.CurlException(e);
                                            }
                                        };
                            } catch (ClassCastException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull), additionalInterceptors.stream())
                        .collect(toList());
        executor.setRequestExecutor(new HttpRequestExecutor() {
            @Override
            public HttpResponse execute(HttpRequest request, HttpClientConnection connection, HttpContext context) {
                Supplier<HttpResponse> executor = () -> {
                    try {
                        return super.execute(request, connection, context);
                    } catch (IOException | HttpException e) {
                        throw new Curl.CurlException(e);
                    }
                };
                return loop(request, executor, interceptors);
            }

            HttpResponse loop(HttpRequest request, Supplier<HttpResponse> realCall,
                              List<BiFunction<HttpRequest, Supplier<HttpResponse>, HttpResponse>> remainingInterceptors) {
                if (remainingInterceptors.size() > 0) {
                    BiFunction<HttpRequest, Supplier<HttpResponse>, HttpResponse> nextInterceptor =
                            remainingInterceptors.get(0);
                    return nextInterceptor.apply(request, () -> this.loop(request, realCall,
                            remainingInterceptors.subList(1, remainingInterceptors.size())));
                } else return realCall.get();
            }
        });
    }
}
