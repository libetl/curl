package org.toilelibre.libe.curl;

import org.apache.commons.cli.*;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.impl.io.HttpRequestExecutor;
import org.apache.hc.core5.http.io.HttpClientConnection;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.*;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;
import static java.util.stream.Stream.*;

final class InterceptorsBinder {

    private static final BiFunction<HttpRequest, Supplier<ClassicHttpResponse>, ClassicHttpResponse> EXAMPLE
            = ((request, responseSupplier) -> responseSupplier.get ());

    private static Type EXAMPLE_TYPE;

    static {
        try {
            EXAMPLE_TYPE = InterceptorsBinder.class.getDeclaredField ("EXAMPLE").getGenericType ();
        } catch (NoSuchFieldException e) {
            throw new Curl.CurlException (new IllegalArgumentException (e));
        }
    }

    @SuppressWarnings ("unchecked")
    static void handleInterceptors (CommandLine commandLine, HttpClientBuilder executor, List<BiFunction<HttpRequest, Supplier<ClassicHttpResponse>, ClassicHttpResponse>> additionalInterceptors) {
        final List<BiFunction<HttpRequest, Supplier<ClassicHttpResponse>, ClassicHttpResponse>> interceptors =
                concat (stream (Optional.ofNullable (commandLine.getOptionValues (Arguments.INTERCEPTOR.getOpt ())).orElse (new String[0]))
                        .map (methodName -> {
                            final Class<?> targetClass;
                            try {
                                targetClass = Class.forName (methodName.split ("::")[0]);
                            } catch (ClassNotFoundException e) {
                                return null;
                            }
                            Object newInstance;
                            try {
                                newInstance = targetClass.newInstance ();
                            } catch (InstantiationException | IllegalAccessException e) {
                                newInstance = null;
                            }
                            final Object finalNewInstance = newInstance;
                            try {
                                final BiFunction<HttpRequest, Supplier<ClassicHttpResponse>, ClassicHttpResponse> candidate =
                                        stream (targetClass.getDeclaredFields ()).filter (f ->
                                                EXAMPLE_TYPE.equals (f.getGenericType ()))
                                                .findFirst ()
                                                .map (f -> {
                                                    try {
                                                        f.setAccessible (true);
                                                        return (BiFunction<HttpRequest, Supplier<ClassicHttpResponse>, ClassicHttpResponse>)
                                                                f.get (finalNewInstance);
                                                    } catch (IllegalAccessException e) {
                                                        return null;
                                                    }
                                                }).orElse (null);
                                if (candidate != null) return candidate;
                                final Method targetMethod = stream (targetClass.getDeclaredMethods ()).filter (m ->
                                        methodName.split ("::")[1].equals (m.getName ())).findFirst ().orElse (null);
                                if (targetMethod == null) return null;
                                return (BiFunction<HttpRequest, Supplier<ClassicHttpResponse>, ClassicHttpResponse>)
                                        (request, subsequentCall) -> {
                                            try {
                                                return (ClassicHttpResponse) targetMethod.invoke (finalNewInstance,
                                                        request,
                                                        subsequentCall);
                                            } catch (IllegalAccessException | InvocationTargetException e) {
                                                throw new Curl.CurlException (e);
                                            }
                                        };
                            } catch (ClassCastException e) {
                                return null;
                            }
                        })
                        .filter (Objects::nonNull), additionalInterceptors.stream ())
                        .collect (toList ());
        executor.setRequestExecutor (new HttpRequestExecutor () {
            @Override
            public ClassicHttpResponse execute (ClassicHttpRequest request, HttpClientConnection connection, HttpContext context) {
                Supplier<ClassicHttpResponse> executor = () -> {
                    try {
                        return super.execute (request, connection, context);
                    } catch (IOException | HttpException e) {
                        throw new Curl.CurlException (e);
                    }
                };
                return loop (request, executor, interceptors);
            }

            ClassicHttpResponse loop (HttpRequest request, Supplier<ClassicHttpResponse> realCall,
                              List<BiFunction<HttpRequest, Supplier<ClassicHttpResponse>, ClassicHttpResponse>> remainingInterceptors) {
                if (remainingInterceptors.size () > 0) {
                    BiFunction<HttpRequest, Supplier<ClassicHttpResponse>, ClassicHttpResponse> nextInterceptor =
                            remainingInterceptors.get (0);
                    return nextInterceptor.apply (request, () -> this.loop (request, realCall,
                            remainingInterceptors.subList (1, remainingInterceptors.size ())));
                } else return realCall.get ();
            }
        });
    }
}
