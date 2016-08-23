package org.toilelibre.libe.curl;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicHeader;
import org.toilelibre.libe.curl.Curl.CurlException;

class HttpRequestProvider {

    static HttpUriRequest prepareRequest (final CommandLine commandLine) throws CurlException {

        final HttpUriRequest request = HttpRequestProvider.getBuilder (commandLine);

        HttpRequestProvider.setData (commandLine, request);

        HttpRequestProvider.setForm (commandLine, request);

        HttpRequestProvider.setHeaders (commandLine, request);

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

    private static HttpUriRequest getBuilder (final CommandLine cl) throws CurlException {
        try {
            final String method = cl.getOptionValue (Arguments.HTTP_METHOD.getOpt ()) == null ? "GET" : cl.getOptionValue (Arguments.HTTP_METHOD.getOpt ());
            return (HttpUriRequest) Class.forName (HttpRequestBase.class.getPackage ().getName () + ".Http" + StringUtils.capitalize (method.toLowerCase ().replaceAll ("[^a-z]", ""))).getConstructor (URI.class).newInstance (new URI (cl.getArgs () [0]));
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException | IllegalStateException | InstantiationException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException | URISyntaxException e) {
            throw new CurlException (e);
        }
    }

    private static void setData (final CommandLine commandLine, final HttpUriRequest request) {
        if (commandLine.hasOption (Arguments.DATA.getOpt ()) && (request instanceof HttpEntityEnclosingRequest)) {
            try {
                ((HttpEntityEnclosingRequest) request).setEntity (new StringEntity (commandLine.getOptionValue (Arguments.DATA.getOpt ()).toString ()));
            } catch (final UnsupportedEncodingException e) {
                throw new CurlException (e);
            }
        }
    }

    private static void setForm (final CommandLine commandLine, final HttpUriRequest request) {
        final String [] forms = Optional.ofNullable (commandLine.getOptionValues (Arguments.FORM.getOpt ())).orElse (new String [0]);

        if (forms.length == 0) {
            return;
        }

        final MultipartEntityBuilder multiPartBuilder = MultipartEntityBuilder.create ();

        Arrays.stream (forms).forEach (arg -> {
            if (arg.indexOf ('=') == -1) {
                throw new IllegalArgumentException ("option -F: is badly used here");
            }
        });

        final List<String> binaryForms = Arrays.stream (forms).filter (arg -> HttpRequestProvider.isBinary (arg.substring (arg.indexOf ('=') + 1))).collect (Collectors.toList ());
        final List<String> textForms = Arrays.asList (forms).stream ().filter (form -> !binaryForms.contains (form)).collect (Collectors.toList ());

        binaryForms.forEach (arg -> multiPartBuilder.addBinaryBody (arg.substring (0, arg.indexOf ('=')), HttpRequestProvider.dataBehind (arg.substring (arg.indexOf ('=') + 1))));
        textForms.forEach (arg -> multiPartBuilder.addTextBody (arg.substring (0, arg.indexOf ('=')), arg.substring (arg.indexOf ('=') + 1)));

        ((HttpEntityEnclosingRequest) request).setEntity (multiPartBuilder.build ());

    }

    private static void setHeaders (final CommandLine commandLine, final HttpUriRequest request) {
        final String [] headers = Optional.ofNullable (commandLine.getOptionValues (Arguments.HEADER.getOpt ())).orElse (new String [0]);

        Arrays.stream (headers).filter (optionAsString -> optionAsString.indexOf (':') != -1).map (optionAsString -> optionAsString.split (":"))
                .map (optionAsArray -> new BasicHeader (optionAsArray [0].trim ().replaceAll ("^\"", "").replaceAll ("\\\"$", "").replaceAll ("^\\'", "").replaceAll ("\\'$", ""), optionAsArray [1].trim ())).forEach (request::addHeader);

        if (commandLine.hasOption (Arguments.USER_AGENT.getOpt ())) {
            request.addHeader ("User-Agent", commandLine.getOptionValue (Arguments.USER_AGENT.getOpt ()));
        }
    }
}
