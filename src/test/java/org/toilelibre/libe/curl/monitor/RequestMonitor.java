package org.toilelibre.libe.curl.monitor;

import java.util.Enumeration;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.JobExecutionExitCodeGenerator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
public class RequestMonitor {
    private static ConfigurableApplicationContext context;
    private static int                            managementPort;
    private static int                            port;
    
    private static final Logger LOGGER = LoggerFactory.getLogger (RequestMonitor.class);

    public static void main (String [] args) {
        System.setProperty ("spring.output.ansi.enabled", "detect");
        RequestMonitor.start (8080, 8092, false);
    }
    
    @Controller
    @RequestMapping ("/**")
    static class MonitorController {
        

        @RequestMapping (value = "redirection", produces = MediaType.TEXT_PLAIN_VALUE)
        @ResponseStatus (code = HttpStatus.FOUND)
        @ResponseBody
        public String redirection (HttpServletRequest request, HttpServletResponse response, @RequestBody (required = false) String body) {
            response.setHeader ("Location", serverLocation (request) + "/redirectedThere");
            logRequest (request, body);
            return "";
        }

        @RequestMapping (value = "unauthorized", produces = MediaType.TEXT_PLAIN_VALUE)
        @ResponseStatus (code = HttpStatus.UNAUTHORIZED)
        @ResponseBody
        public String unauthorized (HttpServletRequest request, HttpServletResponse response, @RequestBody (required = false) String body) {
            response.setHeader ("Location", serverLocation (request) + "/authenticate");
            logRequest (request, body);
            return "";
        }
        
        @RequestMapping (produces = MediaType.TEXT_PLAIN_VALUE)
        @ResponseStatus (code = HttpStatus.OK)
        @ResponseBody
        public String receiveRequest (HttpServletRequest request, @RequestBody (required = false) String body) {
            return logRequest (request, body);
        }

        private String logRequest (HttpServletRequest request, String body) {
            StringBuffer curlLog = new StringBuffer ("curl");

            curlLog.append (" -k ");
            curlLog.append (" -X ");
            curlLog.append (request.getMethod ());
            
            for (Enumeration<String> headerNameEnumeration = request.getHeaderNames () ; headerNameEnumeration.hasMoreElements () ;) {
                String headerName = headerNameEnumeration.nextElement ();
                String headerValue = request.getHeader (headerName);
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
            curlLog.append (serverLocation(request) +
                    request.getServletPath () + 
                    (request.getQueryString ()== null ? "" : "?" + request.getQueryString ()));
            curlLog.append ("'");
            LOGGER.info (curlLog.toString ());
            return curlLog.toString ();
        }

        private String serverLocation (HttpServletRequest request) {
            return request.getScheme () + "://" + request.getServerName () + ":" + port();
        }
    }
    
    public static int [] start () {
        return start (true);
    }
    
    public static int [] start (boolean withSsl) {
        final Random random = new Random ();
        port = random.nextInt (32767) + 32768;
        managementPort = random.nextInt (32767) + 32768;
        start (port, managementPort, withSsl);
        return new int [] { port, managementPort };
    }
    
    public static void start (int port, int managementPort, boolean withSsl) {
        System.setProperty ("server.port", String.valueOf (port));
        System.setProperty ("managementPort.port", String.valueOf (managementPort));
        System.setProperty ("server.ssl.key-store", "classpath:keystore.jks");
        System.setProperty ("server.ssl.key-store-password", "password");
        System.setProperty ("server.ssl.key-password", "password");
        
        context = SpringApplication.run (RequestMonitor.class, new String [0]);
    }
    
    public static void stop () {
        SpringApplication.exit (context, new JobExecutionExitCodeGenerator ());
    }
    
    public static int port () {
        return port;
    }
}