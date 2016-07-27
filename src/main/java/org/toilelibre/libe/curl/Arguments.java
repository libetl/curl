package org.toilelibre.libe.curl;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

final class Arguments {
    
    final static String      ARGS_SPLIT_REGEX = "([^'\"][^ ]*|(?:\"(?:[^\"]|\\\\\")+\")|(?:'(?:[^']|[^ ]+')+'))\\s+";
    
    final static Option      HTTP_METHOD      = Option.builder ("X").longOpt ("request").desc ("Http Method").required (false).hasArg ().argName ("method").build ();

    final static Option      HEADER           = Option.builder ("H").longOpt ("header").desc ("Header").required (false).hasArg ().argName ("headerValue").build ();

    final static Option      DATA             = Option.builder ("d").longOpt ("data").desc ("Data").required (false).hasArg ().argName ("payload").build ();

    final static Option      SILENT           = Option.builder ("s").longOpt ("silent").desc ("silent").required (false).hasArg (false).build ();

    final static Option      TRUST_INSECURE   = Option.builder ("k").longOpt ("insecure").desc ("trust insecure").required (false).hasArg (false).build ();

    final static Option      NO_BUFFERING     = Option.builder ("n").longOpt ("no-buffer").desc ("no buffering").required (false).hasArg (false).build ();

    final static Option      NTLM             = Option.builder ("ntlm").longOpt ("ntlm").desc ("NTLM auth").required (false).hasArg (false).build ();

    final static Option      AUTH             = Option.builder ("u").longOpt ("username").desc ("credentials").required (false).hasArg (true).desc ("user:password").build ();
    
    final static Option      CERT             = Option.builder ("E").longOpt ("cert").desc ("client certificate").required (false).hasArg (true).desc ("CERT[:password]").build ();
    
    final static Option      CERT_TYPE        = Option.builder ("certtype").longOpt ("cert-type").desc ("certificate type").required (false).hasArg (true).desc ("DER|PEM|ENG").build ();
    
    final static Option      FOLLOW_REDIRECTS = Option.builder ("L").longOpt ("location").desc ("follow redirects").required (false).hasArg (false).build ();

    final static Options     OPTIONS          = new Options ().addOption (Arguments.HTTP_METHOD).addOption (Arguments.HEADER).addOption (Arguments.DATA).addOption (Arguments.SILENT)
            .addOption (Arguments.TRUST_INSECURE).addOption (Arguments.NO_BUFFERING).addOption (Arguments.NTLM).addOption (Arguments.CERT).addOption (Arguments.AUTH).addOption (Arguments.FOLLOW_REDIRECTS);
}
