package com.tramchester.dataimport.rail.records;

import com.google.common.base.Ascii;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Line {

    private static final Charset charset = StandardCharsets.US_ASCII;
    final byte[] bytes;

    private Line(final String text) {
        this(text.getBytes(charset));
    }

    private Line(final byte[] bytes) {
        this.bytes = bytes;
    }

    public static Line of(final String text) {
        return new Line(text);
    }

    public String extractToString(final int begin, final int end) {
        int length = end-begin;

        int previous = length+1;
        // assuming Ascii
        while (bytes[begin+length]==Ascii.SP) {
            previous = length;
            if (length==0) {
                return "";
            }
            length--;
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
            for (int i =0; i < bytes.length; i++) {
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

    public byte[] subArray(final int begin, final int length) {
        final byte[] result = new byte[length];
        System.arraycopy(bytes, begin, result, 0, length);
        return result;
    }

    public boolean isEmpty() {
        return bytes.length==0;
    }

    public Line subLine(final int begin, final int length) {
        return new Line(subArray(begin, length));
    }
}
