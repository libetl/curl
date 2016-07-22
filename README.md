# curl
curl command in java (using Apache libs : HttpClient and commons-cli)

Examples :
```java
    $("curl https://localhost:%d/public/");
    $("curl -k https://localhost:%d/public/");
    curl("-k https://localhost:%d/public/");
    curl("-k --cert src/test/resources/client.p12:password https://localhost:%d/public/");
    curl("-k https://localhost:%d/public/redirection");
    curl("-k https://localhost:%d/public/unauthorized");
    curl("-k -L https://localhost:%d/public/redirection");
    curl("-k -H'Host: localhost' -H'Authorization: 00000000-0000-0000-0000-000000000000' https://localhost:%d/public/v1/coverage/sncf/journeys?from=admin:7444extern");
    curl("-k -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost'  'https://localhost:%d/public/curlCommand1?param1=value1&param2=value2'");
    curl("-k -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u foo:bar 'https://localhost:%d/private/login'");
    curl("-L -k -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u user:password 'https://localhost:%d/private/login'");
    curl("-k -X POST 'https://localhost:%d/public/json' -d '{\"var1\":\"val1\",\"var2\":\"val2\"}'");
```
