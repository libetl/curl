package org.toilelibre.libe.curl;

import org.apache.http.HttpResponse;
import org.toilelibre.libe.curl.Curl.CurlException;

public class ArgumentsBuilder {
    
    private final StringBuilder curlCommand = new StringBuilder ("curl ");
    
    ArgumentsBuilder () {}
    
    public HttpResponse run () throws CurlException {
        return Curl.curl (curlCommand.toString ());
    }

    public String $ () throws CurlException {
        return Curl.$ (curlCommand.toString ());
    }

}
