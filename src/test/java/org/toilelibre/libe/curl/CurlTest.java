package org.toilelibre.libe.curl;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.toilelibre.libe.curl.monitor.RequestMonitor;

import static org.toilelibre.libe.curl.Curl.$;
import static org.toilelibre.libe.curl.Curl.$t;

import static org.fest.assertions.Assertions.assertThat;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.log4j.MDC;

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
    
    @Test
    public void curlRoot () {
        assertOk($("curl http://localhost:" + RequestMonitor.port() + "/"));
    }
    
    @Test
    public void curlToRedirectionWithoutFollowRedirectParam () {
        assertFound($("curl http://localhost:" + RequestMonitor.port() + "/redirection"));
    }
    
    @Test
    public void curlToUnauthorized () {
        assertUnauthorized($("curl http://localhost:" + RequestMonitor.port() + "/unauthorized"));
    }
    
    @Test
    public void curlToRedirectionWithFollowRedirectParam () {
        assertOk($("curl -L http://localhost:" + RequestMonitor.port() + "/redirection"));
    }
    
    @Test
    public void curlWithHeaders () {
        assertOk($("curl -H'Host: localhost' -H'Authorization: 45e03eb2-8954-40a3-8068-c926f0461182' http://localhost:" + RequestMonitor.port() + "/v1/coverage/sncf/journeys?from=admin:7444extern"));
    }
    
    @Test
    public void readCurlRoot () {
        assertOk($($t ("curl http://localhost:" + RequestMonitor.port() + "/")));
    }
    
    @Test
    public void readCurlWithHeaders () {
        assertOk($($t("curl -H'Host: localhost' -H'Authorization: 45e03eb2-8954-40a3-8068-c926f0461182' http://localhost:" + RequestMonitor.port() + "/v1/coverage/sncf/journeys?from=admin:7444extern")));
    }

    
    @Test
    public void curlOfReadCurlOfReadCurl () {
        assertOk($($t($t($t($t("curl http://localhost:" + RequestMonitor.port() + "/"))))));
    }
    
    @Test
    public void readCurlCommand () {
        assertOk($("curl -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost'  'http://localhost:" + RequestMonitor.port() + "/curlCommand1?param1=value1&param2=value2'"));
    }
    @Test
    public void readCurlOfCurlCommand () {
        assertOk($($t("curl -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost'  'http://localhost:" + RequestMonitor.port() + "/curlCommand2?param1=value1&param2=value2'")));
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
