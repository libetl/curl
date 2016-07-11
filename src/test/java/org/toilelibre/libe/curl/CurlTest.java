package org.toilelibre.libe.curl;

import org.junit.Test;

import static org.toilelibre.libe.curl.Curl.curl;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Response;

public class CurlTest {
    
    @Test
    public void curlGoogle () {
        assertOk(curl ("http://www.google.fr"));
    }
    
    @Test
    public void curlSncf () {
        assertUnauthorized(curl ("curl -H'Authorization: aaaa' https://api.sncf.com/v1/coverage/sncf/journeys?from=admin:7444extern&to=admin:120965extern&datetime=20160102T215522"));
    }
    
    private void assertUnauthorized (Response curlResponse) {
        assertThat (curlResponse).isNotNull ();
        assertThat (statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_UNAUTHORIZED);

    }

    private void assertOk (Response curlResponse) {
        assertThat (curlResponse).isNotNull ();
        assertThat (statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_OK);
        
    }

    private int statusCodeOf (Response response) {
        try {
            return response.returnResponse ().getStatusLine ().getStatusCode ();
        } catch (IOException e) {
            return 0;
        }
    }
}
