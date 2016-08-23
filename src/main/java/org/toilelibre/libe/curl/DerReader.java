package org.toilelibre.libe.curl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;

class DerReader {
    static class Asn1Object {
        private static final byte LOWER_5_BITS = (byte) 0x1F;
        private final int         length;
        private final int         tag;
        private final int         type;

        private final byte []     value;

        public Asn1Object (final int tag, final int length, final byte [] value) {
            this.tag = tag;
            this.type = tag & Asn1Object.LOWER_5_BITS;
            this.length = length;
            this.value = value;
        }

        public BigInteger getInteger () throws IOException {
            if (this.type != DerReader.INTEGER) {
                throw new IOException ("Invalid DER: object is not integer");
            }

            return new BigInteger (this.value);
        }

        public KeySpec getKeySpec () throws IOException {

            final DerReader parser = this.getReader ();

            parser.read ();
            final BigInteger modulus = parser.read ().getInteger ();
            final BigInteger publicExp = parser.read ().getInteger ();
            final BigInteger privateExp = parser.read ().getInteger ();
            final BigInteger prime1 = parser.read ().getInteger ();
            final BigInteger prime2 = parser.read ().getInteger ();
            final BigInteger exp1 = parser.read ().getInteger ();
            final BigInteger exp2 = parser.read ().getInteger ();
            final BigInteger crtCoef = parser.read ().getInteger ();

            return new RSAPrivateCrtKeySpec (modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef);
        }

        public int getLength () {
            return this.length;
        }

        public DerReader getReader () throws IOException {
            if (!this.isConstructed ()) {
                throw new IOException ("Invalid DER: can't parse primitive entity");
            }

            return new DerReader (this.value);
        }

        public String getString () throws IOException {

            String encoding;

            switch (this.type) {

                case DerReader.NUMERIC_STRING :
                case DerReader.PRINTABLE_STRING :
                case DerReader.VIDEOTEX_STRING :
                case DerReader.IA5_STRING :
                case DerReader.GRAPHIC_STRING :
                case DerReader.ISO646_STRING :
                case DerReader.GENERAL_STRING :
                    encoding = "ISO-8859-1";
                    break;

                case DerReader.BMP_STRING :
                    encoding = "UTF-16BE";
                    break;

                case DerReader.UTF8_STRING :
                    encoding = "UTF-8";
                    break;

                case DerReader.UNIVERSAL_STRING :
                    throw new IOException ("Invalid DER: can't handle UCS-4 string");

                default:
                    throw new IOException ("Invalid DER: object is not a string");
            }

            return new String (this.value, encoding);
        }

        public int getType () {
            return this.type;
        }

        public byte [] getValue () {
            return this.value;
        }

        public boolean isConstructed () {
            return (this.tag & DerReader.CONSTRUCTED) == DerReader.CONSTRUCTED;
        }
    }

    public static final int   ANY                 = 0x00;
    public static final int   APPLICATION         = 0x40;
    public static final int   BIT_STRING          = 0x03;
    public static final int   BMP_STRING          = 0x1E;

    public static final int   BOOLEAN             = 0x01;

    public static final int   BYTE_MAX            = 0xFF;
    public static final int   CONSTRUCTED         = 0x20;
    public static final int   CONTEXT             = 0x80;
    public static final int   ENUMERATED          = 0x0a;
    public static final int   GENERAL_STRING      = 0x1B;
    public static final int   GENERALIZED_TIME    = 0x18;
    public static final int   GRAPHIC_STRING      = 0x19;
    public static final int   IA5_STRING          = 0x16;
    public static final int   INTEGER             = 0x02;
    public static final int   ISO646_STRING       = 0x1A;
    public static final byte  LOWER_7_BITS        = (byte) 0x7F;
    public static final int   MAX_NUMBER_OF_BYTES = 4;
    public static final int   NULL                = 0x05;
    public static final int   NUMERIC_STRING      = 0x12;
    public static final int   OBJECT_IDENTIFIER   = 0x06;
    public static final int   OCTET_STRING        = 0x04;
    public static final int   PRINTABLE_STRING    = 0x13;
    public static final int   PRIVATE             = 0xC0;
    public static final int   REAL                = 0x09;
    public static final int   RELATIVE_OID        = 0x0d;
    public static final int   SEQUENCE            = 0x10;
    public static final int   SET                 = 0x11;
    public static final int   T61_STRING          = 0x14;
    public static final int   UNIVERSAL           = 0x00;
    public static final int   UNIVERSAL_STRING    = 0x1C;

    public static final int   UTC_TIME            = 0x17;
    public static final int   UTF8_STRING         = 0x0C;
    public static final int   VIDEOTEX_STRING     = 0x15;

    private final InputStream in;

    public DerReader (final byte [] bytes) {
        this (new ByteArrayInputStream (bytes));
    }

    public DerReader (final InputStream in) {
        this.in = in;
    }

    private int getLength () throws IOException {

        final int i = this.in.read ();
        if (i == -1) {
            throw new IOException ("Invalid DER: length missing");
        }

        if ((i & ~DerReader.LOWER_7_BITS) == 0) {
            return i;
        }

        final int num = i & DerReader.LOWER_7_BITS;

        if ((i >= DerReader.BYTE_MAX) || (num > DerReader.MAX_NUMBER_OF_BYTES)) {
            throw new IOException ("Invalid DER: length field too big (" + i + ")");
        }

        final byte [] bytes = new byte [num];
        final int n = this.in.read (bytes);
        if (n < num) {
            throw new IOException ("Invalid DER: length too short");
        }

        return new BigInteger (1, bytes).intValue ();
    }

    public Asn1Object read () throws IOException {
        final int tag = this.in.read ();

        if (tag == -1) {
            throw new IOException ("Invalid DER: stream too short, missing tag");
        }

        final int length = this.getLength ();

        final byte [] value = new byte [length];
        final int n = this.in.read (value);
        if (n < length) {
            throw new IOException ("Invalid DER: stream too short, missing value");
        }

        final Asn1Object o = new Asn1Object (tag, length, value);

        return o;
    }
}
