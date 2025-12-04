package org.toilelibre.libe.outside.curl;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.toilelibre.libe.curl.Curl;
import org.toilelibre.libe.curl.Curl.CurlException;
import org.toilelibre.libe.outside.monitor.RequestMonitor;
import org.toilelibre.libe.outside.monitor.StupidHttpServer;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.*;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.toilelibre.libe.curl.Curl.CurlArgumentsBuilder.CurlJavaOptions.with;

public class CurlTest {

    private static final Integer proxyPort = Math.abs (new Random ().nextInt ()) % 20000 + 1024;
    private static final Logger LOGGER = Logger.getLogger (CurlTest.class.getName ());
    private static ClientAndServer proxy;

    @BeforeAll
    public static void startRequestMonitor () {
        if (System.getProperty ("skipServer") == null) {
            RequestMonitor.start ();
            StupidHttpServer.start ();
            proxy = ClientAndServer.startClientAndServer (proxyPort);
        }
    }

    @AfterAll
    public static void stopRequestMonitor () {
        if (System.getProperty ("skipServer") == null) {
            RequestMonitor.stop ();
            StupidHttpServer.stop ();
            proxy.stop ();
        }
    }

    private String $ (final String requestCommand) {
        return Curl.$ (String.format (requestCommand, RequestMonitor.port ()));
    }

    private CompletableFuture<String> $Async (final String requestCommand) {
        return Curl.$Async (String.format (requestCommand, RequestMonitor.port ()));
    }

    private ClassicHttpResponse curl (final String requestCommand) {
        return curl (requestCommand, with ().build ());
    }

    private ClassicHttpResponse curl (final String requestCommand, Curl.CurlArgumentsBuilder.CurlJavaOptions curlJavaOptions) {
        return Curl.curl (String.format (requestCommand, RequestMonitor.port ()), curlJavaOptions);
    }

    private CompletableFuture<ClassicHttpResponse> curlAsync (final String requestCommand) {
        return Curl.curlAsync (String.format (requestCommand, RequestMonitor.port ()));
    }

    private void assertFound (final ClassicHttpResponse curlResponse) {
        Assertions.assertThat (curlResponse).isNotNull ();
        Assertions.assertThat (this.statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_MOVED_TEMPORARILY);
    }

    private void assertOk (final ClassicHttpResponse curlResponse) {
        Assertions.assertThat (curlResponse).isNotNull ();
        Assertions.assertThat (this.statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_OK);
    }

    private void assertUnauthorized (final ClassicHttpResponse curlResponse) {
        Assertions.assertThat (curlResponse).isNotNull ();
        Assertions.assertThat (this.statusCodeOf (curlResponse)).isEqualTo (HttpStatus.SC_UNAUTHORIZED);
    }

    private int statusCodeOf (final ClassicHttpResponse response) {
        return response.getCode();
    }

    @Test
    public void displayVersion () {
        assertThrows(CurlException.class, () -> this.assertOk (this.curl ("-V")));
    }

    @Test
    public void curlRoot () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/"));
    }

    @Test
    public void curlCompressed () {
        this.assertOk (this.curl ("-k --compressed -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/"));
    }

    @Test
    public void curlNoKeepAlive () {
        this.assertOk (this.curl ("-k --no-keepalive -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/"));
    }

    @Test
    public void curlTlsV12 () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/ --tlsv1.2"));
    }

    @Test
    public void curlBadHeaderFormatIgnored () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -H 'toto' https://localhost:%d/public/"));
    }

    @Test
    public void theSkyIsBlueInIvritWithTheWrongEncoding () throws IOException {
        ClassicHttpResponse response = this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/  -H 'Content-Type: text/plain; charset=ISO-8859-1' -d \"השמים כחולים\"");
        Assertions.assertThat (IOUtils.toString (response.getEntity ().getContent (), StandardCharsets.UTF_8)).contains ("'????? ??????'");
    }

    @Test
    public void theSkyIsBlueInIvritWithoutEncoding () throws IOException {
        ClassicHttpResponse response = this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/  -d \"השמים כחולים\"");
        Assertions.assertThat (IOUtils.toString (response.getEntity ().getContent (), StandardCharsets.UTF_8)).contains ("'השמים כחולים'");
    }

    @Test
    public void theSkyIsBlueInIvritWithUTF8Encoding () throws IOException {
        ClassicHttpResponse response = this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/  -H 'Content-Type: text/plain; charset=UTF-8'  -d \"השמים כחולים\"");
        Assertions.assertThat (IOUtils.toString (response.getEntity ().getContent (), StandardCharsets.UTF_8)).contains ("'השמים כחולים'");
    }

    @Test
    public void curlDER () {
        this.assertOk (this.curl ("-k --cert-type DER --cert src/test/resources/clients/libe/libe.der:mylibepass --key src/test/resources/clients/libe/libe.key.der --key-type DER https://localhost:%d/public/"));
    }

    @Test
    public void curlHalfPemHalfPKCS12 () {
        this.assertOk (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass --key-type PEM --key src/test/resources/clients/libe/libe.pem https://localhost:%d/public/"));
    }

    @Test
    public void curlWithPlaceholders () {
        this.assertOk (this.curl ("-k --cert-type $curl_placeholder_0 --cert $curl_placeholder_1 --key-type $curl_placeholder_2 --key $curl_placeholder_3 https://localhost:%d/public/",
                with ().placeHolders (asList ("P12", "src/test/resources/clients/libe/libe.p12:mylibepass", "PEM", "src/test/resources/clients/libe/libe.pem")).build ()));
    }
    @Test
    public void curlWithConnectionManager () throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException, IOException, CertificateException {
        KeyStore keystore = KeyStore.getInstance ("JKS");
        keystore.load (Thread.currentThread ().getContextClassLoader ().getResourceAsStream ("clients/libe/libe.jks"), "mylibepass".toCharArray ());
        this.assertOk (this.curl ("https://localhost:%d/public/",
                with ().connectionManager (new PoolingHttpClientConnectionManager (RegistryBuilder.<ConnectionSocketFactory>create ()
                        .register ("https", new SSLConnectionSocketFactory (SSLContextBuilder.create ()
                                .loadTrustMaterial (null, new TrustSelfSignedStrategy())
                                .loadKeyMaterial (keystore, "mylibepass".toCharArray ())
                                .build (), NoopHostnameVerifier.INSTANCE))
                        .build ())).build ()));
    }

    @Test
    public void curlJKS () {
        this.assertOk (this.curl ("-k --cert-type JKS --cert src/test/resources/clients/libe/libe.jks:mylibepass https://localhost:%d/public/"));
    }

    @Test
    public void curlOfReadCurlOfReadCurl () {
        this.assertOk (this.curl (this.$ (this.$ (this.$ (this.$ ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/"))))));
    }

    @Test
    public void curlPKCS12 () {
        this.assertOk (this.curl ("-k --cert-type P12 --cert src/test/resources/clients/libe/libe.p12:mylibepass https://localhost:%d/public/"));
    }

    @Test
    public void curlToRedirectionWithFollowRedirectParam () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -L https://localhost:%d/public/redirection"));
    }

    @Test
    public void curlToRedirectionWithoutFollowRedirectParam () {
        this.assertFound (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/redirection"));
    }

    @Test
    public void curlToUnauthorized () {
        this.assertUnauthorized (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/unauthorized"));
    }

    @Test
    public void curlWithCaCert () {
        this.assertOk (this.curl ("-k --cacert src/test/resources/ca/fakeCa.crt --cert-type PEM --cert src/test/resources/clients/libe/libe.pem:mylibepass https://localhost:%d/public/"));
    }

    @Test
    public void curlWithFullSslChain () {
        this.assertOk (this.curl ("-k --cacert src/test/resources/ca/fakeCa.crt --cert-type PEM --cert src/test/resources/clients/libe/libe.pem:mylibepass --key-type P12 --key src/test/resources/clients/libe/libe.p12:mylibepass https://localhost:%d/public/"));

        try {
            // correct cert password and wrong key password
            this.curl ("-k --cacert src/test/resources/ca/fakeCa.crt --cert-type PEM --cert src/test/resources/clients/libe/libe.pem:mylibepass --key-type P12 --key src/test/resources/clients/libe/libe.p12:mylibepass2 https://localhost:%d/public/");
            Assertions.fail ("This curl is not supposed to work and should fail with a IOException");
        }catch (CurlException curlException){
            Assertions.assertThat (curlException.getCause ().getClass ().getName ())
                    .isEqualTo (IOException.class.getName ());
            Assertions.assertThat (curlException.getCause ().getMessage ())
                    .isEqualTo (
                    "keystore password was incorrect");
        }
    }

    @Test
    public void curlWithHeaders () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -H'Host: localhost' -H'Authorization: 45e03eb2-8954-40a3-8068-c926f0461182' https://localhost:%d/public/v1/coverage/sncf/journeys?from=admin:7444extern"));
    }

    @Test
    public void curlWithHeadersContainingColon () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -H'Host: localhost' -H'SOAPAction: action1:action2:action3' https://localhost:%d/public/test"));
    }

    @Test
    public void curlWithOnlyALogin () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -u user https://localhost:%d/public/"));
    }

    @Test
    public void loginCorrectLoginCurlCommand () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -L -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u user:password 'https://localhost:%d/private/login'"));
    }

    @Test
    public void loginWithIncorrectLoginCurlCommand () {
        this.assertUnauthorized (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' -u foo:bar 'https://localhost:%d/private/login'"));
    }

    @Test
    public void readCurlCommand () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public/curlCommand1?param1=value1&param2=value2'"));
    }

    @Test
    public void readCurlOfCurlCommand () {
        this.assertOk (this.curl (this.$ ("-k -E src/test/resources/clients/libe/libe.pem -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public/curlCommand2?param1=value1&param2=value2'")));
    }

    @Test
    public void readCurlPublicRoot () {
        this.assertOk (this.curl (this.$ ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/")));
    }

    @Disabled
    @Test
    public void curlWithTooLowRequestTimeout () {
        try {
            this.curl (this.$("-k -E src/test/resources/clients/libe/libe.pem --connect-timeout 0.001 --max-time 10 https://localhost:%d/public/tooLong"));
            Assertions.fail ("This curl is not supposed to work and should fail with a ConnectTimeoutException");
        }catch (CurlException curlException){
            Assertions.assertThat (
                    asList (ConnectTimeoutException.class.getName (), ClientProtocolException.class.getName ())
                            .contains (curlException.getCause ().getClass ().getName ())).isTrue ();
        }
    }

    @Test
    public void curlWithMaxTime () {
        try {
            this.curl (this.$("-k -E src/test/resources/clients/libe/libe.pem --connect-timeout 10 --max-time 0.001 https://localhost:%d/public/tooLong"));
            Assertions.fail ("This curl is not supposed to work and should fail with a SocketTimeoutException");
        }catch (CurlException curlException){
            Assertions.assertThat (curlException.getCause ().getClass ().getName ())
                    .isEqualTo (SocketTimeoutException.class.getName ());
        }
    }

    @Test
    public void readCurlWithHeaders () {
        this.assertOk (this.curl (this.$ ("-k -E src/test/resources/clients/libe/libe.pem -H'Host: localhost' -H'Authorization: 45e03eb2-8954-40a3-8068-c926f0461182' https://localhost:%d/public/v1/coverage/sncf/journeys?from=admin:7444extern")));
    }

    @Test
    public void tryToLoginAnonymouslyWithCurlCommand () {
        this.assertUnauthorized (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X GET -H 'User-Agent: curl/7.49.1' -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/private/login'"));
    }

    @Test
    public void withForm () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -F 'toto=titi;foo=bar' -F 'tutu=tata' -X POST -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public/form'"));
    }

    @Test
    public void withJsonBody () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X POST 'https://localhost:%d/public/json' -d '{\"var1\":\"val1\",\"var2\":\"val2\"}'"));
    }

    @Test
    public void withSimpleArgsParsing () {
        this.assertOk (curl ("-k -E src/test/resources/clients/libe/libe.pem -X POST 'https://localhost:%d/public/json' -d '{\"var1\":\"val1\",\"var2\":\"val2\"}'",
                with ().simpleArgsParsing ().build ()));
    }

    @Test
    public void withUrlEncodedData () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X POST 'https://localhost:%d/public/data' --data-urlencode 'message=hello world' --data-urlencode 'othermessage=how are you'"));
    }

    @Test
    public void withUrlEncodedData2 () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X POST 'https://localhost:%d/public/data' --data-urlencode '=hello world'"));
    }

    @Test
    public void withUrlEncodedData3 () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X POST 'https://localhost:%d/public/data' --data-urlencode 'message@src/test/resources/test.sh'"));
    }

    @Test
    public void withBinaryData () throws IOException {
        ClassicHttpResponse response = this.curl ("-k -E src/test/resources/clients/libe/libe.pem --data-binary \"@src/test/resources/clients/libe/libe.der\" -X POST -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public/data'");
        String expected = IOUtils.toString (Objects.requireNonNull (Thread.currentThread ().getContextClassLoader ().getResourceAsStream ("clients/libe/libe.der")), StandardCharsets.UTF_8);
        String fullCurl = IOUtils.toString (response.getEntity ().getContent (), StandardCharsets.UTF_8);
        String actual = fullCurl.substring (fullCurl.indexOf ("-d '") + 4, fullCurl.indexOf ("'  'https"));
        Assertions.assertThat (actual.length ()).isEqualTo (expected.length ());
    }

    @Test
    public void withFileForm () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -F 'toto=titi' -F 'script=@src/test/resources/test.sh' -X POST -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public/form'"));
    }

    @Test
    public void withUserAgent () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X GET -A 'toto' -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public'"));
    }

    @Test
    public void outputFile () {
        File file = new File ("target/classes/downloadedCurl");

        boolean fileDeleted = file.delete ();
        LOGGER.log (Level.FINE, "output file deleted : " + fileDeleted);
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X GET -A 'toto' -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public' -o target/classes/downloadedCurl"));
        Assertions.assertThat (new File ("target/classes/downloadedCurl").exists ()).isTrue ();
    }

    @Test
    public void outputFileWithSpaces () {
        File file = new File ("target/classes/downloaded Curl With Spaces");

        boolean fileDeleted = file.delete ();
        LOGGER.log (Level.FINE, "output file deleted : " + fileDeleted);
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -X GET -A 'toto' -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public' -o 'target/classes/downloaded Curl With Spaces'"));
        Assertions.assertThat (new File ("target/classes/downloaded Curl With Spaces").exists ()).isTrue ();
    }

    @Test
    public void justTheVersion () {
        assertThrows (CurlException.class, () -> this.assertOk (this.curl ("-V")));
    }

    @Test
    public void curlCertNotFound () {
        assertThrows (CurlException.class, () -> this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/toto.pem https://localhost:%d/public/")));
    }

    @Test
    public void readHelp () {
        assertThrows (CurlException.class, () -> this.curl ("--help"));
    }

    @Test
    public void withBadForm () {
        assertThrows (CurlException.class, () ->
                this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem -F 'totoghghgh' -X POST -H 'Accept: */*' -H 'Host: localhost' 'https://localhost:%d/public/form'")));
    }

    @Test
    public void curlRootWithoutClientCertificate () {
        assertThrows (CurlException.class, () -> this.$ ("curl -k https://localhost:%d/public/"));
    }

    @Test
    public void curlRootWithoutTrustingInsecure () {
        assertThrows (CurlException.class, () -> this.$ ("curl https://localhost:%d/public/"));
    }

    @Test
    @Disabled // tls v1.1 is now disabled in all recent versions of the jdk, so this test will always fail
    public void curlTlsV11 () {
        this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/ --tlsv1.1"));
    }

    @Test
    public void curlTlsV10 () {
        assertThrows (CurlException.class, () ->
                this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/ --tlsv1.0")));
    }

    @Test
    public void curlTlsV1 () {
        assertThrows (CurlException.class, () ->
                this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/ -1")));
    }

    @Test
    public void curlSslV2 () {
        assertThrows (CurlException.class, () ->
                this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/ -2")));
    }

    @Test
    public void curlSslV3 () {
        assertThrows (CurlException.class, () ->
                this.assertOk (this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/ -3")));
    }

    @Test
    public void curlWithProxy () {
        this.assertOk (Curl.curl ("-x http://localhost:" + proxyPort + " http://localhost:" + StupidHttpServer.port () + "/public/foo"));
    }

    @Test
    public void curlAsync () throws InterruptedException, ExecutionException {
        this.$Async (this.$Async ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/pathAsync").get ());
    }

    @Test
    public void twoCurlsInParallel () {
        final CompletableFuture <ClassicHttpResponse> future1 = this.curlAsync ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/path1");
        final CompletableFuture <ClassicHttpResponse> future2 = this.curlAsync ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/path2");

        try {
            CompletableFuture.allOf (future1, future2).get ();
            this.assertOk (future1.get ());
            this.assertOk (future2.get ());
        } catch (InterruptedException | ExecutionException e) {
            Assertions.fail ();
        }
    }

    @Test
    public void noContentShouldNotTriggerANullPointerException () {
        this.$ ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/noContent");
    }

    @SuppressWarnings ("unused")
    public static class MyInterceptor {
        public ClassicHttpResponse intercept (HttpRequest request, Supplier <ClassicHttpResponse> responseSupplier){
            LOGGER.info ("I log something before the call");
            ClassicHttpResponse response = responseSupplier.get ();
            LOGGER.info ("I log something after the call... Bingo, the status of the response is " +
                    response.getCode ());
            return response;
        }
    }

    @SuppressWarnings ("unused")
    private final BiFunction<HttpRequest, Supplier <ClassicHttpResponse>, ClassicHttpResponse> mySecondInterceptor =
            (request, responseSupplier) -> {
        LOGGER.info ("I log something before the call (from a lambda)");
        ClassicHttpResponse response = responseSupplier.get ();
        LOGGER.info ("I log something after the call (from a lambda)... Bingo, the status of the response is " +
                response.getCode ());
        return response;
    };

    @Test
    public void withAnInterceptor (){
        this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/  --interceptor org.toilelibre.libe.outside.curl.CurlTest$MyInterceptor::intercept  --interceptor org.toilelibre.libe.outside.curl.CurlTest::mySecondInterceptor");
    }

    @Test
    public void withAnInlinedInterceptor (){
        Curl.curl ()
                .javaOptions (with ().interceptor (((request, responseSupplier) -> {
                    LOGGER.info ("I log something before the call");
                    ClassicHttpResponse response = responseSupplier.get ();
                    LOGGER.info ("I log something after the call... Bingo, the status of the response is " +
                            response.getCode ());
                    return response;
                })).build ())
        .run ("http://www.google.com");
    }

    @Test
    public void nonExistingInterceptor1(){
        this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/  --interceptor org.toilelibre.libe.outside.curl.CurlTest$ThatSoCalledInterceptor::intercept");
    }

    @Test
    public void nonExistingInterceptor2(){
        this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/  --interceptor org.toilelibre.libe.outside.curl.CurlTest.A_SO_CALLED_FIELDNAME");
    }

    @Test
    public void nonInterceptorField (){
        this.curl ("-k -E src/test/resources/clients/libe/libe.pem https://localhost:%d/public/  --interceptor org.toilelibre.libe.outside.curl.CurlTest.LOGGER");
    }
}
