package com.tramchester.unit.domain.time;

import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class TimeRangeTest {

    @Test
    void shouldHaveSimpleRange() {
        TramTime timeA = TramTime.of(10,31);
        TramTime timeB = TramTime.of(15,56);

        TimeRange range = TimeRangePartial.of(timeA, timeB);

        assertTrue(range.contains(timeA));
        assertTrue(range.contains(timeB));

        assertTrue(range.contains(TramTime.of(12,11)));

        assertFalse(range.contains(timeA.minusMinutes(1)));
        assertFalse(range.contains(timeB.plusMinutes(1)));

        assertFalse(range.intoNextDay());

    }

    @Test
    void shouldHaveExpandingRange() {
        TramTime timeA = TramTime.of(9,42);

        TimeRange range = TimeRangePartial.of(timeA);

        assertTrue(range.contains(timeA));
        assertFalse(range.contains(timeA.plusMinutes(30)));
        assertFalse(range.contains(timeA.minusMinutes(30)));

        range.updateToInclude(timeA.plus(Duration.ofHours(1)));
        assertTrue(range.contains(timeA.plusMinutes(30)));

        range.updateToInclude(timeA.minus(Duration.ofHours(1)));
        assertTrue(range.contains(timeA.minusMinutes(30)));

    }

    @Test
    void shouldBehaveOverMidnightBasic() {
        TramTime time = TramTime.of(23,55);

        TimeRange timeRange = TimeRangePartial.of(time, Duration.ZERO, Duration.ofHours(2));

        assertTrue(timeRange.contains(TramTime.nextDay(1,15)), timeRange.toString());
        assertFalse(timeRange.contains(TramTime.of(1,15)), timeRange.toString());
    }

    @Test
    void shouldHaveAnyoverLap() {
        TramTime time = TramTime.of(12,55);

        TimeRange timeRangeA = TimeRangePartial.of(time, Duration.ofHours(2), Duration.ofHours(2));
        TimeRange timeRangeB = TimeRangePartial.of(time, Duration.ofHours(1), Duration.ofHours(1));

        assertTrue(timeRangeA.anyOverlap(timeRangeB));
        assertTrue(timeRangeB.anyOverlap(timeRangeA));

        TimeRange timeRangeC = TimeRangePartial.of(TramTime.of(22,0), Duration.ofHours(1), Duration.ofHours(1));

        assertFalse(timeRangeA.anyOverlap(timeRangeC));
        assertFalse(timeRangeC.anyOverlap(timeRangeA));

    }

    @Test
    void shouldCalculateProperlyOverMidnightRange() {
        TramTime begin = TramTime.of(23,55);
        TramTime end = TramTime.nextDay(1,24);

        TimeRange timeRange = TimeRangePartial.of(begin, end);

        TimeRange beginOverlapsRange = TimeRangePartial.of(TramTime.of(23, 56), Duration.ZERO, Duration.ofHours(2));
        assertTrue(timeRange.anyOverlap(beginOverlapsRange));

        TimeRange endOverlaps = TimeRangePartial.of(begin, Duration.ofMinutes(30), Duration.ofMinutes(5));
        assertTrue(timeRange.anyOverlap(endOverlaps));

    }

    @Test
    void shouldSplitIntoBeforeOrAfterMidnight() {
        TramTime begin = TramTime.of(23, 55);
        TramTime endInTheNextDay = TramTime.nextDay(1, 24);

        TimeRange timeRange = TimeRangePartial.of(begin, endInTheNextDay);

        assertTrue(timeRange.intoNextDay());

        TimeRange nextDayPart = timeRange.forFollowingDay();

        assertFalse(nextDayPart.intoNextDay());
        assertFalse(nextDayPart.contains(begin));
        assertFalse(nextDayPart.contains(endInTheNextDay));
        assertTrue(nextDayPart.contains(TramTime.of(1,24)));
    }

    @Test
    void shouldReportNextDayCorrectlyWhenExactlyOnMidnight() {
        //begin=TramTime{h=21, m=56}, end=TramTime{d=1 h=0, m=0}}

        TimeRange range = TimeRangePartial.of(TramTime.of(21,56), TramTime.nextDay(0,0));
        assertTrue(range.intoNextDay());
    }

    @Test
    void shouldBottomAtBeginingOfDay() {
        TramTime time = TramTime.of(0, 14);

        TimeRange range = TimeRangePartial.of(time, Duration.ofMinutes(20), Duration.ofMinutes(12));

        assertTrue(range.contains(TramTime.of(0,0)));
        assertTrue(range.contains(TramTime.of(0,26)));
        assertFalse(range.contains(TramTime.of(23,59)));
        assertFalse(range.contains(TramTime.of(0,30)));
    }

    @Test
    void shouldBottomAtBeginingOfNextDay() {
        TramTime time = TramTime.nextDay(0, 14);

        TimeRange range = TimeRangePartial.of(time, Duration.ofMinutes(20), Duration.ofMinutes(12));

        assertTrue(range.contains(TramTime.nextDay(0,0)));
        assertTrue(range.contains(TramTime.nextDay(0,26)));

        assertTrue(range.contains(TramTime.of(23,59)));

        assertFalse(range.contains(TramTime.of(0,30)));
    }

    @Test
    void shouldHaveRangeCoveringAllOfSingle() {

        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8,45), TramTime.of(9,55));
        TimeRange result = TimeRange.coveringAllOf(Collections.singleton(timeRange));

        assertEquals(timeRange, result);
    }

    @Test
    void shouldHaveRangeCoveringAllOf() {

        TramTime earliest = TramTime.of(8, 45);
        TramTime latest = TramTime.of(17, 15);

        TimeRange timeRangeA = TimeRangePartial.of(earliest, TramTime.of(9,55));
        TimeRange timeRangeB = TimeRangePartial.of(TramTime.of(9,15), TramTime.of(16,35));
        TimeRange timeRangeC = TimeRangePartial.of(TramTime.of(16,25), latest);

        TimeRange result = TimeRange.coveringAllOf(new HashSet<>(Arrays.asList(timeRangeB, timeRangeA, timeRangeC)));

        assertEquals(TimeRangePartial.of(earliest, latest), result);
    }

    @Test
    void shouldHaveAllDay() {
        final TimeRange allDay = TimeRange.AllDay();

        assertEquals(TramTime.of(0,1), allDay.getStart());
        assertEquals(TramTime.of(0,0), allDay.getEnd());

        TramTime time = TramTime.of(0,1);

        while(time.isBefore(TramTime.of(0,0))) {
            assertTrue(allDay.contains(time), "Did not contain " + time);
            time = time.plusMinutes(1);
        }
    }

    @Test
    void shouldHaveAllDayOverlaps() {
        final TimeRange allDay = TimeRange.AllDay();

        TramTime time = TramTime.of(0,1);

        while(time.isBefore(TramTime.of(23,59))) {
            TimeRange timeRange = TimeRangePartial.of(time, time.plusMinutes(1));
            assertTrue(allDay.anyOverlap(timeRange), "Did not contain " + time);
            time = time.plusMinutes(1);
        }
    }
}
