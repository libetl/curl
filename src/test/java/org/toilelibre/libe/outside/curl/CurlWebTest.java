package org.toilelibre.libe.outside.curl;

import org.junit.Test;

import static org.toilelibre.libe.curl.Curl.curl;

public class CurlWebTest {
    @Test
    public void wrongHost () {
        curl("-k https://wrong.host.badssl.com/");
    }
}
