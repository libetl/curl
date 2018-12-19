package org.toilelibre.libe.curl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.toilelibre.libe.curl.Curl.CurlException;

import static java.lang.Integer.parseInt;

final class ReadArguments {
    private static final Pattern PLACEHOLDER_REGEX = Pattern.compile("^\\$curl_placeholder_[0-9]+$"); 

    static CommandLine getCommandLineFromRequest (final String requestCommand, final List<String> placeholderValues) {

        // configure a parser
        final DefaultParser parser = new DefaultParser ();

        final String requestCommandWithoutBasename = requestCommand.replaceAll ("^[ ]*curl[ ]*", " ") + " ";
        final String [] args = ReadArguments.getArgsFromCommand (requestCommandWithoutBasename, placeholderValues);
        final CommandLine commandLine;
        try {
            commandLine = parser.parse (Arguments.ALL_OPTIONS, args);
        } catch (final ParseException e) {
            new HelpFormatter ().printHelp ("curl [options] url", Arguments.ALL_OPTIONS);
            throw new CurlException (e);
        }
        return commandLine;
    }

    private static String [] getArgsFromCommand (final String requestCommandWithoutBasename, final List<String> placeholderValues) {
        final String requestCommandInput = requestCommandWithoutBasename.replaceAll ("\\s+-([a-zA-Z0-9])", " -$1 ");
        final Matcher matcher = Arguments.ARGS_SPLIT_REGEX.matcher (requestCommandInput);
        final List<String> args = new ArrayList<> ();
        while (matcher.find ()) {
            String argument = ReadArguments.removeSlashes (matcher.group (1).trim ());
            if (PLACEHOLDER_REGEX.matcher(argument).matches())
                 args.add(placeholderValues.get(parseInt(argument.substring("$curl_placeholder_".length()))));
            else args.add(argument);
        }
        return args.toArray (new String[0]);
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
