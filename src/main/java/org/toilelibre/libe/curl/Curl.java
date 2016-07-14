package org.toilelibre.libe.curl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.message.BasicHeader;

public class Curl {

    private final static String      ARGS_SPLIT_REGEX = "([^'\"][^ ]*|(?:\"(?:[^\"]|\\\\\")+\")|(?:'(?:[^']|[^ ]+')+'))\\s+";
    
    private final static Option      HTTP_METHOD      = Option.builder ("X").longOpt ("request").desc ("Http Method").required (false).hasArg ().argName ("method").build ();

    private final static Option      HEADER           = Option.builder ("H").longOpt ("header").desc ("Header").required (false).hasArg ().argName ("headerValue").build ();

    private final static Option      DATA             = Option.builder ("d").longOpt ("data").desc ("Data").required (false).hasArg ().argName ("payload").build ();

    private final static Option      SILENT           = Option.builder ("s").longOpt ("silent").desc ("silent").required (false).hasArg (false).build ();

    private final static Option      TRUST_INSECURE   = Option.builder ("k").longOpt ("insecure").desc ("trust insecure").required (false).hasArg (false).build ();

    private final static Option      NO_BUFFERING     = Option.builder ("n").longOpt ("no-buffer").desc ("no buffering").required (false).hasArg (false).build ();

    private final static Option      NTLM             = Option.builder ().longOpt ("ntlm").desc ("NTLM auth").required (false).hasArg (false).build ();

    private final static Option      AUTH             = Option.builder ("u").longOpt ("username").desc ("user:password").required (false).hasArg (true).desc ("credentials")
            .build ();

    private final static Options     OPTIONS          = new Options ().addOption (Curl.HTTP_METHOD).addOption (Curl.HEADER).addOption (Curl.DATA).addOption (Curl.SILENT)
            .addOption (Curl.TRUST_INSECURE).addOption (Curl.NO_BUFFERING).addOption (Curl.NTLM).addOption (Curl.AUTH);

    public static String $t (final String requestCommand) {
        try {
            return Curl.curl (requestCommand).returnContent ().asString ();
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }

    public static String curlT (final Request request) {
        try {
            return Curl.curl (request, null).returnContent ().asString ();
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }
    
    public static Response $ (final String requestCommand) {
        return Curl.curl (requestCommand);
    }

    public static Response curl (final Request request) {
        return Curl.curl (request, null);
    }

    public static Response curl (final Request request, final Executor executor) {

        try {
            return executor == null ? request.execute () : executor.execute (request);
        } catch (final IOException e) {
            throw new RuntimeException (e);
        }
    }

    public static Response curl (final String requestCommand) {
        final CommandLine commandLine = Curl.getCommandLineFromRequest (requestCommand);
        return Curl.curl (Curl.prepareRequest (commandLine), Curl.prepareExecutor (commandLine));
    }

    private static String [] getArgsFromCommand (final String requestCommandWithoutBasename) {
        final String requestCommandInput = requestCommandWithoutBasename.replaceAll ("\\s+-([a-zA-Z0-9])", " -$1 ");
        final Matcher matcher = Pattern.compile (Curl.ARGS_SPLIT_REGEX).matcher (requestCommandInput);
        final List<String> args = new ArrayList<> ();
        while (matcher.find ()) {
            args.add (Curl.removeSlashes (matcher.group (1).trim ()));
        }
        return args.toArray (new String [args.size ()]);

    }

    private static Request getBuilder (final CommandLine cl) {
        try {
            final String method = (cl.getOptionValue (Curl.HTTP_METHOD.getOpt ()) == null ? "GET" : cl.getOptionValue (Curl.HTTP_METHOD.getOpt ())).toString ();
            return (Request) Request.class.getDeclaredMethod (StringUtils.capitalize (method.toLowerCase ().replaceAll ("[^a-z]", "")), String.class).invoke (null,
                    cl.getArgs () [0]);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | IllegalStateException e) {
            throw new RuntimeException (e);
        }
    }

    private static CommandLine getCommandLineFromRequest (final String requestCommand) {

        // configure a parser
        final DefaultParser parser = new DefaultParser ();

        final String requestCommandWithoutBasename = requestCommand.replaceAll ("^[ ]*curl[ ]*", " ") + " ";
        final String [] args = Curl.getArgsFromCommand (requestCommandWithoutBasename);
        CommandLine commandLine;
        try {
            commandLine = parser.parse (Curl.OPTIONS, args);
        } catch (final ParseException e) {
            new HelpFormatter ().printHelp ("curl", Curl.OPTIONS);
            throw new RuntimeException (e);
        }
        return commandLine;
    }

    public static Executor prepareExecutor (final CommandLine commandLine) {
        Executor executor = null;

        String hostname;
        try {
            hostname = InetAddress.getLocalHost ().getHostName ();
        } catch (final UnknownHostException e1) {
            throw new RuntimeException (e1);
        }

        if (commandLine.getOptionValue (Curl.AUTH.getOpt ()) != null) {
            final String [] authValue = commandLine.getOptionValue (Curl.AUTH.getOpt ()).toString ().split ("(?<!\\\\):");
            if (commandLine.hasOption (Curl.NTLM.getOpt ())) {
                final String [] userName = authValue [0].split ("\\\\");
                executor = Executor.newInstance ().auth (new NTCredentials (userName [1], authValue [1], hostname, userName [0]));
            } else {
                executor = Executor.newInstance ().auth (authValue [0], authValue [1]);
            }
        }
        return executor;
    }

    public static Request prepareRequest (final CommandLine commandLine) {

        final Request request = Curl.getBuilder (commandLine);

        final String [] headers = Optional.ofNullable (commandLine.getOptionValues (Curl.HEADER.getOpt ())).orElse (new String [0]);
        Arrays.asList (headers).stream ().map (optionAsString -> optionAsString.split (":"))
                .map (optionAsArray -> new BasicHeader (optionAsArray [0].trim ().replaceAll ("^\"", "").replaceAll ("\\\"$", "").replaceAll ("^\\'", "").replaceAll ("\\'$", ""),
                        optionAsArray [1].trim ()))
                .forEach (basicHeader -> request.addHeader (basicHeader));

        if (commandLine.hasOption (Curl.DATA.getOpt ())) {
            request.bodyByteArray (commandLine.getOptionValue (Curl.DATA.getOpt ()).toString ().getBytes ());
        }

        if (commandLine.hasOption (Curl.TRUST_INSECURE.getOpt ())) {
            HttpsURLConnection.setDefaultHostnameVerifier ( (host, sslSession) -> true);
        }

        return request;

    }

    private static String removeSlashes (final String arg) {
        if (arg.charAt (0) == '\"') {
            return arg.substring (1, arg.length () - 1).replaceAll ("\\\"", "\"");
        }
        if (arg.charAt (0) == '\'') {
            return arg.substring (1, arg.length () - 1).replaceAll ("\\\'", "\'");
        }
        return arg;
    }
}
