package org.toilelibre.libe.curl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * A generic PEM reader, based on the format outlined in RFC 1421
 */
class PemReader extends BufferedReader {
    private static final String BEGIN = "-----BEGIN ";
    private static final String END   = "-----END ";

    PemReader (final Reader reader) {
        super (reader);
    }

    PemObject readPemObject () throws IOException {
        String line = this.readLine ();

        while (line != null && !line.startsWith (PemReader.BEGIN)) {
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

    private PemObject loadObject (final String type) throws IOException {
        String line;
        final String endMarker = PemReader.END + type;
        final StringBuilder stringBuffer = new StringBuilder ();
        final List<PemHeader> headers = new ArrayList<> ();

        while ((line = this.readLine ()) != null) {
            if (line.contains (":")) {
                final int index = line.indexOf (':');
                final String hdr = line.substring (0, index);
                final String value = line.substring (index + 1).trim ();

                headers.add (new PemHeader (hdr, value));

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

        return new PemObject (type, headers, Base64.getDecoder ().decode (stringBuffer.toString ()));
    }

    static class PemHeader {
        private final String name;
        private final String value;

        /**
         * Base constructor.
         *
         * @param name
         *            name of the header property.
         * @param value
         *            value of the header property.
         */
        PemHeader (final String name, final String value) {
            this.name = name;
            this.value = value;
        }

        public String getName () {
            return this.name;
        }

        public String getValue () {
            return this.value;
        }
    }

    static class PemObject {

        private final String          type;
        private final List<PemHeader> headers;
        private final byte []         content;

        /**
         * Generic constructor for object with headers.
         *
         * @param type
         *            pem object type.
         * @param headers
         *            a list of PemHeader objects.
         * @param content
         *            the binary content of the object.
         */
        PemObject (final String type, final List<PemHeader> headers, final byte [] content) {
            this.type = type;
            this.headers = Collections.unmodifiableList (headers);
            this.content = content;
        }

        String getType () {
            return this.type;
        }

        byte [] getContent () {
            return this.content;
        }

        List<PemHeader> getHeaders () {
            return this.headers;
        }

    }
}
