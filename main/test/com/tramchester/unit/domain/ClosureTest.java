package com.tramchester.unit.domain;

import com.tramchester.domain.closures.Closure;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Test;

import static com.tramchester.testSupport.reference.TramStations.ExchangeSquare;
import static org.junit.jupiter.api.Assertions.*;

public class ClosureTest {
    private final TramDate when = TestEnv.testDay();

    @Test
    void shouldMatchIfDateRangesAndTimesOverlap() {
        Station station = ExchangeSquare.fake();
        DateRange dateRangeA = DateRange.of(when, when.plusWeeks(1));
        DateRange dateRangeB = DateRange.of(when.plusDays(5), when.plusWeeks(2));
        TimeRange timeRangeA = TimeRangePartial.of(TramTime.of(10,30), TramTime.of(16,30));
        TimeRange timeRangeB = TimeRangePartial.of(TramTime.of(15,30), TramTime.of(22,30));

        Closure closedStationA = new Closure(station, dateRangeA, timeRangeA, true);
        Closure closedStationB = new Closure(station, dateRangeB, timeRangeB,true);

        assertNotEquals(closedStationA, closedStationB);
        assertTrue(closedStationA.overlapsWith(closedStationB));
    }

    @Test
    void shouldNotMatchIfTimeRangesDifferent() {
        Station station = ExchangeSquare.fake();
        DateRange dateRange = DateRange.of(when, when.plusDays(3));
        TimeRange timeRangeA = TimeRangePartial.of(TramTime.of(1,30), TramTime.of(2,30));
        TimeRange timeRangeB = TimeRangePartial.of(TramTime.of(15,30), TramTime.of(22,30));

        Closure closedStationA = new Closure(station, dateRange, timeRangeA, true);
        Closure closedStationB = new Closure(station, dateRange, timeRangeB, true);

        assertNotEquals(closedStationA, closedStationB);
        assertFalse(closedStationA.overlapsWith(closedStationB));
    }

    @Test
    void shouldNotBeSameClosureIfDatesDontOverlap() {
        Station station = ExchangeSquare.fake();
        DateRange dateRangeA = DateRange.of(when, when.plusDays(3));
        DateRange dateRangeB = DateRange.of(when.plusWeeks(1), when.plusWeeks(2));

        Closure closedStationA = new Closure(station, dateRangeA, TimeRange.AllDay(),true);
        Closure closedStationB = new Closure(station, dateRangeB, TimeRange.AllDay(), true);

        assertNotEquals(closedStationA, closedStationB);
        assertFalse(closedStationA.overlapsWith(closedStationB));
    }
}
