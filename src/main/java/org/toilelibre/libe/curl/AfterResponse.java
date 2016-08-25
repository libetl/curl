package org.toilelibre.libe.curl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpResponse;
import org.toilelibre.libe.curl.Curl.CurlException;

class AfterResponse {

    static void handle (final CommandLine commandLine, final HttpResponse response) {
        if (commandLine.hasOption (Arguments.OUTPUT.getOpt ())) {
            final File file = new File (commandLine.getOptionValue (Arguments.OUTPUT.getOpt ()));
            FileOutputStream outputStream;
            try {
                outputStream = new FileOutputStream (file);
            } catch (final FileNotFoundException e) {
                throw new CurlException (e);
            }
            try {
                outputStream.write (IOUtils.toByteArray (response.getEntity ().getContent (), (int) response.getEntity ().getContentLength ()));
            } catch (final IOException e) {
                throw new CurlException (e);
            } finally {
                try {
                    outputStream.close ();
                } catch (final IOException e) {
                    throw new CurlException (e);
                }
            }
        }

    }

}
