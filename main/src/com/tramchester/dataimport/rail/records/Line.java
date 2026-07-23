package com.tramchester.dataimport.rail.records;

public interface Line {
    static Line of(String text) {
        return AsciiLine.of(text);
    }

    String extractToString(int begin, int end);

    int length();

    char charAt(int index);

    byte[] subArray(int begin, int length);

    boolean isEmpty();

    Line subLine(int begin, int length);
}
