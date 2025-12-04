package org.toilelibre.libe.outside.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
public class StupidHttpServer {

    private static ConfigurableApplicationContext context;

    private static int                            managementPort;

    private static int                            port;

    public static void main (final String [] args) {
        StupidHttpServer.start (args);
    }

    public static int port () {
        return StupidHttpServer.port;
    }

    public static int [] start () {
        return StupidHttpServer.start (new String [0]);
    }

    public static int [] start (final String [] args) {
        final Random random = new Random ();
        StupidHttpServer.port = random.nextInt (32767) + 32768;
        StupidHttpServer.managementPort = random.nextInt (32767) + 32768;
        StupidHttpServer.start (StupidHttpServer.port, StupidHttpServer.managementPort, args);
        return new int [] { StupidHttpServer.port, StupidHttpServer.managementPort };
    }

    public static void start (final int port, final int managementPort, final String [] args) {
        Map<String, Object> properties = new HashMap<String, Object> ();
        properties.put ("server.port", port);
        properties.put ("management.port", managementPort);

        StupidHttpServer.context = new SpringApplicationBuilder ().sources (StupidHttpServer.class).bannerMode (Banner.Mode.OFF).addCommandLineProperties (true).properties (properties).run (args);
    }

    public static void stop () {
        SpringApplication.exit (StupidHttpServer.context, () -> 0);
    }
}