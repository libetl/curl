package org.toilelibre.libe.outside.curl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleProxyServer {

    public static final Logger LOGGER = LoggerFactory.getLogger (SimpleProxyServer.class);

    public static void start (final int remotePort) {
        final String host = "localhost";
        final int localPort = 8080;
        SimpleProxyServer.LOGGER.info ("Starting proxy for " + host + ":" + remotePort + " on port " + localPort);

        new Thread () {
            @Override
            public void run () {
                try {
                    SimpleProxyServer.runServer (host, remotePort, localPort);
                } catch (final IOException e) {
                    throw new RuntimeException (e);
                }
            }
        }.start ();
        try {
            Thread.sleep (300);
        } catch (final InterruptedException e) {
            throw new RuntimeException (e);
        }
    }

    public static void runServer (final String host, final int remotePort, final int localPort) throws IOException {
        final ServerSocket ss = new ServerSocket (localPort);

        final byte [] request = new byte [1024];
        final byte [] reply = new byte [4096];
        Socket client = null;
        Socket server = null;

        try {
            client = ss.accept ();

            final InputStream streamFromClient = client.getInputStream ();
            final OutputStream streamToClient = client.getOutputStream ();

            server = new Socket (host, remotePort);
            final InputStream streamFromServer = server.getInputStream ();
            final OutputStream streamToServer = server.getOutputStream ();

            final Thread t = new Thread () {
                @Override
                public void run () {
                    try {
                        int bytesRead;
                        while ((bytesRead = streamFromClient.read (request)) != -1) {
                            streamToServer.write (request, 0, bytesRead);
                            streamToServer.flush ();
                        }
                        streamToServer.close ();
                    } catch (final IOException e) {
                        e.printStackTrace ();
                    }
                }
            };

            t.start ();

            int bytesRead;
            while ((bytesRead = streamFromServer.read (reply)) != -1) {
                streamToClient.write (reply, 0, bytesRead);
                streamToClient.flush ();
            }

            streamToClient.close ();
        } finally {
            if (server != null) {
                server.close ();
            }
            if (client != null) {
                client.close ();
            }
            ss.close ();
        }
    }
}
