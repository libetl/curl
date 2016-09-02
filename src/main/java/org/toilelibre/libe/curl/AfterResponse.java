package org.toilelibre.libe.curl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.toilelibre.libe.curl.Curl.CurlException;

class AfterResponse {

    private static Logger LOGGER = Logger.getLogger(AfterResponse.class.getName());

    static void handle (final CommandLine commandLine, final HttpResponse response) {

        if (!commandLine.hasOption (Arguments.OUTPUT.getOpt ())) return;

        File file = createTheOutputFile (commandLine.getOptionValue (Arguments.OUTPUT.getOpt ()));
        FileOutputStream outputStream = getOutputStreamFromFile (file);
        writeTheResponseEntityInsideStream (outputStream, response.getEntity());
    }

    private static void writeTheResponseEntityInsideStream(FileOutputStream outputStream, HttpEntity httpEntity) {
        try {
            outputStream.write (IOUtils.toByteArray (httpEntity.getContent (), (int) httpEntity.getContentLength ()));
        } catch (final IOException e) {
            throw new CurlException (e);
        } finally {
            try {
                outputStream.close ();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Cannot flush the file in output");
            }
        }
    }

    private static FileOutputStream getOutputStreamFromFile(File file) {
        try {
            return new FileOutputStream (file);
        } catch (final FileNotFoundException e) {
            throw new CurlException (e);
        }
    }

    private static File createTheOutputFile(String fileName) {
        final File file = new File (fileName);
        try {
            if (!file.createNewFile()){
                throw new CurlException(new RuntimeException("Could not create the file. Does it already exist ?"));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot flush the output file");
            throw new CurlException(e);
        }
        return file;
    }

}
