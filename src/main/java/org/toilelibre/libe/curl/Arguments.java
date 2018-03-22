package org.toilelibre.libe.curl;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.regex.Pattern;

final class Arguments {

    final static Options ALL_OPTIONS      = new Options ();

    final static Pattern ARGS_SPLIT_REGEX = Pattern.compile("((?:(?:([^'\"\\s]+)(?=[\\s\"']))|(?:\"((?:[^\"]|(?<=\\\\)\")*)\"|(?:'((?:[^']|'(?! )[^']*')*)')|^[^\\s]+$|^[^\\s]+(?= )|(?<= )[^ ]+$)[^\\s\"']*))");

    final static Option  AUTH             = Arguments.add (Option.builder ("u").longOpt ("username").desc ("credentials").required (false).hasArg (true).desc ("user:password").build ());

    final static Option  CA_CERT          = Arguments.add (Option.builder ("cacert").longOpt ("cacert").desc ("CA certificate").required (false).hasArg (true).desc ("CA_CERT").build ());

    final static Option  CERT             = Arguments.add (Option.builder ("E").longOpt ("cert").desc ("client certificate").required (false).hasArg (true).desc ("CERT[:password]").build ());

    final static Option  CERT_TYPE        = Arguments.add (Option.builder ("ct").longOpt ("cert-type").desc ("certificate type").required (false).hasArg (true).desc ("PEM|P12|JKS|DER|ENG").build ());

    final static Option  DATA             = Arguments.add (Option.builder ("d").longOpt ("data").desc ("Data").required (false).hasArg ().argName ("payload").build ());

    final static Option  DATA_BINARY      = Arguments.add (Option.builder ("databinary").longOpt ("data-binary").desc ("http post binary data").required (false).hasArg ().argName ("payload").build ());

    final static Option  DATA_URLENCODE   = Arguments.add (Option.builder ("dataurlencode").longOpt ("data-urlencode").desc ("Data to URLEncode").required (false).hasArg ().argName ("payload").build ());

    final static Option  FOLLOW_REDIRECTS = Arguments.add (Option.builder ("L").longOpt ("location").desc ("follow redirects").required (false).hasArg (false).build ());

    final static Option  FORM             = Arguments.add (Option.builder ("F").longOpt ("form").desc ("http multipart post data").required (false).hasArg (true).build ());

    final static Option  HEADER           = Arguments.add (Option.builder ("H").longOpt ("header").desc ("Header").required (false).hasArg ().argName ("headerValue").build ());

    final static Option  HTTP_METHOD      = Arguments.add (Option.builder ("X").longOpt ("request").desc ("Http Method").required (false).hasArg ().argName ("method").build ());

    final static Option  KEY              = Arguments.add (Option.builder ("key").longOpt ("key").desc ("key").required (false).hasArg (true).desc ("KEY").build ());

    final static Option  KEY_TYPE         = Arguments.add (Option.builder ("kt").longOpt ("key-type").desc ("key type").required (false).hasArg (true).desc ("PEM|P12|JKS|DER|ENG").build ());

    final static Option  NTLM             = Arguments.add (Option.builder ("ntlm").longOpt ("ntlm").desc ("NTLM auth").required (false).hasArg (false).build ());

    final static Option  OUTPUT           = Arguments.add (Option.builder ("o").longOpt ("output").desc ("write to file").required (false).hasArg (true).argName ("FILE").build ());

    final static Option  PROXY            = Arguments.add (Option.builder ("x").longOpt ("proxy").desc ("use the specified HTTP proxy").required (false).hasArg (true).argName ("<[protocol://][user:password@]proxyhost[:port]>").build ());

    final static Option  TLS_V1           = Arguments.add (Option.builder ("1").longOpt ("tlsv1").desc ("use >= TLSv1 (SSL)").required (false).hasArg (false).build ());

    final static Option  TLS_V10          = Arguments.add (Option.builder ("tlsv10").longOpt ("tlsv1.0").desc ("use TLSv1.0 (SSL)").required (false).hasArg (false).build ());

    final static Option  TLS_V11          = Arguments.add (Option.builder ("tlsv11").longOpt ("tlsv1.1").desc ("use TLSv1.1 (SSL)").required (false).hasArg (false).build ());

    final static Option  TLS_V12          = Arguments.add (Option.builder ("tlsv12").longOpt ("tlsv1.2").desc ("use TLSv1.2 (SSL)").required (false).hasArg (false).build ());

    final static Option  SSL_V2           = Arguments.add (Option.builder ("2").longOpt ("sslv2").desc ("use SSLv2 (SSL)").required (false).hasArg (false).build ());

    final static Option  SSL_V3           = Arguments.add (Option.builder ("3").longOpt ("sslv3").desc ("use SSLv3 (SSL)").required (false).hasArg (false).build ());

    final static Option  TRUST_INSECURE   = Arguments.add (Option.builder ("k").longOpt ("insecure").desc ("trust insecure").required (false).hasArg (false).build ());

    final static Option  USER_AGENT       = Arguments.add (Option.builder ("A").longOpt ("user-agent").desc ("user agent").required (false).hasArg (true).build ());

    final static Option  VERSION          = Arguments.add (Option.builder ("V").longOpt ("version").desc ("get the version of this library").required (false).hasArg (false).build ());

    private static Option add (final Option option) {
        Arguments.ALL_OPTIONS.addOption (option);
        return option;
    }
}
