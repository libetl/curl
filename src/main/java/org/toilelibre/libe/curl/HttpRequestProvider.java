package org.toilelibre.libe.curl;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicHeader;
import org.toilelibre.libe.curl.Curl.CurlException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.net.URLEncoder.encode;

class HttpRequestProvider {

    static HttpUriRequest prepareRequest (final CommandLine commandLine) throws CurlException {

        final HttpRequestBase request = HttpRequestProvider.getBuilder (commandLine);

        if (request instanceof HttpEntityEnclosingRequest) {
            final HttpEntityEnclosingRequest requestWithPayload = (HttpEntityEnclosingRequest) request;
            requestWithPayload.setEntity (HttpRequestProvider.getData (commandLine));

            if (requestWithPayload.getEntity () == null) {
                requestWithPayload.setEntity (HttpRequestProvider.getForm (commandLine));
            }
        }

        HttpRequestProvider.setHeaders (commandLine, request);

        request.setConfig (HttpRequestProvider.getConfig (commandLine));

        return request;

    }

    private static boolean isBinary (final String ref) {
        final String fileName = (Optional.ofNullable (ref).orElse ("").trim () + " ");
        if (fileName.charAt (0) != '@') {
            return false;
        }

        final File file = new File (fileName.substring (1).trim ());
        return file.exists () && file.isFile ();
    }

    private static byte [] dataBehind (final String ref) {
        try {
            return IOUtils.toByteArray (new File (ref.substring (1).trim ()));
        } catch (final IOException e) {
            throw new CurlException (e);
        }
    }

    private static HttpRequestBase getBuilder (final CommandLine cl) throws CurlException {
        try {
            final String method = cl.getOptionValue (Arguments.HTTP_METHOD.getOpt ()) == null ? determineVerbWithoutArgument(cl) : cl.getOptionValue (Arguments.HTTP_METHOD.getOpt ());
            return (HttpRequestBase) Class.forName (HttpRequestBase.class.getPackage ().getName () + ".Http" + StringUtils.capitalize (method.toLowerCase ().replaceAll ("[^a-z]", ""))).getConstructor (URI.class).newInstance (new URI (cl.getArgs () [0]));
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException | IllegalStateException | InstantiationException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException | URISyntaxException e) {
            throw new CurlException (e);
        }
    }

    private static String determineVerbWithoutArgument(CommandLine commandLine) {
        if (commandLine.hasOption (Arguments.DATA.getOpt ()) ||
                commandLine.hasOption (Arguments.DATA_URLENCODE.getOpt ()) ||
                commandLine.hasOption(Arguments.FORM.getOpt())) {
            return "POST";
        }
        return "GET";
    }

    private static AbstractHttpEntity getData (final CommandLine commandLine) {
        if (commandLine.hasOption (Arguments.DATA.getOpt ())) {
            return simpleDataFrom(commandLine);
        }
        if (commandLine.hasOption (Arguments.DATA_BINARY.getOpt ())) {
            return binaryDataFrom(commandLine);
        }
        if (commandLine.hasOption (Arguments.DATA_URLENCODE.getOpt ())) {
            try {
                return new StringEntity(Arrays.stream(commandLine.getOptionValues(Arguments.DATA_URLENCODE.getOpt ()))
                        .map(HttpRequestProvider::urlEncodedDataFrom)
                        .collect(Collectors.joining("&")));
            } catch (final UnsupportedEncodingException e) {
                throw new CurlException (e);
            }
        }
        return null;
    }

    private static String urlEncodedDataFrom(String value) {
        if (value.startsWith("=")) {
            value = value.substring(1);
        }
        if (value.indexOf('=') != -1) {
            return value.substring(0, value.indexOf('=') + 1) + encodeOrFail(value.substring(value.indexOf('=') + 1), Charset.defaultCharset());
        }
        if (value.indexOf('@') == 0) {
            return encodeOrFail(new String(dataBehind (value)), Charset.defaultCharset());
        }
        if (value.indexOf('@') != -1) {
            return value.substring(0, value.indexOf('@')) + '=' + encodeOrFail(new String(dataBehind (value.substring(value.indexOf('@')))), Charset.defaultCharset());
        }
        return encodeOrFail(value, Charset.defaultCharset());
    }

    private static String encodeOrFail(String value, Charset encoding) {
        try {
            return encode(value, encoding.name());
        } catch (UnsupportedEncodingException e) {
            throw new CurlException(e);
        }
    }

    private static InputStreamEntity binaryDataFrom(CommandLine commandLine) {
        final String value = commandLine.getOptionValue (Arguments.DATA_BINARY.getOpt ());
        if (value.indexOf('@') == 0) {
            return new InputStreamEntity(new ByteArrayInputStream(dataBehind(value)));
        }
        return new InputStreamEntity(new ByteArrayInputStream(value.getBytes()));
    }

    private static StringEntity simpleDataFrom(CommandLine commandLine) {
        try {
            return new StringEntity (commandLine.getOptionValue (Arguments.DATA.getOpt ()));
        } catch (final UnsupportedEncodingException e) {
            throw new CurlException (e);
        }
    }

    private static HttpEntity getForm (final CommandLine commandLine) {
        final String [] forms = Optional.ofNullable (commandLine.getOptionValues (Arguments.FORM.getOpt ())).orElse (new String [0]);

        if (forms.length == 0) {
            return null;
        }

        final MultipartEntityBuilder multiPartBuilder = MultipartEntityBuilder.create ();

        Arrays.stream (forms).forEach (arg -> {
            if (arg.indexOf ('=') == -1) {
                throw new IllegalArgumentException ("option -F: is badly used here");
            }
        });

        final List<String> binaryForms = Arrays.stream (forms).filter (arg -> HttpRequestProvider.isBinary (arg.substring (arg.indexOf ('=') + 1))).collect (Collectors.toList ());
        final List<String> textForms = Arrays.stream (forms).filter (form -> !binaryForms.contains (form)).collect (Collectors.toList ());

        binaryForms.forEach (arg -> multiPartBuilder.addBinaryBody (arg.substring (0, arg.indexOf ('=')), HttpRequestProvider.dataBehind (arg.substring (arg.indexOf ('=') + 1))));
        textForms.forEach (arg -> multiPartBuilder.addTextBody (arg.substring (0, arg.indexOf ('=')), arg.substring (arg.indexOf ('=') + 1)));

        return multiPartBuilder.build ();

    }

    private static void setHeaders (final CommandLine commandLine, final HttpRequestBase request) {
        final String [] headers = Optional.ofNullable (commandLine.getOptionValues (Arguments.HEADER.getOpt ())).orElse (new String [0]);

        Arrays.stream (headers).filter (optionAsString -> optionAsString.indexOf (':') != -1).map (optionAsString -> optionAsString.split (":"))
                .map (optionAsArray -> new BasicHeader (optionAsArray [0].trim ().replaceAll ("^\"", "").replaceAll ("\\\"$", "").replaceAll ("^\\'", "").replaceAll ("\\'$", ""), optionAsArray [1].trim ())).forEach (request::addHeader);

        if (commandLine.hasOption (Arguments.USER_AGENT.getOpt ())) {
            request.addHeader ("User-Agent", commandLine.getOptionValue (Arguments.USER_AGENT.getOpt ()));
        }

        if (commandLine.hasOption (Arguments.DATA_URLENCODE.getOpt ())) {
            request.addHeader ("Content-Type", "application/x-www-form-urlencoded");
        }
    }

    private static RequestConfig getConfig (final CommandLine commandLine) {
        final Builder requestConfig = RequestConfig.custom ();

        if (commandLine.hasOption (Arguments.PROXY.getOpt ())) {
            requestConfig.setProxy (HttpHost.create (commandLine.getOptionValue (Arguments.PROXY.getOpt ())));
        }

        return requestConfig.build ();
    }
}
