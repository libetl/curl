package org.toilelibre.libe.curl;

import org.apache.commons.cli.*;
import org.toilelibre.libe.curl.Curl.*;

import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import static java.lang.Integer.*;
import static java.util.Optional.*;

final class ReadArguments {
    private static final Pattern PLACEHOLDER_REGEX = Pattern.compile ("^\\$curl_placeholder_[0-9]+$");
    private static final Map<String, List<String>> CACHED_ARGS_MATCHES = new HashMap<> ();

    static CommandLine getCommandLineFromRequest (final String requestCommand, final List<String> placeholderValues) {

        // configure a parser
        final DefaultParser parser = new DefaultParser ();

        final String requestCommandWithoutBasename = requestCommand.replaceAll ("^[ ]*curl[ ]*", " ") + " ";
        final String[] args = ReadArguments.getArgsFromCommand (requestCommandWithoutBasename, placeholderValues);
        final CommandLine commandLine;
        try {
            commandLine = parser.parse (Arguments.ALL_OPTIONS, args);
        } catch (final ParseException e) {
            new HelpFormatter ().printHelp ("curl [options] url", Arguments.ALL_OPTIONS);
            throw new CurlException (e);
        }
        return commandLine;
    }

    private static List<String> asMatches (Pattern regex, String input) {
        Matcher matcher = regex.matcher (input);
        List<String> result = new ArrayList<> ();
        while (matcher.find ()){
            result.add (matcher.group (1));
        }
        return result;
    }


    private static String[] getArgsFromCommand (final String requestCommandWithoutBasename,
                                                final List<String> placeholderValues) {
        final String requestCommandInput = requestCommandWithoutBasename.replaceAll ("\\s+-([a-zA-Z0-9])\\s+", " -$1 ");
        final List<String> matches;
        if (CACHED_ARGS_MATCHES.containsKey (requestCommandInput)) {
            matches = CACHED_ARGS_MATCHES.get (requestCommandInput);
        }else{
            matches = asMatches (Arguments.ARGS_SPLIT_REGEX, requestCommandInput);
            CACHED_ARGS_MATCHES.put (requestCommandInput, matches);
        }

        return ofNullable (matches).map (List :: stream).orElse (Stream.empty ()).map (match -> {
            String argument = ReadArguments.removeSlashes (match.trim ());
            if (PLACEHOLDER_REGEX.matcher (argument).matches ())
                return placeholderValues.get (parseInt (argument.substring ("$curl_placeholder_".length ())));
            else return argument;

        }).toArray (String[] ::new);
    }

    private static String removeSlashes (final String arg) {
        if (arg.length () == 0) {
            return arg;
        }
        if (arg.charAt (0) == '\"') {
            return arg.substring (1, arg.length () - 1).replaceAll ("\\\"", "\"");
        }
        if (arg.charAt (0) == '\'') {
            return arg.substring (1, arg.length () - 1).replaceAll ("\\\'", "\'");
        }
        return arg;
    }
}
