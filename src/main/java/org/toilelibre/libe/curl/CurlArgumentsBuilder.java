package org.toilelibre.libe.curl;

import org.apache.http.HttpResponse;
import org.toilelibre.libe.curl.Curl.CurlException;

public class CurlArgumentsBuilder {
    
    private final StringBuilder curlCommand = new StringBuilder ("curl ");
    
    CurlArgumentsBuilder () {}
    
    public HttpResponse run (String url) throws CurlException {
        curlCommand.append (url + " ");
        return Curl.curl (curlCommand.toString ());
    }

    public String $ (String url) throws CurlException {
        curlCommand.append (url + " ");
        return Curl.$ (curlCommand.toString ());
    }

}
