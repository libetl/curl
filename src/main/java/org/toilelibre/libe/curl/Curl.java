package org.toilelibre.libe.curl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
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
    
    private final static Option      FOLLOW_REDIRECTS = Option.builder ("L").longOpt ("location").desc ("follow redirects").required (false).hasArg (false).build ();

    private final static Options     OPTIONS          = new Options ().addOption (Curl.HTTP_METHOD).addOption (Curl.HEADER).addOption (Curl.DATA).addOption (Curl.SILENT)
            .addOption (Curl.TRUST_INSECURE).addOption (Curl.NO_BUFFERING).addOption (Curl.NTLM).addOption (Curl.AUTH).addOption (Curl.FOLLOW_REDIRECTS);

    public static String $t (final String requestCommand) {
        try {
            return IOUtils.toString(Curl.curl (requestCommand).getEntity ().getContent ());
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }

    public static String curlT (final HttpUriRequest request) {
        try {
            return IOUtils.toString(Curl.curl (request).getEntity ().getContent ());
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }
    
    public static HttpResponse $ (final String requestCommand) {
        return Curl.curl (requestCommand);
    }

    public static HttpResponse curl (final HttpUriRequest request) {
        return Curl.curl (request, null);
    }

    public static HttpResponse curl (final HttpUriRequest request, final HttpClient executor) {

        try {
            return sendRequest (request, executor);
        } catch (final IOException e) {
            throw new RuntimeException (e);
        }
    }

    private static HttpResponse sendRequest (HttpUriRequest request, HttpClient executor) throws IOException {
        return executor.execute (request);
    }

    public static HttpResponse curl (final String requestCommand) {
        final CommandLine commandLine = Curl.getCommandLineFromRequest (requestCommand);
        return Curl.curl (Curl.prepareRequest (commandLine), Curl.prepareHttpClient (commandLine));
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

    private static HttpUriRequest getBuilder (final CommandLine cl) {
        try {
            final String method = (cl.getOptionValue (Curl.HTTP_METHOD.getOpt ()) == null ? "GET" : cl.getOptionValue (Curl.HTTP_METHOD.getOpt ())).toString ();
            return 
                    (HttpUriRequest) Class.forName (
                            HttpRequestBase.class.getPackage ().getName () + ".Http" + StringUtils.capitalize (method.toLowerCase ().replaceAll ("[^a-z]", "")))
                                          .getConstructor (URI.class)
                                          .newInstance (new URI(cl.getArgs ()[0]));
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException | IllegalStateException | InstantiationException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException | URISyntaxException e) {
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

    public static HttpClient prepareHttpClient (final CommandLine commandLine) {
        HttpClientBuilder executor = HttpClientBuilder.create ();

        String hostname;
        try {
            hostname = InetAddress.getLocalHost ().getHostName ();
        } catch (final UnknownHostException e1) {
            throw new RuntimeException (e1);
        }

        executor = Curl.handleAuthMethod (commandLine, executor, hostname);
        
        if (!commandLine.hasOption (Curl.FOLLOW_REDIRECTS.getOpt ())) { 
            executor.disableRedirectHandling ();
        }
        if (commandLine.hasOption (Curl.TRUST_INSECURE.getOpt ())) {
            executor.setSSLHostnameVerifier ( (host, sslSession) -> true);
        }
        return executor.build ();
    }

    private static HttpClientBuilder handleAuthMethod (final CommandLine commandLine, HttpClientBuilder executor, String hostname) {
        if (commandLine.getOptionValue (Curl.AUTH.getOpt ()) != null) {
            final String [] authValue = commandLine.getOptionValue (Curl.AUTH.getOpt ()).toString ().split ("(?<!\\\\):");
            if (commandLine.hasOption (Curl.NTLM.getOpt ())) {
                final String [] userName = authValue [0].split ("\\\\");
                SystemDefaultCredentialsProvider systemDefaultCredentialsProvider = new SystemDefaultCredentialsProvider ();
                systemDefaultCredentialsProvider.setCredentials (AuthScope.ANY, new NTCredentials (userName [1], authValue [1], hostname, userName [0]));
                executor = executor.setDefaultCredentialsProvider (systemDefaultCredentialsProvider);
            } else {
                BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider ();
                basicCredentialsProvider.setCredentials (new AuthScope (HttpHost.create (commandLine.getArgs ()[0])), new UsernamePasswordCredentials (authValue [0], authValue [1]));
                executor = executor.setDefaultCredentialsProvider (basicCredentialsProvider);
            }
        }
        return executor;
    }

    public static HttpUriRequest prepareRequest (final CommandLine commandLine) {

        final HttpUriRequest request = Curl.getBuilder (commandLine);
        
        if (commandLine.hasOption (Curl.DATA.getOpt ()) && request instanceof HttpEntityEnclosingRequest) {
            try {
                ((HttpEntityEnclosingRequest)request).setEntity (new StringEntity (commandLine.getOptionValue (Curl.DATA.getOpt ()).toString ()));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException (e);
            }
        }
        
        final String [] headers = Optional.ofNullable (commandLine.getOptionValues (Curl.HEADER.getOpt ())).orElse (new String [0]);
        Arrays.asList (headers).stream ().map (optionAsString -> optionAsString.split (":"))
                .map (optionAsArray -> new BasicHeader (optionAsArray [0].trim ().replaceAll ("^\"", "").replaceAll ("\\\"$", "").replaceAll ("^\\'", "").replaceAll ("\\'$", ""),
                        optionAsArray [1].trim ()))
                .forEach (basicHeader -> request.addHeader (basicHeader));

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
