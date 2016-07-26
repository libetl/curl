package org.toilelibre.libe.curl;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.toilelibre.libe.curl.monitor.RequestMonitor;

import static org.fest.assertions.Assertions.assertThat;

public class CurlTest {

    @BeforeClass
    public static void startRequestMonitor() {
        RequestMonitor.start();
    }

    @AfterClass
    public static void stopRequestMonitor() {
        RequestMonitor.stop();
    }

    private HttpResponse curl(final String requestCommand) {
        return Curl.curl(String.format(requestCommand, RequestMonitor.port()));
    }

    private String $(final String requestCommand) {
        return Curl.$(String.format(requestCommand, RequestMonitor.port()));
    }

    @Test(expected = RuntimeException.class)
    public void curlRootWithoutTrustingInsecure() {
        $("curl https://localhost:%d/public/");
    }

    @Test(expected= RuntimeException.class)
    public void curlRootWithoutClientCertificate() {
        $("curl -k https://localhost:%d/public/");
    }

    @Test
    public void curlRoot() {
        assertOk(curl("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass https://localhost:%d/public/"));
    }

    @Test
    public void curlToRedirectionWithoutFollowRedirectParam() {
        assertFound(curl("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass https://localhost:%d/public/redirection"));
    }

    @Test
    public void curlToUnauthorized() {
        assertUnauthorized(curl("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass https://localhost:%d/public/unauthorized"));
    }

    @Test
    public void curlToRedirectionWithFollowRedirectParam() {
        assertOk(curl("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass -L https://localhost:%d/public/redirection"));
    }

    @Test
    public void curlWithHeaders() {
        assertOk(curl("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass -H'Host: localhost' -H'Authorization: 45e03eb2-8954-40a3-8068-c926f0461182' https://localhost:%d/public/v1/coverage/sncf/journeys?from=admin:7444extern"));
    }

    @Test
    public void readCurlPublicRoot() {
        assertOk(curl($("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass https://localhost:%d/public/")));
    }

    @Test
    public void readCurlWithHeaders() {
        assertOk(curl($("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass -H'Host: localhost' -H'Authorization: 45e03eb2-8954-40a3-8068-c926f0461182' https://localhost:%d/public/v1/coverage/sncf/journeys?from=admin:7444extern")));
    }


    @Test
    public void curlOfReadCurlOfReadCurl() {
        assertOk(curl($($($($("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass https://localhost:%d/public/"))))));
    }

    @Test
    public void readCurlCommand() {
        assertOk(curl("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost'  'https://localhost:%d/public/curlCommand1?param1=value1&param2=value2'"));
    }

    @Test
    public void readCurlOfCurlCommand() {
        assertOk(curl($("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost'  'https://localhost:%d/public/curlCommand2?param1=value1&param2=value2'")));
    }

    @Test
    public void tryToLoginAnonymouslyWithCurlCommand() {
        assertUnauthorized(curl("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost'  'https://localhost:%d/private/login'"));
    }

    @Test
    public void loginWithIncorrectLoginCurlCommand() {
        assertUnauthorized(curl("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u foo:bar 'https://localhost:%d/private/login'"));
    }

    @Test
    public void loginCorrectLoginCurlCommand() {
        assertOk(curl("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass -L -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u user:password 'https://localhost:%d/private/login'"));
    }

    @Test
    public void withJsonBody() {
        assertOk(curl("-k --cert src/test/resources/clients/libe/libe.p12:mylibepass -X POST 'https://localhost:%d/public/json' -d '{\"var1\":\"val1\",\"var2\":\"val2\"}'"));
    }

    private void assertUnauthorized(final HttpResponse curlResponse) {
        assertThat(curlResponse).isNotNull();
        assertThat(statusCodeOf(curlResponse)).isEqualTo(HttpStatus.SC_UNAUTHORIZED);

    }

    private void assertOk(final HttpResponse curlResponse) {
        assertThat(curlResponse).isNotNull();
        assertThat(statusCodeOf(curlResponse)).isEqualTo(HttpStatus.SC_OK);
    }

    private void assertFound(final HttpResponse curlResponse) {
        assertThat(curlResponse).isNotNull();
        assertThat(statusCodeOf(curlResponse)).isEqualTo(HttpStatus.SC_MOVED_TEMPORARILY);
    }

    private int statusCodeOf(final HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }
}
