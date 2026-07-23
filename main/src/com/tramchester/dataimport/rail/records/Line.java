package com.tramchester.dataimport.rail.records;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface Line {
    static Line of(final String text) {
        if (text.length()==1) {
            throw new RuntimeException("Use a char");
        }
        if (text.length()==2) {
            return new TwoChars(text);
        }
        return AsciiLine.of(text);
    }

    static Line of(final byte[] bytes) {
        if (bytes.length==2) {
            return new TwoChars(bytes);
        }
        if (bytes.length>2) {
            return new AsciiLine(bytes);
        }
        throw new IndexOutOfBoundsException(bytes.length);
    }

    String extractToString(int begin, int end);

    int length();

    char charAt(int index);

    byte[] subArray(int begin, int length);

    boolean isEmpty();

    Line subLine(int begin, int length);

    class TwoChars implements Line {
        static final Charset charset = StandardCharsets.US_ASCII;

        private final byte[] bytes;

        private TwoChars(final String text) {
            this(text.getBytes(charset));
        }

        public TwoChars(final byte[] bytes) {
            if (bytes.length!=2) {
                throw new IndexOutOfBoundsException(bytes.length);
            }
            this.bytes = bytes;
        }

        @Override
        public String extractToString(int begin, int end) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public int length() {
            return 2;
        }

        @Override
        public char charAt(final int index) {
            return (char) (bytes[index] & 0xFF);
        }

        @Override
        public byte[] subArray(int begin, int length) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Line subLine(int begin, int length) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TwoChars twoChars = (TwoChars) o;
            return twoChars.bytes[0]==bytes[0] && twoChars.bytes[1]==bytes[1];
        }

        @Override
        public int hashCode() {
            return (31 * bytes[0]) + bytes[1];
        }
    }
}
