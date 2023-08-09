# curl [![License: Unlicense](https://img.shields.io/badge/license-Unlicense-blue.svg)](http://unlicense.org/)
curl command in java (using Apache libs : HttpClient 5 and commons-cli)

Setup with maven

`<dependency>`

&nbsp;&nbsp;&nbsp;&nbsp;`<groupId>org.toile-libre.libe</groupId>`

&nbsp;&nbsp;&nbsp;&nbsp;`<artifactId>curl</artifactId>`

&nbsp;&nbsp;&nbsp;&nbsp;`<version>`![LATEST](https://img.shields.io/maven-central/v/org.toile-libre.libe/curl?label=%20&style=for-the-badge)`</version>`

`</dependency>`

Usage
```java
    org.apache.hc.core5.http.ClassicHttpResponse org.toilelibre.libe.curl.Curl.curl (String curlParams);
    String org.toilelibre.libe.curl.Curl.$ (String curlCommand); //Returns responseBody
```

You can import static these methods :
```java
    import static org.toilelibre.libe.curl.Curl.curl;
    import static org.toilelibre.libe.curl.Curl.$;
```

Examples :
```java
    $("curl https://localhost:8443/public/");
    $("curl -k https://localhost:8443/public/");
    curl("-k https://localhost:8443/public/");
    curl("-k --cert src/test/resources/client.p12:password https://localhost:8443/public/");
    curl("-k https://localhost:8443/public/redirection");
    curl("-k https://localhost:8443/public/unauthorized");
    curl("-k -L https://localhost:8443/public/redirection");
    curl("-k -H'Host: localhost' -H'Authorization: 00000000-0000-0000-0000-000000000000' https://localhost:8443/public/v1/coverage/sncf/journeys?from=admin:7444extern");
    curl("-k -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost'  'https://localhost:8443/public/curlCommand1?param1=value1&param2=value2'");
    curl("-k -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u foo:bar 'https://localhost:8443/private/login'");
    curl("-L -k -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u user:password 'https://localhost:8443/private/login'");
    curl("-k -X POST 'https://localhost:8443/public/json' -d '{\"var1\":\"val1\",\"var2\":\"val2\"}'");
```

It also works with a builder

```java
    HttpResponse response = curl().k().xUpperCase("POST").d("{\"var1\":\"val1\",\"var2\":\"val2\"}").run("https://localhost:8443/public/json");
```

How to get Google Homepage with this lib :
```java
    public String getGoogleHomepage (){
        //-L is passed to follow the redirects
        return curl ().lUpperCase ().$ ("https://www.google.com/");
    }
```

You can also specify five additional curl options using jvm code :
* javaOptions.interceptor can be used to surround the call with a custom
  handling
* javaOptions.placeHolders allows to define substitution variables
  (useful mostly for long payloads to avoid StackOverflowErrors)
* javaOptions.connectionManager allows to specify your own connection
  manager for pooling purposes or optimization purposes
  (warning, this will break the trust insecure behavior)
* javaOptions.httpClientCustomizer lets you manipulate the HttpClientBuilder
* javaOptions.contextTester allows to inspect the request resolved information (it is a Consumer of HttpContext)

```java
curl()
   .javaOptions(with().interceptor(((request, responseSupplier) -> {
       LOGGER.info("I log something before the call");
       HttpResponse response = responseSupplier.get();
       LOGGER.info("I log something after the call, status code is {}",
       response.getStatusLine().getStatusCode());
       return response;}))
                      .connectionManager(new PoolingHttpClientConnectionManager ())
                      .placeHolders(asList("fr-FR", "text/html")).build())
   .hUpperCase("'Accept-Language: $curl_placeholder_0'")
   .hUpperCase("'Accept: $curl_placeholder_1'")
   .run("http://www.google.com");
```

Supported arguments (so far) :

| Short Name    | Long Name       | Argument Required | Description                                                                                                                                                 |
| ------------- | --------------- | ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| u             | username        | true              | user:password                                                                                                                                               |
| cacert        | cacert          | true              | CA_CERT                                                                                                                                                     |
| E             | cert            | true              | CERT[:password]                                                                                                                                             |
| ct            | cert-type       | true              | PEM,P12,JKS,DER,ENG                                                                                                                                         |
| compressed    | compressed      | false             | Request compressed response                                                                                                                                 |
| cti           | connect-timeout | true              | Maximum time allowed for connection                                                                                                                         |
| d             | data            | true              | Data                                                                                                                                                        |
| databinary    | data-binary     | true              | http post binary data                                                                                                                                       |
| dataurlencode | data-urlencode  | true              | Data to URLEncode                                                                                                                                           |
| L             | location        | false             | follow redirects                                                                                                                                            |
| F             | form            | true              | http multipart post data                                                                                                                                    |
| H             | header          | true              | Header                                                                                                                                                      |
| X             | request         | true              | Http Method                                                                                                                                                 |
| key           | key             | true              | KEY                                                                                                                                                         |
| kt            | key-type        | true              | PEM,P12,JKS,DER,ENG                                                                                                                                         |
| m             | max-time        | true              | Maximum time allowed for the transfer                                                                                                                       |
| nokeepalive   | no-keepalive    | false             | Disable TCP keepalive on the connection                                                                                                                     |
| ntlm          | ntlm            | false             | NTLM auth                                                                                                                                                   |
| o             | output          | true              | write to file                                                                                                                                               |
| x             | proxy           | true              | use the specified HTTP proxy                                                                                                                                |
| U             | proxy-user      | true              | authentication for proxy                                                                                                                                    |
| 1             | tlsv1           | false             | use >= TLSv1 (SSL)                                                                                                                                          |
| tlsv10        | tlsv1.0         | false             | use TLSv1.0 (SSL)                                                                                                                                           |
| tlsv11        | tlsv1.1         | false             | use TLSv1.1 (SSL)                                                                                                                                           |
| tlsv12        | tlsv1.2         | false             | use TLSv1.2 (SSL)                                                                                                                                           |
| 2             | sslv2           | false             | use SSLv2 (SSL)                                                                                                                                             |
| 3             | sslv3           | false             | use SSLv3 (SSL)                                                                                                                                             |
| k             | insecure        | false             | trust insecure                                                                                                                                              |
| A             | user-agent      | true              | user agent                                                                                                                                                  |
| V             | version         | false             | get the version of this library                                                                                                                             |
| interceptor   | interceptor     | true              | interceptor field or method (syntax is classname::fieldname). Must be a BiFunction<HttpRequest, Supplier< HttpResponse>, HttpResponse> or will be discarded |
