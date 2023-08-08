package org.toilelibre.libe.curl;

import org.apache.commons.cli.*;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.toilelibre.libe.curl.Curl.*;

import java.io.*;
import java.util.logging.*;

final class AfterResponse {

    private static Logger LOGGER = Logger.getLogger (AfterResponse.class.getName ());

    static void handle (final CommandLine commandLine, final HttpResponse response) {

        if (!commandLine.hasOption (Arguments.OUTPUT.getOpt ())) return;

        File file = createTheOutputFile (commandLine.getOptionValue (Arguments.OUTPUT.getOpt ()));
        FileOutputStream outputStream = getOutputStreamFromFile (file);
        writeTheResponseEntityInsideStream (outputStream, ((ClassicHttpResponse) response).getEntity ());
    }

    private static void writeTheResponseEntityInsideStream (FileOutputStream outputStream, HttpEntity httpEntity) {
        try {
            if (httpEntity.getContentLength () >= 0) {
                outputStream.write (IOUtils.toByteArray (httpEntity.getContent (), (int) httpEntity.getContentLength ()));
            }
            else {
                outputStream.write (IOUtils.toByteArray (httpEntity.getContent ()));
            }
        } catch (final IOException e) {
            throw new CurlException (e);
        } finally {
            try {
                outputStream.close ();
            } catch (IOException e) {
                LOGGER.log (Level.WARNING, "Cannot flush the file in output");
            }
        }
    }

    private static FileOutputStream getOutputStreamFromFile (File file) {
        try {
            return new FileOutputStream (file);
        } catch (final FileNotFoundException e) {
            throw new CurlException (e);
        }
    }

    private static File createTheOutputFile (String fileName) {
        final File file = new File (fileName);
        try {
            if (!file.createNewFile ()){
                throw new CurlException (new IOException ("Could not create the file. Does it already exist ?"));
            }
        } catch (IOException e) {
            LOGGER.log (Level.WARNING, "Cannot flush the output file");
            throw new CurlException (e);
        }
        return file;
    }

}
