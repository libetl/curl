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

final class ReadArguments {

	static CommandLine getCommandLineFromRequest(final String requestCommand) {

		// configure a parser
		final DefaultParser parser = new DefaultParser();

		final String requestCommandWithoutBasename = requestCommand.replaceAll("^[ ]*curl[ ]*", " ") + " ";
		final String[] args = ReadArguments.getArgsFromCommand(requestCommandWithoutBasename);
		final CommandLine commandLine;
		try {
			commandLine = parser.parse(Arguments.ALL_OPTIONS, args);
		} catch (final ParseException e) {
			new HelpFormatter().printHelp("curl [options] url", Arguments.ALL_OPTIONS);
			throw new CurlException(e);
		}
		return commandLine;
	}

	private static String[] getArgsFromCommand(final String requestCommandWithoutBasename) {
		final String requestCommandInput = requestCommandWithoutBasename.replaceAll("\\s+-([a-zA-Z0-9])", " -$1 ");
		final Matcher matcher = Pattern.compile(Arguments.ARGS_SPLIT_REGEX).matcher(requestCommandInput);
		final List<String> args = new ArrayList<>();
		while (matcher.find()) {
			args.add(ReadArguments.removeSlashes(matcher.group(1).trim()));
		}
		return args.toArray(new String[args.size()]);
	}

	private static String removeSlashes(final String arg) {
		if (arg.charAt(0) == '\"') {
			return arg.substring(1, arg.length() - 1).replaceAll("\\\"", "\"");
		}
		if (arg.charAt(0) == '\'') {
			return arg.substring(1, arg.length() - 1).replaceAll("\\\'", "\'");
		}
		return arg;
	}
}
