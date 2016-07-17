package org.toilelibre.libe.curl;

import static org.fest.assertions.Assertions.assertThat;
import static org.toilelibre.libe.curl.Curl.$;
import static org.toilelibre.libe.curl.Curl.$t;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.log4j.MDC;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.toilelibre.libe.curl.monitor.RequestMonitor;

public class CurlTest {
    
    @BeforeClass
    public static void startRequestMonitor () {
        RequestMonitor.start ();
    }
    
    @AfterClass
    public static void stopRequestMonitor () {
        RequestMonitor.stop ();
    }
    
    @Rule
    public MethodRule setThreadNameMethodRule = new MethodRule () {

        @Override
        public Statement apply (Statement base, FrameworkMethod method, Object target) {
            return new Statement () {

                @Override
                public void evaluate () throws Throwable {
                    MDC.put ("testMethod", method.getType ().getSimpleName () + "." + method.getName ());
                    base.evaluate ();
                }
                
            };
        }
    };
    
    @Test(expected= RuntimeException.class)
    public void curlRootWithoutTrustingInsecure () {
        $("curl https://localhost:" + RequestMonitor.port() + "/public/");
    }
    
    @Test
    public void curlRoot () {
        assertOk($("curl -k https://localhost:" + RequestMonitor.port() + "/public/"));
    }
    
    @Test
    public void curlToRedirectionWithoutFollowRedirectParam () {
        assertFound($("curl -k https://localhost:" + RequestMonitor.port() + "/public/redirection"));
    }
    
    @Test
    public void curlToUnauthorized () {
        assertUnauthorized($("curl -k https://localhost:" + RequestMonitor.port() + "/public/unauthorized"));
    }
    
    @Test
    public void curlToRedirectionWithFollowRedirectParam () {
        assertOk($("curl -k -L https://localhost:" + RequestMonitor.port() + "/public/redirection"));
    }
    
    @Test
    public void curlWithHeaders () {
        assertOk($("curl -k -H'Host: localhost' -H'Authorization: 45e03eb2-8954-40a3-8068-c926f0461182' https://localhost:" + RequestMonitor.port() + "/public/v1/coverage/sncf/journeys?from=admin:7444extern"));
    }
    
    @Test
    public void readCurlPublicRoot () {
        assertOk($($t ("curl -k https://localhost:" + RequestMonitor.port() + "/public/")));
    }
    
    @Test
    public void readCurlWithHeaders () {
        assertOk($($t("curl -k -H'Host: localhost' -H'Authorization: 45e03eb2-8954-40a3-8068-c926f0461182' https://localhost:" + RequestMonitor.port() + "/public/v1/coverage/sncf/journeys?from=admin:7444extern")));
    }

    
    @Test
    public void curlOfReadCurlOfReadCurl () {
        assertOk($($t($t($t($t("curl -k https://localhost:" + RequestMonitor.port() + "/public/"))))));
    }
    
    @Test
    public void readCurlCommand () {
        assertOk($("curl -k -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost'  'https://localhost:" + RequestMonitor.port() + "/public/curlCommand1?param1=value1&param2=value2'"));
    }
    
    @Test
    public void readCurlOfCurlCommand () {
        assertOk($($t("curl -k -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost'  'https://localhost:" + RequestMonitor.port() + "/public/curlCommand2?param1=value1&param2=value2'")));
    }
    
    @Test
    public void tryToLoginAnonymouslyWithCurlCommand () {
        assertUnauthorized($("curl -k -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost'  'https://localhost:" + RequestMonitor.port() + "/private/login'"));
    }
    
    @Test
    public void loginWithIncorrectLoginCurlCommand () {
        assertUnauthorized($("curl -k -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u foo:bar 'https://localhost:" + RequestMonitor.port() + "/private/login'"));
    }
    
    @Test
    public void loginCorrectLoginCurlCommand () {
        assertOk($("curl -L -k -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u user:password 'https://localhost:" + RequestMonitor.port() + "/private/login'"));
    }
    
    @Test
    public void withJsonBody () {
        assertOk($("curl -k -X POST 'https://localhost:" + RequestMonitor.port() + "/public/json' -d '{\"var1\":\"val1\",\"var2\":\"val2\"}'"));
    }
    
    private void assertUnauthorized (HttpResponse curlResponse) {
        assertThat (curlResponse).isNotNull ();
        assertThat (statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_UNAUTHORIZED);

    }

    private void assertOk (HttpResponse curlResponse) {
        assertThat (curlResponse).isNotNull ();
        assertThat (statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_OK); 
    }
    
    private void assertFound (HttpResponse curlResponse) {
        assertThat (curlResponse).isNotNull ();
        assertThat (statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_MOVED_TEMPORARILY); 
    }

    private int statusCodeOf (HttpResponse response) {
        return response.getStatusLine ().getStatusCode ();
    }
}
