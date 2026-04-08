package com.tramchester.unit.dataimport.rail;

import com.tramchester.dataimport.rail.records.Line;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LineTest {

    @Test
    void shouldTrimOnExtract() {
        Line line = Line.of("DRBY     ");
        String result = line.extractToString(0, 7);

        assertEquals("DRBY", result);
    }

    @Test
    void shouldTrimOToEmpty() {
        Line line = Line.of("         ");
        String result = line.extractToString(0, 7);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldTrimToEmptyOneChar() {
        Line line = Line.of(" ");
        String result = line.extractToString(0, 0);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldTrimToEmptyTwoChar() {
        Line line = Line.of("  ");
        String result = line.extractToString(0, 1);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotTrimToEmptyOneChar() {
        Line line = Line.of("1");
        String result = line.extractToString(0, 0);

        assertEquals("1", result);
    }

    @Test
    void shouldGetChatAt() {
        Line line = Line.of("123456789");

        assertEquals('1', line.charAt(0));
        assertEquals('9', line.charAt(8));
    }

    @Test
    void shouldHaveSubArray() {
        Line line = Line.of("0123456789");

        assertEquals("0123", new String(line.subArray(0,4)));
        assertEquals("123", new String(line.subArray(1,3)));
        assertEquals("7", new String(line.subArray(7,1)));

    }
}
