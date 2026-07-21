package com.tramchester.dataimport.rail.records;

import java.util.Arrays;

public class Line {

    final char[] chars;
    private final int hashCode;

    public Line(final String text) {
        this(text.toCharArray());
    }

    private Line(final char[] chars) {
        this.chars = chars;
        hashCode = Arrays.hashCode(chars);
    }

    public static Line of(final String text) {
        return new Line(text);
    }

    public static Line of(final char[] chars) {
        return new Line(Arrays.copyOf(chars, chars.length));
    }

    public String extractToString(final int begin, final int end) {
        int count = end-begin;

        int previous = count+1;
        // assuming ascii
        while (chars[begin+count]==32) {
            previous = count;
            if (count==0) {
                return "";
            }
            count--;
        }

        return new String(chars, begin, previous);
    }

    public int length() {
        return chars.length;
    }

    public char charAt(int index) {
        return chars[index];
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        final Line line = (Line) object;
        if (line.chars.length == chars.length) {
            for (int i = 0; i < chars.length; i++) {
                if (chars[i]!=line.chars[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (i>0) {
                result.append(", ");
            }
            result.append(i).append(":'").append(chars[i]).append("'");
        }
        return "Line{" +
                "text='" + new String(chars) + "', " +
                "chars=" + result +
                '}';
    }

    public char[] subArray(final int begin, final int count) {
        final char[] result = new char[count];
        System.arraycopy(chars, begin, result, 0, count);
        return result;
    }

    public boolean isEmpty() {
        return chars.length==0;
    }
}
