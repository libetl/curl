package org.toilelibre.libe.outside.monitor;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.JobExecutionExitCodeGenerator;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@EnableWebMvc
@EnableWebSecurity
public class RequestMonitor {
    @Controller
    @RequestMapping ("/**")
    static class MonitorController {

        @RequestMapping (value = "/public/noContent", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE, method = RequestMethod.GET)
        @ResponseStatus (code = HttpStatus.NO_CONTENT)
        @ResponseBody
        public String emptyResponse () {
            return null;
        }

        @RequestMapping (value = "/public/data", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE, method = RequestMethod.POST)
        @ResponseStatus (code = HttpStatus.OK)
        @ResponseBody
        public byte[] data (final HttpServletRequest request) throws IOException {
            return this.logRequest (request, IOUtils.toString(request.getInputStream())).getBytes();
        }

        @RequestMapping (value = "/public/form", produces = MediaType.TEXT_PLAIN_VALUE, method = RequestMethod.POST)
        @ResponseStatus (code = HttpStatus.OK)
        @ResponseBody
        public String form (final HttpServletRequest request) throws ServletException, IOException {
            final Collection<Part> parts = request.getParts ();
            RequestMonitor.LOGGER.info (parts.toString ());
            return this.logRequest (request, "");
        }

        @RequestMapping (value = "/public/json", produces = MediaType.TEXT_PLAIN_VALUE)
        @ResponseStatus (code = HttpStatus.OK)
        @ResponseBody
        public String json (final HttpServletRequest request, @RequestBody (required = true) final String body) throws JsonParseException, JsonMappingException, IOException {
            @SuppressWarnings ("unchecked")
            final Map<String, Object> map = new ObjectMapper ().readValue (body, Map.class);
            RequestMonitor.LOGGER.info (map.toString ());
            return this.logRequest (request, body);
        }

        @RequestMapping (value = "/public/tooLong", produces = MediaType.TEXT_PLAIN_VALUE)
        @ResponseStatus (code = HttpStatus.OK)
        @ResponseBody
        public String tooLong () throws InterruptedException {
            Thread.sleep(1000);
            RequestMonitor.LOGGER.info ("Finally !");
            return "...Finally.";
        }

        @RequestMapping (value = "/private/login", produces = MediaType.TEXT_PLAIN_VALUE)
        @ResponseStatus (code = HttpStatus.FOUND)
        @ResponseBody
        public String login (final HttpServletRequest request, final HttpServletResponse response, @RequestBody (required = false) final String body, final Authentication auth) {
            response.setHeader ("Location", this.serverLocation (request) + "/private/logged");
            this.logRequest (request, body);
            return "";
        }

        private String logRequest (final HttpServletRequest request, final String body) {
            final StringBuffer curlLog = new StringBuffer ("curl");

            curlLog.append (" -k ");
            curlLog.append ("-E src/test/resources/clients/libe/libe.pem");
            curlLog.append (" -X ");
            curlLog.append (request.getMethod ());

            for (final Enumeration<String> headerNameEnumeration = request.getHeaderNames (); headerNameEnumeration.hasMoreElements ();) {
                final String headerName = headerNameEnumeration.nextElement ();
                final String headerValue = request.getHeader (headerName);
                curlLog.append (" -H '");
                curlLog.append (headerName);
                curlLog.append (": ");
                curlLog.append (headerValue);
                curlLog.append ("'");

            }

            if (body != null) {
                curlLog.append (" -d '");
                curlLog.append (body.replace ("'", "''"));
                curlLog.append ("'");
            }

            curlLog.append (" ");
            curlLog.append (" '");
            curlLog.append (this.serverLocation (request) + request.getServletPath () + (request.getQueryString () == null ? "" : "?" + request.getQueryString ()));
            curlLog.append ("'");
            RequestMonitor.LOGGER.info (curlLog.toString ());
            return curlLog.toString ();
        }

        @RequestMapping (produces = "text/plain;charset=utf-8")
        @ResponseStatus (code = HttpStatus.OK)
        @ResponseBody
        public String receiveRequest (final HttpServletRequest request, @RequestBody (required = false) final String body) {
            return this.logRequest (request, body);
        }

        @RequestMapping (value = "/public/redirection", produces = MediaType.TEXT_PLAIN_VALUE)
        @ResponseStatus (code = HttpStatus.FOUND)
        @ResponseBody
        public String redirection (final HttpServletRequest request, final HttpServletResponse response, @RequestBody (required = false) final String body) {
            response.setHeader ("Location", this.serverLocation (request) + "/public/redirectedThere");
            this.logRequest (request, body);
            return "";
        }

        private String serverLocation (final HttpServletRequest request) {
            return request.getScheme () + "://" + request.getServerName () + ":" + RequestMonitor.port ();
        }

        @RequestMapping (value = "/public/unauthorized", produces = MediaType.TEXT_PLAIN_VALUE)
        @ResponseStatus (code = HttpStatus.UNAUTHORIZED)
        @ResponseBody
        public String unauthorized (final HttpServletRequest request, final HttpServletResponse response, @RequestBody (required = false) final String body) {
            response.setHeader ("Location", this.serverLocation (request) + "/public/tryagain");
            this.logRequest (request, body);
            return "";
        }
    }

    @Configuration
    @EnableWebSecurity
    static class WebSecurityConfig extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure (final HttpSecurity http) throws Exception {
            http.authorizeRequests ().antMatchers ("/private").permitAll ().anyRequest ().authenticated ().and ().httpBasic ().realmName ("basic").and ().logout ().permitAll ();
        }

        @Override
        public void configure (final WebSecurity http) {
            http.ignoring ().antMatchers ("/public/**");
        }

        @Autowired
        public void configureGlobal (final AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication ().withUser ("user").password ("{noop}password").roles ("USER");
        }
    }

    private static ConfigurableApplicationContext context;
    private static final Logger                   LOGGER = LoggerFactory.getLogger (RequestMonitor.class);

    private static int                            managementPort;

    private static int                            port;

    public static void main (final String [] args) {
        RequestMonitor.start (args);
    }

    public static int port () {
        return RequestMonitor.port;
    }

    public static int [] start () {
        return RequestMonitor.start (new String [0]);
    }

    public static int [] start (final boolean withSsl, final String [] args) {
        final Random random = new Random ();
        RequestMonitor.port = random.nextInt (32767) + 32768;
        RequestMonitor.managementPort = random.nextInt (32767) + 32768;
        RequestMonitor.start (RequestMonitor.port, RequestMonitor.managementPort, withSsl, args);
        return new int [] { RequestMonitor.port, RequestMonitor.managementPort };
    }

    public static void start (final int port, final int managementPort, final boolean withSsl, final String [] args) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put ("server.port", port);
        properties.put ("management.port", managementPort);
        if (withSsl) {
            properties.put ("server.ssl.key-store", "classpath:server/libe/libe.jks");
            properties.put ("server.ssl.key-store-password", "myserverpass");
            properties.put ("server.ssl.trust-store", "classpath:server/libe/libe.jks");
            properties.put ("server.ssl.trust-store-password", "myserverpass");
            properties.put ("server.ssl.client-auth", "need");
            properties.put ("server.ssl.enabled-protocols","SSLv2,SSLv3,TLSv1.0,TLSv1.1,TLSv1.2");
        }
        RequestMonitor.context = new SpringApplicationBuilder()
                .sources (RequestMonitor.class)
                .bannerMode (Banner.Mode.OFF)
                .addCommandLineProperties (true)
                .properties (properties)
                .run(args);
    }

    public static int [] start (final String [] args) {
        return RequestMonitor.start (true, args);
    }

    public static void stop () {
        SpringApplication.exit (RequestMonitor.context, new JobExecutionExitCodeGenerator ());
    }
}