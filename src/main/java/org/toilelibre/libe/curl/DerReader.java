package org.toilelibre.libe.curl;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;

class DerReader {
    static class Asn1Object {
        private final int type;
        private final int length;
        private final byte[] value;
        private final int tag;

        private static final byte LOWER_5_BITS = (byte) 0x1F;

        public Asn1Object(int tag, int length, byte[] value) {
            this.tag = tag;
            this.type = tag & LOWER_5_BITS;
            this.length = length;
            this.value = value;
        }

        public int getType() {
            return type;
        }
        
        public int getLength() {
            return length;
        }

        public byte[] getValue() {
            return value;
        }

        public boolean isConstructed() {
            return (tag & DerReader.CONSTRUCTED) == DerReader.CONSTRUCTED;
        }

        public DerReader getReader() throws IOException {
            if (!isConstructed()) {
                throw new IOException("Invalid DER: can't parse primitive entity");
            }

            return new DerReader(value);
        }

        public BigInteger getInteger() throws IOException {
            if (type != DerReader.INTEGER) {
                throw new IOException("Invalid DER: object is not integer");
            }

            return new BigInteger(value);
        }
        
        public String getString() throws IOException {

            String encoding;

            switch (type) {

                case DerReader.NUMERIC_STRING:
                case DerReader.PRINTABLE_STRING:
                case DerReader.VIDEOTEX_STRING:
                case DerReader.IA5_STRING:
                case DerReader.GRAPHIC_STRING:
                case DerReader.ISO646_STRING:
                case DerReader.GENERAL_STRING:
                    encoding = "ISO-8859-1";
                    break;

                case DerReader.BMP_STRING:
                    encoding = "UTF-16BE";
                    break;

                case DerReader.UTF8_STRING:
                    encoding = "UTF-8";
                    break;

                case DerReader.UNIVERSAL_STRING:
                    throw new IOException("Invalid DER: can't handle UCS-4 string");

                default:
                    throw new IOException("Invalid DER: object is not a string");
            }

            return new String(value, encoding);
        }
        
        public KeySpec getKeySpec () throws IOException {

            DerReader parser = this.getReader ();

            parser.read();
            BigInteger modulus = parser.read().getInteger();
            BigInteger publicExp = parser.read().getInteger();
            BigInteger privateExp = parser.read().getInteger();
            BigInteger prime1 = parser.read().getInteger();
            BigInteger prime2 = parser.read().getInteger();
            BigInteger exp1 = parser.read().getInteger();
            BigInteger exp2 = parser.read().getInteger();
            BigInteger crtCoef = parser.read().getInteger();

            return new RSAPrivateCrtKeySpec(
                    modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef);
        }
    }
    public static final int UNIVERSAL = 0x00;
    public static final int APPLICATION = 0x40;
    public static final int CONTEXT = 0x80;
    public static final int PRIVATE = 0xC0;

    public static final int CONSTRUCTED = 0x20;

    public static final int ANY = 0x00;
    public static final int BOOLEAN = 0x01;
    public static final int INTEGER = 0x02;
    public static final int BIT_STRING = 0x03;
    public static final int OCTET_STRING = 0x04;
    public static final int NULL = 0x05;
    public static final int OBJECT_IDENTIFIER = 0x06;
    public static final int REAL = 0x09;
    public static final int ENUMERATED = 0x0a;
    public static final int RELATIVE_OID = 0x0d;
    public static final int SEQUENCE = 0x10;
    public static final int SET = 0x11;
    public static final int NUMERIC_STRING = 0x12;
    public static final int PRINTABLE_STRING = 0x13;
    public static final int T61_STRING = 0x14;
    public static final int VIDEOTEX_STRING = 0x15;
    public static final int IA5_STRING = 0x16;
    public static final int GRAPHIC_STRING = 0x19;
    public static final int ISO646_STRING = 0x1A;
    public static final int GENERAL_STRING = 0x1B;
    public static final int UTF8_STRING = 0x0C;
    public static final int UNIVERSAL_STRING = 0x1C;
    public static final int BMP_STRING = 0x1E;
    public static final int UTC_TIME = 0x17;
    public static final int GENERALIZED_TIME = 0x18;


    public static final byte LOWER_7_BITS = (byte) 0x7F;
    public static final int BYTE_MAX = (int) 0xFF;
    public static final int MAX_NUMBER_OF_BYTES = 4;

    private final InputStream in;

    public DerReader(InputStream in) {
        this.in = in;
    }

    public DerReader (byte[] bytes) {
        this(new ByteArrayInputStream(bytes));
    }

    public Asn1Object read() throws IOException {
        int tag = in.read();

        if (tag == -1) {
            throw new IOException("Invalid DER: stream too short, missing tag");
        }

        int length = getLength();

        byte[] value = new byte[length];
        int n = in.read(value);
        if (n < length) {
            throw new IOException("Invalid DER: stream too short, missing value");
        }

        Asn1Object o = new Asn1Object(tag, length, value);

        return o;
    }

    private int getLength() throws IOException {

        int i = in.read();
        if (i == -1) {
            throw new IOException("Invalid DER: length missing");
        }

        if ((i & ~LOWER_7_BITS) == 0) {
            return i;
        }

        int num = i & LOWER_7_BITS;

        if (i >= BYTE_MAX || num > MAX_NUMBER_OF_BYTES) {
            throw new IOException("Invalid DER: length field too big (" + i + ")");
        }

        byte[] bytes = new byte[num];
        int n = in.read(bytes);
        if (n < num) {
            throw new IOException("Invalid DER: length too short");
        }

        return new BigInteger(1, bytes).intValue();
    }
}
