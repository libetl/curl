package org.toilelibre.libe.curl;

import java.io.*;
import java.nio.charset.Charset;


class IOUtils {

    public static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static int copy(Reader input, Writer output) throws IOException {
        long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    public static void copy(InputStream input, Writer output, Charset encoding) throws IOException {
        InputStreamReader in = new InputStreamReader(input, encoding);
        copy(in, output);
    }
    
    public static long copyLarge(Reader input, Writer output) throws IOException {
        return copyLarge(input, output, new char[DEFAULT_BUFFER_SIZE]);
    }
    
    public static long copyLarge(Reader input, Writer output, char [] buffer) throws IOException {
        long count = 0;
        int n = 0;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static String toString(InputStream input) throws IOException {
        return toString(input, Charset.defaultCharset());
    }

    public static String toString(InputStream input, Charset encoding) throws IOException {
        StringBuilderWriter sw = new StringBuilderWriter();
        copy(input, sw, encoding);
        return sw.toString();
    }

    static class StringBuilderWriter extends Writer implements Serializable {

        /**
         *
         */
        private static final long serialVersionUID = 8461966367767048539L;
        private final StringBuilder builder;

        public StringBuilderWriter() {
            this.builder = new StringBuilder();
        }

        public StringBuilderWriter(int capacity) {
            this.builder = new StringBuilder(capacity);
        }

        public StringBuilderWriter(StringBuilder builder) {
            this.builder = builder != null ? builder : new StringBuilder();
        }
        @Override
        public Writer append(char value) {
            builder.append(value);
            return this;
        }

        @Override
        public Writer append(CharSequence value) {
            builder.append(value);
            return this;
        }

        @Override
        public Writer append(CharSequence value, int start, int end) {
            builder.append(value, start, end);
            return this;
        }

        @Override
        public void close() {
        }
        @Override
        public void flush() {
        }

        @Override
        public void write(String value) {
            if (value != null) {
                builder.append(value);
            }
        }

        @Override
        public void write(char[] value, int offset, int length) {
            if (value != null) {
                builder.append(value, offset, length);
            }
        }

        public StringBuilder getBuilder() {
            return builder;
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
