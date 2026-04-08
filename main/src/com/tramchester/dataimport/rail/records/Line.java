package com.tramchester.dataimport.rail.records;

import java.util.Arrays;
import java.util.Objects;

public class Line {

    final char[] chars;

    public Line(final String text) {
        chars = text.toCharArray();
    }

    public static Line of(final String text) {
        return new Line(text);
    }

    public String extractToString(final int begin, final int end) {
        int count = end-begin;

        int previous = count+1;
        while (Character.isWhitespace(chars[begin+count])) {
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
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        Line line = (Line) object;
        return Objects.deepEquals(chars, line.chars);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(chars);
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
}
