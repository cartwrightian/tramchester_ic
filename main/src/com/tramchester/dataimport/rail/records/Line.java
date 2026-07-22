package com.tramchester.dataimport.rail.records;

import com.google.common.base.Ascii;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Line {

    private static final Charset charset = StandardCharsets.US_ASCII;
    private static final CharsetDecoder decoder = charset.newDecoder();
    final byte[] bytes;
    //private final int hashCode;

    public Line(final String text) {
        this(text.getBytes(charset));
    }

    private Line(final byte[] bytes) {
        this.bytes = bytes;
    }

    public static Line of(final String text) {
        return new Line(text);
    }

    public String extractToString(final int begin, final int end) {
        int count = end-begin;

        int previous = count+1;
        // assuming ascii
        while (bytes[begin+count]==Ascii.SP) {
            previous = count;
            if (count==0) {
                return "";
            }
            count--;
        }

        final byte[] dest = new byte[previous];
        System.arraycopy(bytes, begin, dest, 0, previous); // performance
        return new String(dest, charset);
    }

    public int length() {
        return bytes.length;
    }

    public char charAt(final int index) {
        // hacky
        return (char) (bytes[index] & 0xFF);
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        final Line other = (Line) object;
        if (other.bytes.length == bytes.length) {
            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i]!=other.bytes[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (int i = bytes.length - 1; i >= 0; i--)
            hash = 31 * hash + (int)bytes[i];
        return hash;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i>0) {
                result.append(", ");
            }
            result.append(i).append(":'").append(bytes[i]).append("'");
        }
        return "Line{" +
                "text='" + new String(bytes, charset) + "', " +
                "bytes=" + result +
                '}';
    }

    public byte[] subArray(final int begin, final int count) {
        final byte[] result = new byte[count];
        System.arraycopy(bytes, begin, result, 0, count);
        return result;
    }

    public boolean isEmpty() {
        return bytes.length==0;
    }

    public char[] getChars() {
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).array();
        } catch (CharacterCodingException e) {
            throw new RuntimeException("Could not decode " + Arrays.toString(bytes), e);
        }
    }
}
