package org.toilelibre.libe.curl;

import org.apache.commons.cli.*;
import org.apache.http.entity.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import static java.net.URLEncoder.encode;
import static java.util.Arrays.stream;
import static org.toilelibre.libe.curl.IOUtils.dataBehind;

final class PayloadReader {
    private static final Pattern CONTENT_TYPE_ENCODING =
            Pattern.compile("\\s*content-type\\s*:[^;]+;\\s*charset\\s*=\\s*(.*)", Pattern.CASE_INSENSITIVE);

    static AbstractHttpEntity getData (final CommandLine commandLine) {
        if (commandLine.hasOption (Arguments.DATA.getOpt ())) {
            return simpleDataFrom(commandLine);
        }
        if (commandLine.hasOption (Arguments.DATA_BINARY.getOpt ())) {
            return binaryDataFrom(commandLine);
        }
        if (commandLine.hasOption (Arguments.DATA_URLENCODE.getOpt ())) {
            try {
                return new StringEntity(stream(commandLine.getOptionValues(Arguments.DATA_URLENCODE.getOpt ()))
                        .map(PayloadReader::urlEncodedDataFrom)
                        .collect(Collectors.joining("&")));
            } catch (final UnsupportedEncodingException e) {
                throw new Curl.CurlException (e);
            }
        }
        return null;
    }

    private static StringEntity simpleDataFrom(CommandLine commandLine) {
        try {
            Charset encoding = charsetReadFromThe(commandLine).orElse(Charset.forName("UTF-8"));
            return new StringEntity (commandLine.getOptionValue (Arguments.DATA.getOpt ()), encoding);
        } catch (final IllegalArgumentException e) {
            throw new Curl.CurlException (e);
        }
    }

    private static InputStreamEntity binaryDataFrom(CommandLine commandLine) {
        final String value = commandLine.getOptionValue (Arguments.DATA_BINARY.getOpt ());
        if (value.indexOf('@') == 0) {
            return new InputStreamEntity(new ByteArrayInputStream(dataBehind(value)));
        }
        return new InputStreamEntity(new ByteArrayInputStream(value.getBytes()));
    }

    private static Optional<Charset> charsetReadFromThe(CommandLine commandLine) {

        return stream (Optional.ofNullable (commandLine.getOptionValues (Arguments.HEADER.getOpt ())).orElse (new String[0]))
                .filter (header -> header != null && CONTENT_TYPE_ENCODING.asPredicate ().test (header))
                .findFirst ().map (correctHeader -> {
                    final Matcher matcher = CONTENT_TYPE_ENCODING.matcher (correctHeader);
                    if (!matcher.find ()) return null;
                    return Charset.forName (matcher.group (1));});
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
            throw new Curl.CurlException (e);
        }
    }
}
