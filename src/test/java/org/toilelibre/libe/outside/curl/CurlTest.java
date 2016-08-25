package org.toilelibre.libe.outside.curl;

import java.io.File;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.fest.assertions.Assertions;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.toilelibre.libe.curl.Curl;
import org.toilelibre.libe.curl.Curl.CurlException;
import org.toilelibre.libe.outside.monitor.RequestMonitor;

public class CurlTest {

    @BeforeClass
    public static void startRequestMonitor () {
        if (System.getProperty ("skipServer") == null) {
            RequestMonitor.start ();
        }
    }

    @AfterClass
    public static void stopRequestMonitor () {
        if (System.getProperty ("skipServer") == null) {
            RequestMonitor.stop ();
        }
    }

    private String $ (final String requestCommand) {
        return Curl.$ (String.format (requestCommand, RequestMonitor.port ()));
    }

    private void assertFound (final HttpResponse curlResponse) {
        Assertions.assertThat (curlResponse).isNotNull ();
        Assertions.assertThat (this.statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_MOVED_TEMPORARILY);
    }

    private void assertOk (final HttpResponse curlResponse) {
        Assertions.assertThat (curlResponse).isNotNull ();
        Assertions.assertThat (this.statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_OK);
    }

    private void assertUnauthorized (final HttpResponse curlResponse) {
        Assertions.assertThat (curlResponse).isNotNull ();
        Assertions.assertThat (this.statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_UNAUTHORIZED);
    }

    private int statusCodeOf (final HttpResponse response) {
        return response.getStatusLine ().getStatusCode ();
    }

    private HttpResponse curl (final String requestCommand) {
        return Curl.curl (String.format (requestCommand, RequestMonitor.port ()));
    }

    @Test
    public void curlRoot () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/"));
    }

    @Test
    public void curlTlsV12 () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/ --tlsv1.2"));
    }

    @Test
    public void curlBadHeaderFormatIgnored () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -H 'toto' https://localhost:%d/public/"));
    }

    @Test
    public void curlDER () {
        this.assertOk (this.curl ("-k --cert-type DER --cert src/test/resources/clients/libe/libe.der:mylibepass --key src/test/resources/clients/libe/libe.key.der --key-type DER https://localhost:%d/public/"));
    }

    @Test
    public void curlHalfPemHalfPKCS12 () {
        this.assertOk (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass --key-type PEM --key src/test/resources/clients/libe/libe.pem https://localhost:%d/public/"));
    }

    @Test
    public void curlJKS () {
        this.assertOk (this.curl ("-k --cert-type JKS --cert src/test/resources/clients/libe/libe.jks:mylibepass https://localhost:%d/public/"));
    }

    @Test
    public void curlOfReadCurlOfReadCurl () {
        this.assertOk (this.curl (this.$ (this.$ (this.$ (this.$ ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/"))))));
    }

    @Test
    public void curlPKCS12 () {
        this.assertOk (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass https://localhost:%d/public/"));
    }

    @Test
    public void curlToRedirectionWithFollowRedirectParam () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -L https://localhost:%d/public/redirection"));
    }

    @Test
    public void curlToRedirectionWithoutFollowRedirectParam () {
        this.assertFound (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/redirection"));
    }

    @Test
    public void curlToUnauthorized () {
        this.assertUnauthorized (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/unauthorized"));
    }

    @Test
    public void curlWithCaCert () {
        this.assertOk (this.curl ("-k --cacert src/test/resources/ca/fakeCa.crt --cert-type PEM --cert src/test/resources/clients/libe/libe.pem:mylibepass https://localhost:%d/public/"));
    }

    @Test
    public void curlWithHeaders () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -H'Host: localhost' -H'Authorization: 45e03eb2-8954-40a3-8068-c926f0461182' https://localhost:%d/public/v1/coverage/sncf/journeys?from=admin:7444extern"));
    }

    @Test
    public void loginCorrectLoginCurlCommand () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -L -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u user:password 'https://localhost:%d/private/login'"));
    }

    @Test
    public void loginWithIncorrectLoginCurlCommand () {
        this.assertUnauthorized (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u foo:bar 'https://localhost:%d/private/login'"));
    }

    @Test
    public void readCurlCommand () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public/curlCommand1?param1=value1&param2=value2'"));
    }

    @Test
    public void readCurlOfCurlCommand () {
        this.assertOk (this.curl (this.$ ("-k -E src/test/resources/clients/libe/libe.pem -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public/curlCommand2?param1=value1&param2=value2'")));
    }

    @Test
    public void readCurlPublicRoot () {
        this.assertOk (this.curl (this.$ ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/")));
    }

    @Test
    public void readCurlWithHeaders () {
        this.assertOk (this.curl (this.$ ("-k -E src/test/resources/clients/libe/libe.pem -H'Host: localhost' -H'Authorization: 45e03eb2-8954-40a3-8068-c926f0461182' https://localhost:%d/public/v1/coverage/sncf/journeys?from=admin:7444extern")));
    }

    @Test
    public void tryToLoginAnonymouslyWithCurlCommand () {
        this.assertUnauthorized (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/private/login'"));
    }

    @Test
    public void withForm () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -F 'toto=titi;foo=bar' -F 'tutu=tata' -X POST -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public/form'"));
    }

    @Test
    public void withJsonBody () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X POST 'https://localhost:%d/public/json' -d '{\"var1\":\"val1\",\"var2\":\"val2\"}'"));
    }

    @Test
    public void withFileForm () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -F 'toto=titi' -F 'script=@src/test/resources/test.sh' -X POST -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public/form'"));
    }

    @Test
    public void withUserAgent () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X GET -A 'toto' -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public'"));
    }

    @Test
    public void outputFile () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X GET -A 'toto' -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public' -o target/classes/downloadedCurl"));
        Assert.assertTrue (new File ("target/classes/downloadedCurl").exists ());
    }

    @Test (expected = CurlException.class)
    public void curlCertNotFound () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/toto.pem https://localhost:%d/public/"));
    }

    @Test (expected = CurlException.class)
    public void readHelp () {
        this.curl ("--help");
    }

    @Test (expected = CurlException.class)
    public void withBadForm () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -F 'totoghghgh' -X POST -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public/form'"));
    }

    @Test (expected = CurlException.class)
    public void curlRootWithoutClientCertificate () {
        this.$ ("curl -k https://localhost:%d/public/");
    }

    @Test (expected = CurlException.class)
    public void curlRootWithoutTrustingInsecure () {
        this.$ ("curl https://localhost:%d/public/");
    }

    @Test (expected = CurlException.class)
    public void curlTlsV11 () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/ --tlsv1.1"));
    }

    @Test (expected = CurlException.class)
    public void curlTlsV10 () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/ --tlsv1.0"));
    }

    @Test (expected = CurlException.class)
    public void curlTlsV1 () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/ -1"));
    }

    @Test (expected = CurlException.class)
    public void curlSslV2 () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/ -2"));
    }

    @Test (expected = CurlException.class)
    public void curlSslV3 () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/ -3"));
    }
}
