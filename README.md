# curl
curl command in java (using Apache libs : HttpClient and commons-cli)

Setup
`<dependency>`
`    <groupId>org.toile-libre.libe</groupId>`
`    <artifactId>curl</artifactId>`
`    <version>`[version](https://img.shields.io/maven-central/v/org.toile-libre.libe/curl.svg?logoWidth=-91)`</version>`
`</dependency>`

Usage
```java
    org.apache.http.HttpResponse org.toilelibre.libe.curl.Curl.curl (String curlParams);
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
