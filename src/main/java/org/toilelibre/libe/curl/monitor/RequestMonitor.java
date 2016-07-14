package org.toilelibre.libe.curl.monitor;

import java.util.Enumeration;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

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
        RequestMonitor.start (8080, 8092);
    }
    
    @Controller
    @RequestMapping ("/**")
    static class MonitorController {
        
        @RequestMapping (produces = MediaType.TEXT_PLAIN_VALUE)
        @ResponseStatus (code = HttpStatus.OK)
        @ResponseBody
        public String receiveRequest (HttpServletRequest request, @RequestBody (required = false) String body) {
            StringBuffer curlLog = new StringBuffer ("curl");
            
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
            curlLog.append (request.getScheme () + "://" + request.getServerName () + ":" + port() +
                    request.getServletPath () + 
                    (request.getQueryString ()== null ? "" : "?" + request.getQueryString ()));
            curlLog.append ("'");
            LOGGER.info (curlLog.toString ());
            return curlLog.toString ();
        }
    }
    
    public static int [] start () {
        final Random random = new Random ();
        port = random.nextInt (32767) + 32768;
        managementPort = random.nextInt (32767) + 32768;
        start (port, managementPort);
        return new int [] { port, managementPort };
    }
    public static void start (int port, int managementPort) {
        System.setProperty ("server.port", String.valueOf (port));
        System.setProperty ("managementPort.port", String.valueOf (managementPort));
        context = SpringApplication.run (RequestMonitor.class, new String [0]);
    }
    
    public static void stop () {
        SpringApplication.exit (context, new JobExecutionExitCodeGenerator ());
    }
    
    public static int port () {
        return port;
    }
}