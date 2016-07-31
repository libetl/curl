package org.toilelibre.libe.outside.curl;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.fest.assertions.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.toilelibre.libe.curl.Curl;
import org.toilelibre.libe.curl.Curl.CurlException;
import org.toilelibre.libe.outside.monitor.RequestMonitor;

public class CurlTest {

    @BeforeClass
    public static void startRequestMonitor () {
        RequestMonitor.start ();
    }

    @AfterClass
    public static void stopRequestMonitor () {
        RequestMonitor.stop ();
    }

    private HttpResponse curl (final String requestCommand) {
        return Curl.curl (String.format (requestCommand, RequestMonitor.port ()));
    }

    private String $ (final String requestCommand) {
        return Curl.$ (String.format (requestCommand, RequestMonitor.port ()));
    }

    @Test (expected = CurlException.class)
    public void curlRootWithoutTrustingInsecure () {
        this.$ ("curl https://localhost:%d/public/");
    }

    @Test (expected = CurlException.class)
    public void curlRootWithoutClientCertificate () {
        this.$ ("curl -k https://localhost:%d/public/");
    }

    @Test
    public void curlPKCS12 () {
        this.assertOk (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass https://localhost:%d/public/"));
    }

    @Test (/*not*/ expected = CurlException.class)
    public void curlRoot () {
        this.assertOk (this.curl ("-k --cert-type PEM --cert src/test/resources/clients/libe/libe.pem:mylibepass https://localhost:%d/public/"));
    }

    @Test
    public void curlToRedirectionWithoutFollowRedirectParam () {
        this.assertFound (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass https://localhost:%d/public/redirection"));
    }

    @Test
    public void curlToUnauthorized () {
        this.assertUnauthorized (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass https://localhost:%d/public/unauthorized"));
    }

    @Test
    public void curlToRedirectionWithFollowRedirectParam () {
        this.assertOk (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass -L https://localhost:%d/public/redirection"));
    }

    @Test
    public void curlWithHeaders () {
        this.assertOk (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass -H'Host: localhost' -H'Authorization: 45e03eb2-8954-40a3-8068-c926f0461182' https://localhost:%d/public/v1/coverage/sncf/journeys?from=admin:7444extern"));
    }

    @Test
    public void readCurlPublicRoot () {
        this.assertOk (this.curl (this.$ ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass https://localhost:%d/public/")));
    }

    @Test
    public void readCurlWithHeaders () {
        this.assertOk (this.curl (this.$ ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass -H'Host: localhost' -H'Authorization: 45e03eb2-8954-40a3-8068-c926f0461182' https://localhost:%d/public/v1/coverage/sncf/journeys?from=admin:7444extern")));
    }

    @Test
    public void curlOfReadCurlOfReadCurl () {
        this.assertOk (this.curl (this.$ (this.$ (this.$ (this.$ ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass https://localhost:%d/public/"))))));
    }

    @Test
    public void readCurlCommand () {
        this.assertOk (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost'  'https://localhost:%d/public/curlCommand1?param1=value1&param2=value2'"));
    }

    @Test
    public void readCurlOfCurlCommand () {
        this.assertOk (this.curl (this.$ ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost'  'https://localhost:%d/public/curlCommand2?param1=value1&param2=value2'")));
    }

    @Test
    public void tryToLoginAnonymouslyWithCurlCommand () {
        this.assertUnauthorized (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost'  'https://localhost:%d/private/login'"));
    }

    @Test
    public void loginWithIncorrectLoginCurlCommand () {
        this.assertUnauthorized (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u foo:bar 'https://localhost:%d/private/login'"));
    }

    @Test
    public void loginCorrectLoginCurlCommand () {
        this.assertOk (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass -L -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u user:password 'https://localhost:%d/private/login'"));
    }

    @Test
    public void withJsonBody () {
        this.assertOk (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass -X POST 'https://localhost:%d/public/json' -d '{\"var1\":\"val1\",\"var2\":\"val2\"}'"));
    }

    private void assertUnauthorized (final HttpResponse curlResponse) {
        Assertions.assertThat (curlResponse).isNotNull ();
        Assertions.assertThat (this.statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_UNAUTHORIZED);

    }

    private void assertOk (final HttpResponse curlResponse) {
        Assertions.assertThat (curlResponse).isNotNull ();
        Assertions.assertThat (this.statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_OK);
    }

    private void assertFound (final HttpResponse curlResponse) {
        Assertions.assertThat (curlResponse).isNotNull ();
        Assertions.assertThat (this.statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_MOVED_TEMPORARILY);
    }

    private int statusCodeOf (final HttpResponse response) {
        return response.getStatusLine ().getStatusCode ();
    }
}
