package com.tramchester.unit.domain.dates;

import com.tramchester.domain.dates.*;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.EnumSet;

import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;

public class MutableDaysBitmapTest {

    private TramDate when;
    private long epochDay;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        epochDay = when.toEpochDay();
    }

    @Test
    void shouldNotContainIfBeforeOfAfter() {
        int size = 100;
        MutableDaysBitmap days = new MutableDaysBitmap(epochDay, size);

        TramDate before = when.minusDays(1);
        assertFalse(days.isSet(before));

        TramDate after = when.plusDays(size).plusDays(1);
        assertFalse(days.isSet(after));
    }

    @Test
    void shouldSetADay() {
        MutableDaysBitmap days = new MutableDaysBitmap(epochDay, 100);

        assertEquals(epochDay, days.getBeginningEpochDay());

        assertEquals(0, days.numberSet());
        assertTrue(days.noneSet());

        TramDate date = when.plusDays(5);
        assertFalse(days.isSet(date));

        days.set(date);
        assertTrue(days.isSet(date));

        days.clear(date);
        assertFalse(days.isSet(date));
        assertTrue(days.noneSet());

        for (int i = 0; i < 100; i++) {
            days.set(when.plusDays(i));
        }
        assertEquals(100, days.numberSet());

        days.clearAll();
        assertTrue(days.noneSet());

    }

    @Test
    void shouldSetDaysOfWeek() {
        MutableDaysBitmap days = new MutableDaysBitmap(epochDay, 100);

        EnumSet<DayOfWeek> daysOfWeek = EnumSet.of(MONDAY, WEDNESDAY, SATURDAY);
        days.setDaysOfWeek(daysOfWeek);

        for (int i = 0; i < 100; i++) {
            TramDate date = when.plusDays(i);
            if (daysOfWeek.contains(date.getDayOfWeek())) {
                assertTrue(days.isSet(date), date.toString());
            } else {
                assertFalse(days.isSet(date), date.toString());
            }
        }
    }

    @Test
    void shouldSetAllDaysAsExpected() {

        TramDate startDate = when;
        int size = 5;
        TramDate endDate = when.plusDays(size-1);

        MutableDaysBitmap daysBitmap = new MutableDaysBitmap(epochDay, size);

        daysBitmap.setDaysOfWeek(TestEnv.allDays());

        assertTrue(daysBitmap.isSet(startDate));
        assertTrue(daysBitmap.isSet(endDate));
    }

    @Test
    void shouldGiveExpectedResultsWhenMatchingDateAndSize() {
        MutableDaysBitmap first = new MutableDaysBitmap(epochDay,14);
        MutableDaysBitmap second = new MutableDaysBitmap(epochDay, 14);

        assertFalse(first.anyOverlap(second));
        assertFalse(second.anyOverlap(first));

        TramDate date = when.plusDays(1);

        first.set(date);
        assertFalse(first.anyOverlap(second));
        second.set(date);
        assertTrue(first.anyOverlap(second));
    }

    @Test
    void shouldGiveExpectedResultsWhenOverlapDateAtEnd() {
        MutableDaysBitmap first = new MutableDaysBitmap(epochDay,14);
        MutableDaysBitmap second = new MutableDaysBitmap(epochDay+5, 14);

        assertFalse(first.anyOverlap(second));
        assertFalse(second.anyOverlap(first));

        TramDate date = when.plusDays(1 + 5);

        first.set(date);
        assertFalse(first.anyOverlap(second));
        second.set(date);
        assertTrue(first.anyOverlap(second));
    }

    @Test
    void shouldGiveExpectedResultsWhenOverlapDateAtStart() {
        MutableDaysBitmap first = new MutableDaysBitmap(epochDay,14);
        MutableDaysBitmap second = new MutableDaysBitmap(epochDay-5, 14);

        assertFalse(first.anyOverlap(second));
        assertFalse(second.anyOverlap(first));

        TramDate date = when.plusDays(1);

        first.set(date);
        assertFalse(first.anyOverlap(second));
        second.set(date);
        assertTrue(first.anyOverlap(second));
    }

    @Test
    void shouldInsertAnotherOverlapsBeforeSamller() {
        MutableDaysBitmap first = new MutableDaysBitmap(epochDay,14);
        MutableDaysBitmap second = new MutableDaysBitmap(epochDay-5, 10);

        TramDate date = when.plusDays(1);
        second.set(date);

        first.insert(second);
        assertTrue(first.isSet(date));
    }

    @Test
    void shouldInsertAnotherOverlapsBeforeSLarger() {
        MutableDaysBitmap first = new MutableDaysBitmap(epochDay,14);
        MutableDaysBitmap second = new MutableDaysBitmap(epochDay-5, 28);

        TramDate date = when.plusDays(1);
        second.set(date);
        second.set(when.plusDays(28-5-1)); // set one bit that does not overlap

        first.insert(second);
        assertTrue(first.isSet(date));
    }

    @Test
    void shouldInsertAnotherOverlapsAfter() {
        MutableDaysBitmap first = new MutableDaysBitmap(epochDay,14);
        MutableDaysBitmap second = new MutableDaysBitmap(epochDay+5, 18);

        TramDate date = when.plusDays(1 + 5);
        second.set(date);

        first.insert(second);
        assertTrue(first.isSet(date));
    }

    @Test
    void shouldInsertAnotherOverlapsAfterSmaller() {
        MutableDaysBitmap first = new MutableDaysBitmap(epochDay,14);
        MutableDaysBitmap second = new MutableDaysBitmap(epochDay+5, 5);

        TramDate date = when.plusDays(1 + 5);
        second.set(date);

        first.insert(second);
        assertTrue(first.isSet(date));
    }

    @Test
    void shouldHaveExpectedOverlapMiddle() {
        MutableDaysBitmap first = new MutableDaysBitmap(16485,269);
        MutableDaysBitmap second = new MutableDaysBitmap(16612, 54);

        first.setDaysOfWeek(TestEnv.allDays());
        second.setDaysOfWeek(TestEnv.allDays());

        assertTrue(first.anyOverlap(second));
        assertTrue(second.anyOverlap(first));
    }

    // java.lang.RuntimeException: fromIndex 108 > toIndex: 107 this: DaysBitmap{beginningDay=16541, days={1, 5, 6, 8, 9, 12, 13, 15, 16, 19, 20, 22, 23, 26, 27, 29, 30, 33, 34, 36, 37, 40, 41, 43, 44, 47, 48, 50, 51, 54, 55, 57, 58, 61, 62, 64, 65, 68, 69, 71, 72, 75, 76, 78, 79, 82, 83, 85, 86, 89, 90, 92, 93, 96, 97, 99, 100, 103, 104, 106, 107}, size=108}
    // otherStartDay: 16649 other size: 64

    @Test
    void shouldHaveExpectedOverlapsLastDay() {
        // fromIndex 153 > toIndex: 152 this: DaysBitmap{beginningDay=16527, days={}, size=153} otherStartDay: 16680 other size: 317

        MutableDaysBitmap first = new MutableDaysBitmap(epochDay, 3);
        MutableDaysBitmap second = new MutableDaysBitmap(epochDay+2, 15);


        first.set(when.plusDays(2));
        second.set(when.plusDays(2));

        assertTrue(first.anyOverlap(second), "no overlap " + first + " and " + second);
        assertTrue(second.anyOverlap(first), "no overlap " + first + " and " + second);
    }

    @Test
    void shouldInsertAnotherOverlapsExact() {
        MutableDaysBitmap first = new MutableDaysBitmap(epochDay,14);
        MutableDaysBitmap second = new MutableDaysBitmap(epochDay, 14);

        second.set(when);
        TramDate lastDay = when.plusDays(13);
        second.set(lastDay);

        first.insert(second);
        assertTrue(first.isSet(when));
        assertTrue(first.isSet(lastDay));

    }

    @Test
    void shouldHaveExpectedOverlap() {
        TramDate startDate = TramDate.of(2020, 11, 5);
        TramDate endDate = TramDate.of(2020, 11, 25);

        EnumSet<DayOfWeek> monday = EnumSet.of(MONDAY);

        MutableDaysBitmap serviceA = createDaysBitmap(startDate, endDate, monday);
        MutableDaysBitmap serviceB = createDaysBitmap(startDate, endDate, EnumSet.of(DayOfWeek.SUNDAY));
        MutableDaysBitmap serviceC = createDaysBitmap(startDate.minusDays(10), startDate.minusDays(5), monday);
        MutableDaysBitmap serviceD = createDaysBitmap(startDate, endDate, monday);

        assertTrue(serviceA.anyOverlap(serviceA));

        // wrong operating days
        assertFalse(serviceA.anyOverlap(serviceB));

        // before dates
        assertFalse(serviceA.anyOverlap(serviceC));

        // should match
        assertTrue(serviceA.anyOverlap(serviceD));
    }

    private MutableDaysBitmap createDaysBitmap(TramDate startDate, TramDate endDate, EnumSet<DayOfWeek> operatingDays) {
        long begin = startDate.toEpochDay();
        int size = Math.toIntExact(Math.subtractExact(endDate.toEpochDay(), begin));
        MutableDaysBitmap daysBitmap = new MutableDaysBitmap(begin, size);
        daysBitmap.setDaysOfWeek(operatingDays);
        return daysBitmap;
    }
}
