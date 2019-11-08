package org.toilelibre.libe.curl;

import java.io.*;
import java.util.*;

/**
 * A generic PEM reader, based on the format outlined in RFC 1421
 */
final class PemReader extends BufferedReader {

    static class PemObject {

        private final byte []         content;
        private final String          type;

        /**
         * Generic constructor for object with headers.
         *
         * @param type
         *            pem object type.
         * @param content
         *            the binary content of the object.
         */
        PemObject (final String type, final byte [] content) {
            this.type = type;
            this.content = content;
        }

        byte [] getContent () {
            return this.content;
        }

        String getType () {
            return this.type;
        }

    }

    private static final String BEGIN = "-----BEGIN ";

    private static final String END   = "-----END ";

    PemReader (final Reader reader) {
        super (reader);
    }

    private PemObject loadObject (final String type) throws IOException {
        String line;
        final String endMarker = PemReader.END + type;
        final StringBuilder stringBuffer = new StringBuilder ();

        while ((line = this.readLine ()) != null) {
            if (line.contains (":")) {
                //there is an header. But we don't need them
                continue;
            }

            if (line.contains (endMarker)) {
                break;
            }

            stringBuffer.append (line.trim ());
        }

        if (line == null) {
            throw new IOException (endMarker + " not found");
        }

        return new PemObject (type, Base64.getDecoder ().decode (stringBuffer.toString ()));
    }

    PemObject readPemObject () throws IOException {
        String line = this.readLine ();

        while ((line != null) && !line.startsWith (PemReader.BEGIN)) {
            line = this.readLine ();
        }

        if (line != null) {
            line = line.substring (PemReader.BEGIN.length ());
            final int index = line.indexOf ('-');
            final String type = line.substring (0, index);

            if (index > 0) {
                return this.loadObject (type);
            }
        }

        return null;
    }
}
