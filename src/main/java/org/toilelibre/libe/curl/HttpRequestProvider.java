package org.toilelibre.libe.curl;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.*;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.VersionInfo;
import org.toilelibre.libe.curl.Curl.CurlException;

import java.net.*;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.toilelibre.libe.curl.IOUtils.isFile;
import static org.toilelibre.libe.curl.PayloadReader.getData;

final class HttpRequestProvider {

    static HttpUriRequest prepareRequest (final CommandLine commandLine) throws CurlException {

        final String method = getMethod (commandLine);
        final RequestBuilder request = wrapInRequestBuilder(method, commandLine.getArgs ()[0]);

        if (asList("DELETE", "PATCH", "POST", "PUT").contains (method.toUpperCase ())) {
            request.setEntity (getData (commandLine));

            if (request.getEntity () == null) {
                request.setEntity (HttpRequestProvider.getForm (commandLine));
            }
        }

        HttpRequestProvider.setHeaders (commandLine, request);

        request.setConfig (HttpRequestProvider.getConfig (commandLine));

        return request.build ();

    }

    private static String getMethod (final CommandLine cl) throws CurlException {
        return cl.getOptionValue (Arguments.HTTP_METHOD.getOpt ()) == null ? determineVerbWithoutArgument(cl) : cl.getOptionValue (Arguments.HTTP_METHOD.getOpt ());
    }

    private static RequestBuilder wrapInRequestBuilder (String method, String uriAsString) {
        try {
            return RequestBuilder.create (method).setUri (new URI (uriAsString));
        } catch (URISyntaxException e) {
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


    private static HttpEntity getForm (final CommandLine commandLine) {
        final String [] forms = Optional.ofNullable (commandLine.getOptionValues (Arguments.FORM.getOpt ())).orElse (new String [0]);

        if (forms.length == 0) {
            return null;
        }

        final MultipartEntityBuilder multiPartBuilder = MultipartEntityBuilder.create ();

        stream (forms).forEach (arg -> {
            if (arg.indexOf ('=') == -1) {
                throw new IllegalArgumentException ("option -F: is badly used here");
            }
        });

        final List<String> fileForms = stream (forms).filter (arg -> isFile(arg.substring (arg.indexOf ('=') + 1))).collect (toList ());
        final List<String> textForms = stream (forms).filter (form -> !fileForms.contains (form)).collect (toList ());

        fileForms.forEach (arg -> multiPartBuilder.addPart(arg.substring (0, arg.indexOf ('=')),
                new FileBody(IOUtils.getFile( (arg.substring (arg.indexOf ("=@") + 2))))));
        textForms.forEach (arg -> multiPartBuilder.addTextBody (arg.substring (0, arg.indexOf ('=')), arg.substring (arg.indexOf ('=') + 1)));

        return multiPartBuilder.build ();

    }

    private static void setHeaders (final CommandLine commandLine, final RequestBuilder request) {
        final String [] headers = Optional.ofNullable (commandLine.getOptionValues (Arguments.HEADER.getOpt ())).orElse (new String [0]);

        List<BasicHeader> basicHeaders =
                stream (headers).filter (optionAsString -> optionAsString.indexOf (':') != -1).map (optionAsString -> optionAsString.split (":"))
                .map (optionAsArray -> new BasicHeader (optionAsArray [0].trim ().replaceAll ("^\"", "").replaceAll ("\\\"$", "").replaceAll ("^\\'", "").replaceAll ("\\'$", ""),
                        String.join(":", asList(optionAsArray).subList(1, optionAsArray.length)).trim ())).collect (Collectors.toList ());

        basicHeaders.forEach (request::addHeader);

        if (basicHeaders.stream ().noneMatch (h -> Objects.equals(h.getName().toLowerCase(), "user-agent")) &&
                commandLine.hasOption (Arguments.USER_AGENT.getOpt ())) {
            request.addHeader ("User-Agent", commandLine.getOptionValue (Arguments.USER_AGENT.getOpt ()));
        }

        if (basicHeaders.stream ().noneMatch (h -> Objects.equals(h.getName().toLowerCase(), "user-agent")) &&
                !commandLine.hasOption (Arguments.USER_AGENT.getOpt ())) {
            request.addHeader ("User-Agent",
                    Curl.class.getPackage ().getName () + "/" + Version.VERSION +
                            VersionInfo.getUserAgent (", Apache-HttpClient",
                                    "org.apache.http.client", HttpRequestProvider.class));
        }

        if (commandLine.hasOption (Arguments.DATA_URLENCODE.getOpt ())) {
            request.addHeader ("Content-Type", "application/x-www-form-urlencoded");
        }

        if (commandLine.hasOption(Arguments.NO_KEEPALIVE.getOpt ())){
            request.addHeader (HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
        }

        if (commandLine.hasOption(Arguments.PROXY_USER.getOpt ())) {
            request.addHeader ("Proxy-Authorization", "Basic " + Base64.getEncoder ().encodeToString (
                    commandLine.getOptionValue(Arguments.PROXY_USER.getOpt ()).getBytes ()));
        }else if (commandLine.hasOption(Arguments.PROXY.getOpt ()) &&
                commandLine.getOptionValue(Arguments.PROXY.getOpt ()).contains("@")){
            request.addHeader ("Proxy-Authorization", "Basic " + Base64.getEncoder ().encodeToString (
                    commandLine.getOptionValue(Arguments.PROXY.getOpt ())
                            .replaceFirst("^[^/]+/+", "").split("@")[0].getBytes ()));
        }
    }

    private static RequestConfig getConfig (final CommandLine commandLine) {
        final Builder requestConfig = RequestConfig.custom ();

        if (commandLine.hasOption (Arguments.PROXY.getOpt ())) {
            String hostWithoutTrailingSlash = commandLine.getOptionValue (Arguments.PROXY.getOpt ())
                    .replaceFirst ("\\s*/\\s*$", "")
                    .replaceFirst("^[^@]+@", "");
            requestConfig.setProxy (HttpHost.create (hostWithoutTrailingSlash));
        }

        if (commandLine.hasOption (Arguments.CONNECT_TIMEOUT.getOpt ())) {
            requestConfig.setConnectTimeout ((int)((Float.parseFloat(
                commandLine.getOptionValue (Arguments.CONNECT_TIMEOUT.getOpt ()))) * 1000));
        }

        if (commandLine.hasOption (Arguments.MAX_TIME.getOpt ())) {
            requestConfig.setSocketTimeout((int)((Float.parseFloat(
                    commandLine.getOptionValue (Arguments.MAX_TIME.getOpt ()))) * 1000));
        }

        return requestConfig.build ();
    }
}
