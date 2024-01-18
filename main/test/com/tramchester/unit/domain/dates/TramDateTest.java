package com.tramchester.unit.domain.dates;

import com.tramchester.domain.dates.TramDate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TramDateTest {

    @Test
    void shouldHaveSameEpochDay() {
        for (int i = 1; i < 30; i++) {
            LocalDate date = LocalDate.of(2022, 7, i);
            TramDate tramDate = TramDate.of(2022, 7, i);

            assertEquals(date.toEpochDay(), tramDate.toEpochDay());
            assertEquals(date.getDayOfWeek(), tramDate.getDayOfWeek());
        }

        TramDate tramDate = TramDate.of(2022, 7, 1);
        for (int i = 1; i < 30; i++) {
            LocalDate date = LocalDate.of(2022, 7, i);

            assertEquals(date.toEpochDay(), tramDate.toEpochDay());
            assertEquals(date.getDayOfWeek(), tramDate.getDayOfWeek());

            tramDate = tramDate.plusDays(1);
        }
    }

    @Test
    void shouldHaveMin() {
        TramDate tramDateA = TramDate.of(2022, 11, 20);
        TramDate tramDateB = TramDate.of(2021, 10, 28);

        assertEquals(tramDateB, TramDate.min(tramDateA, tramDateB));
        assertEquals(tramDateB, TramDate.min(tramDateB, tramDateA));
    }

    @Test
    void shouldHaveMax() {
        TramDate tramDateA = TramDate.of(2022, 11, 20);
        TramDate tramDateB = TramDate.of(2021, 10, 28);

        assertEquals(tramDateA, TramDate.max(tramDateA, tramDateB));
        assertEquals(tramDateA, TramDate.max(tramDateB, tramDateA));
    }

    @Test
    void shouldMinusWeek() {
        TramDate tramDate = TramDate.of(2022, 11, 20);

        TramDate result = tramDate.minusWeeks(1);

        assertEquals(TramDate.of(2022,11, 13), result);
    }

    @Test
    void shouldPlusWeek() {
        TramDate tramDate = TramDate.of(2022, 11, 20);

        TramDate result = tramDate.plusWeeks(1);

        assertEquals(TramDate.of(2022,11, 27), result);
    }

    @Test
    void shouldParseDate() {
        String txt = "20220530";

        TramDate tramDate = TramDate.parseSimple(txt,0);

        assertEquals(TramDate.of(2022, 5, 30), tramDate);
    }

    @Test
    void shouldParseDateOffset() {
        String txt = "xyz20220530";

        TramDate tramDate = TramDate.parseSimple(txt,3);

        assertEquals(TramDate.of(2022, 5, 30), tramDate);
    }

    @Test
    void shouldParseDateNoCentury() {
        String txt = "220530";

        TramDate tramDate = TramDate.parseSimple(txt, 20, 0);

        assertEquals(TramDate.of(2022, 5, 30), tramDate);
    }

    @Test
    void shouldParseDateNoCenturyOffset() {
        String txt = "abcd220530";

        TramDate tramDate = TramDate.parseSimple(txt, 20, 4);

        assertEquals(TramDate.of(2022, 5, 30), tramDate);
    }
}
