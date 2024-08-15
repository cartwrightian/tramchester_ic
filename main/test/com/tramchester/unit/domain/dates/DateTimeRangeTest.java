package com.tramchester.unit.domain.dates;

import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.testSupport.reference.TramStations.ExchangeSquare;
import static org.junit.jupiter.api.Assertions.*;

public class DateTimeRangeTest {

    private TramDate when;
    private TramDate weekAfter;

    @BeforeEach
    void onceBeforeEachTest() {
        when = TestEnv.testDay();
        weekAfter = when.plusWeeks(1);
    }

    @Test
    public void shouldHaveUseDatesIfAllDay() {

        DateTimeRange range = new DateTimeRange(when, weekAfter);
        assertTrue(range.contains(when, TramTime.of(13,45)));

    }

    @Test
    public void shouldUseTimeRangeIfPresent() {
        DateTimeRange range = new DateTimeRange(when, weekAfter, TramTime.of(13,55), TramTime.of(17,45));
        assertTrue(range.contains(when, TramTime.of(14,59)));
        assertFalse(range.contains(when, TramTime.of(13,50)));
        assertFalse(range.contains(when, TramTime.of(17,50)));

    }

    @Test
    public void shouldIgnoreTimeRangeIfNoInDateRange() {
        DateTimeRange range = new DateTimeRange(when, weekAfter, TramTime.of(13,55), TramTime.of(17,45));
        assertFalse(range.contains(when.minusDays(1), TramTime.of(13,50)));
        assertFalse(range.contains(weekAfter.plusDays(1), TramTime.of(13,50)));

    }

    @Test
    public void shouldHaveEquality() {
        DateTimeRange rangeA = new DateTimeRange(when, weekAfter, TramTime.of(13,55), TramTime.of(17,45));
        DateTimeRange rangeB = new DateTimeRange(when, weekAfter, TramTime.of(13,55), TramTime.of(17,45));

        assertEquals(rangeA, rangeB);
        assertEquals(rangeB, rangeA);

        DateTimeRange rangeC = new DateTimeRange(when.minusDays(1), weekAfter, TramTime.of(13,55), TramTime.of(17,45));
        DateTimeRange rangeD = new DateTimeRange(when, weekAfter, TramTime.of(13,56), TramTime.of(17,45));

        assertNotEquals(rangeA, rangeC);
        assertNotEquals(rangeA, rangeD);

    }


    @Test
    void shouldOverlapIfSame() {
        DateTimeRange rangeA = new DateTimeRange(TestEnv.testDay(), TestEnv.testDay().plusDays(3));
        DateTimeRange rangeB = new DateTimeRange(TestEnv.testDay(), TestEnv.testDay().plusDays(3));
        assertTrue(rangeA.overlaps(rangeB));
        assertTrue(rangeB.overlaps(rangeA));
    }

    @Test
    void shouldMatchIfTimeRangesAndDateRangesSame() {
        DateRange dateRange = DateRange.of(TestEnv.testDay(), TestEnv.testDay().plusDays(3));
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(1,30), TramTime.of(2,30));

        DateTimeRange rangeA = DateTimeRange.of(dateRange, timeRange);
        DateTimeRange rangeB = DateTimeRange.of(dateRange, timeRange);

        assertTrue(rangeA.overlaps(rangeB));
        assertTrue(rangeB.overlaps(rangeA));
    }

    @Test
    void shouldMatchIfDateRangesOverlap() {

        DateTimeRange rangeA = new DateTimeRange(TestEnv.testDay(), TestEnv.testDay().plusWeeks(1));
        DateTimeRange rangeB = new DateTimeRange(TestEnv.testDay().plusDays(5), TestEnv.testDay().plusWeeks(2));

        assertTrue(rangeA.overlaps(rangeB));
        assertTrue(rangeB.overlaps(rangeA));
    }

}
