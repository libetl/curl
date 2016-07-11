package org.toilelibre.libe.curl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli2.Argument;
import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.util.HelpFormatter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.message.BasicHeader;

public class Curl {
    
    private final static String ARGS_SPLIT_REGEX = "([^'\"][^ ]*|(?:\"(?:[^\"]|\\\\\")+\")|(?:'(?:[^']|[^ ]+')+'))\\s+";
    
    private final static DefaultOptionBuilder OPTION_BUILDER   = new DefaultOptionBuilder ();
    private final static ArgumentBuilder      ARGUMENT_BUILDER = new ArgumentBuilder ();
    private final static GroupBuilder         GROUP_BUILDER    = new GroupBuilder ();
    
    private final static Option               HTTP_METHOD      = Curl.OPTION_BUILDER.withShortName ("X").withDescription ("Http Method").withRequired (false)
            .withArgument (Curl.ARGUMENT_BUILDER.withName ("method").withMinimum (0).withDefault ("GET").withMaximum (1).create ()).create ();
    
    private final static Option               HEADER           = Curl.OPTION_BUILDER.withShortName ("H").withDescription ("Header").withRequired (false)
            .withArgument (Curl.ARGUMENT_BUILDER.withName ("headerValue").withMinimum (1).withMaximum (1).create ()).create ();
    
    private final static Option               DATA             = Curl.OPTION_BUILDER.withShortName ("d").withDescription ("Data").withRequired (false)
            .withArgument (Curl.ARGUMENT_BUILDER.withName ("dataValue").withMaximum (1).create ()).create ();
    
    private final static Option               SILENT           = Curl.OPTION_BUILDER.withShortName ("s").withDescription ("silent curl").withRequired (false).create ();
    
    private final static Option               TRUST_INSECURE   = Curl.OPTION_BUILDER.withShortName ("k").withDescription ("trust insecure").withRequired (false).create ();
    
    private final static Option               NO_BUFFERING     = Curl.OPTION_BUILDER.withShortName ("N").withDescription ("no buffering").withRequired (false).create ();
    
    private final static Option               NTLM             = Curl.OPTION_BUILDER.withLongName ("ntlm").withDescription ("NTLM auth").withRequired (false).create ();
    
    private final static Option               AUTH             = Curl.OPTION_BUILDER.withShortName ("u").withLongName ("username").withDescription ("user:password")
            .withRequired (false).withArgument (Curl.ARGUMENT_BUILDER.withName ("credentials").withMinimum (1).withMaximum (1).create ()).create ();
    
    private final static Argument             URL              = Curl.ARGUMENT_BUILDER.withName ("url").withMaximum (1).withMinimum (0).create ();
    
    private final static Group                CURL_GROUP       = Curl.GROUP_BUILDER.withOption (Curl.URL).withOption (Curl.HTTP_METHOD).withOption (Curl.HEADER)
            .withOption (Curl.DATA).withOption (Curl.SILENT).withOption (Curl.TRUST_INSECURE).withOption (Curl.NO_BUFFERING).withOption (Curl.AUTH)
            .withOption (Curl.NTLM).create ();
    
    public static Response curl (String requestCommand) {
        return Curl.curl (Curl.prepareRequest (requestCommand), Curl.prepareExecutor (requestCommand));
    }
    
    public static Response curl (Request request) {
        return Curl.curl (request, null);
    }
    
    public static Response curl (Request request, Executor executor) {
        
        try {
            return executor == null ? request.execute () : executor.execute (request);
        } catch (final IOException e) {
            throw new RuntimeException (e);
        }
    }
    
    private static String [] getArgsFromCommand (String requestCommandWithoutBasename) {
        String requestCommandInput = requestCommandWithoutBasename.replaceAll ("\\s+-([a-zA-Z0-9])", " -$1 ");
        Matcher matcher = Pattern.compile (ARGS_SPLIT_REGEX).matcher (requestCommandInput);
        List<String> args = new ArrayList<String> ();
        while (matcher.find ()) {
            args.add (removeSlashes(matcher.group (1).trim ()));
        }
        return args.toArray (new String[args.size ()]);
        
    }
    
    private static String removeSlashes (String arg) {
        if (arg.charAt (0) == '\"') {
            return arg.substring (1, arg.length () - 2).replaceAll ("\\\"", "\"");
        }
        if (arg.charAt (0) == '\'') {
            return arg.substring (1, arg.length () - 2).replaceAll ("\\\'", "\'");
        }
        return arg;
    }
    
    private static Request getBuilder (CommandLine cl, Option httpMethod, Argument url) {
        try {
            return (Request) Request.class.getDeclaredMethod (StringUtils.capitalize (cl.getValue (httpMethod).toString ().toLowerCase ().replaceAll ("[^a-z]", "")), String.class)
                    .invoke (null, cl.getValue (url));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | IllegalStateException e) {
            throw new RuntimeException (e);
        }
    }
    
    private static CommandLine getCommandLineFromRequest (String requestCommand) {
        final HelpFormatter hf = new HelpFormatter ();
        
        // configure a parser
        final Parser parser = new Parser ();
        parser.setGroup (Curl.CURL_GROUP);
        parser.setHelpFormatter (hf);
        parser.setHelpTrigger ("--help");
        final String requestCommandWithoutBasename = requestCommand.replaceAll ("^[ ]*curl[ ]*", " ") + " ";
        final String [] args = Curl.getArgsFromCommand (requestCommandWithoutBasename);
        CommandLine commandLine;
        try {
            commandLine = parser.parse (args);
        } catch (final OptionException e) {
            parser.parseAndHelp (new String [] { "--help" });
            throw new RuntimeException (e);
        }
        return commandLine;
    }
    
    public static Executor prepareExecutor (String requestCommand) {
        Executor executor = null;
        
        final CommandLine commandLine = Curl.getCommandLineFromRequest (requestCommand);
        
        String hostname;
        try {
            hostname = InetAddress.getLocalHost ().getHostName ();
        } catch (final UnknownHostException e1) {
            throw new RuntimeException (e1);
        }
        
        if (commandLine.getValue (Curl.AUTH) != null) {
            final String [] authValue = commandLine.getValue (Curl.AUTH).toString ().split ("(?<!\\\\):");
            if (commandLine.getOptionCount (Curl.NTLM) == 1) {
                final String [] userName = authValue [0].split ("\\\\");
                executor = Executor.newInstance ().auth (new NTCredentials (userName [1], authValue [1], hostname, userName [0]));
            } else {
                executor = Executor.newInstance ().auth (authValue [0], authValue [1]);
            }
        }
        return executor;
    }
    
    @SuppressWarnings ("unchecked")
    public static Request prepareRequest (String requestCommand) {
        
        final CommandLine commandLine = Curl.getCommandLineFromRequest (requestCommand);
        
        final Request request = Curl.getBuilder (commandLine, Curl.HTTP_METHOD, Curl.URL);
        
        ((List<String>) commandLine.getValues (Curl.HEADER)).stream ().map (optionAsString -> optionAsString.split (":"))
                .map (optionAsArray -> new BasicHeader (optionAsArray [0].trim ().replaceAll ("^\"", "").replaceAll ("\\\"$", "").replaceAll ("^\\'", "").replaceAll ("\\'$", ""),
                        optionAsArray [1].trim ()))
                .forEach (basicHeader -> request.addHeader (basicHeader));
        
        if (commandLine.getValue (Curl.DATA) != null) {
            request.bodyByteArray (commandLine.getValue (Curl.DATA).toString ().getBytes ());
        }
        
        return request;
        
    }
}
