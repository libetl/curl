package org.toilelibre.libe.curl;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

final class Arguments {

    final static Options ALL_OPTIONS      = new Options ();

    final static String  ARGS_SPLIT_REGEX = "([^'\"][^ ]*|(?:\"(?:[^\"]|\\\\\")+\")|(?:'(?:[^']|[^ ]+')+'))\\s+";

    final static Option  HTTP_METHOD      = Arguments.add (Option.builder ("X").longOpt ("request").desc ("Http Method").required (false).hasArg ().argName ("method").build ());

    final static Option  HEADER           = Arguments.add (Option.builder ("H").longOpt ("header").desc ("Header").required (false).hasArg ().argName ("headerValue").build ());

    final static Option  DATA             = Arguments.add (Option.builder ("d").longOpt ("data").desc ("Data").required (false).hasArg ().argName ("payload").build ());

    final static Option  SILENT           = Arguments.add (Option.builder ("s").longOpt ("silent").desc ("silent").required (false).hasArg (false).build ());

    final static Option  TRUST_INSECURE   = Arguments.add (Option.builder ("k").longOpt ("insecure").desc ("trust insecure").required (false).hasArg (false).build ());

    final static Option  NO_BUFFERING     = Arguments.add (Option.builder ("n").longOpt ("no-buffer").desc ("no buffering").required (false).hasArg (false).build ());

    final static Option  NTLM             = Arguments.add (Option.builder ("ntlm").longOpt ("ntlm").desc ("NTLM auth").required (false).hasArg (false).build ());

    final static Option  AUTH             = Arguments.add (Option.builder ("u").longOpt ("username").desc ("credentials").required (false).hasArg (true).desc ("user:password").build ());

    final static Option  KEY              = Arguments.add (Option.builder ("key").longOpt ("key").desc ("key").required (false).hasArg (true).desc ("KEY").build ());

    final static Option  KEY_TYPE         = Arguments.add (Option.builder ("kt").longOpt ("key-type").desc ("key type").required (false).hasArg (true).desc ("PEM|P12|JKS|DER|ENG").build ());

    final static Option  CERT             = Arguments.add (Option.builder ("E").longOpt ("cert").desc ("client certificate").required (false).hasArg (true).desc ("CERT[:password]").build ());

    final static Option  CERT_TYPE        = Arguments.add (Option.builder ("ct").longOpt ("cert-type").desc ("certificate type").required (false).hasArg (true).desc ("PEM|P12|JKS|DER|ENG").build ());

    final static Option  FOLLOW_REDIRECTS = Arguments.add (Option.builder ("L").longOpt ("location").desc ("follow redirects").required (false).hasArg (false).build ());

    final static Option  USER_AGENT       = Arguments.add (Option.builder ("A").longOpt ("user-agent").desc ("user agent").required (false).hasArg (true).build ());

    private static Option add (final Option option) {
        Arguments.ALL_OPTIONS.addOption (option);
        return option;
    }
}
