package com.tramchester.unit.domain;

import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangeAllDay;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Test;

import static com.tramchester.testSupport.reference.TramStations.ExchangeSquare;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ClosedStationTest {

    private final TramDate when = TestEnv.testDay();

    @Test
    void shouldCreateWithAllDayTimeRangeWhenNoneProvided() {
        Station station = ExchangeSquare.fake();
        DateRange dateRange = DateRange.of(when, when.plusDays(3));
        ClosedStation closedStation = new ClosedStation(station, dateRange, true, null, null);

        assertTrue(closedStation.getDateTimeRange().contains(when, TramTime.of(0,1)));
        assertTrue(closedStation.getDateTimeRange().contains(when, TramTime.of(23,59)));

    }

    @Test
    void shouldCreateWithTimeRange() {
        Station station = ExchangeSquare.fake();
        DateRange dateRange = DateRange.of(when, when.plusDays(3));
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(9,45), TramTime.of(14,13));
        ClosedStation closedStation = new ClosedStation(station, dateRange, timeRange, true, null, null);

        assertFalse(closedStation.getDateTimeRange().contains(when, TramTime.of(0,1)));
        assertFalse(closedStation.getDateTimeRange().contains(when, TramTime.of(23,59)));
        assertTrue(closedStation.getDateTimeRange().contains(when, TramTime.of(13,18)));

    }

    @Test
    void shouldMatchIfDateRangesAndTimesOverlap() {
        Station station = ExchangeSquare.fake();
        DateRange dateRangeA = DateRange.of(when, when.plusWeeks(1));
        DateRange dateRangeB = DateRange.of(when.plusDays(5), when.plusWeeks(2));
        TimeRange timeRangeA = TimeRangePartial.of(TramTime.of(10,30), TramTime.of(16,30));
        TimeRange timeRangeB = TimeRangePartial.of(TramTime.of(15,30), TramTime.of(22,30));

        ClosedStation closedStationA = new ClosedStation(station, dateRangeA, timeRangeA,true, null, null);
        ClosedStation closedStationB = new ClosedStation(station, dateRangeB, timeRangeB,true, null, null);

        assertNotEquals(closedStationA, closedStationB);
        assertTrue(closedStationA.overlaps(closedStationB));
    }

    @Test
    void shouldNotMatchIfTimeRangesDifferent() {
        Station station = ExchangeSquare.fake();
        DateRange dateRange = DateRange.of(when, when.plusDays(3));
        TimeRange timeRangeA = TimeRangePartial.of(TramTime.of(1,30), TramTime.of(2,30));
        TimeRange timeRangeB = TimeRangePartial.of(TramTime.of(15,30), TramTime.of(22,30));

        ClosedStation closedStationA = new ClosedStation(station, dateRange, timeRangeA, true, null, null);
        ClosedStation closedStationB = new ClosedStation(station, dateRange, timeRangeB, true, null, null);

        assertNotEquals(closedStationA, closedStationB);
        assertFalse(closedStationA.overlaps(closedStationB));
    }

    @Test
    void shouldNotBeSameClosureIfDatesDontOverlap() {
        Station station = ExchangeSquare.fake();
        DateRange dateRangeA = DateRange.of(when, when.plusDays(3));
        DateRange dateRangeB = DateRange.of(when.plusWeeks(1), when.plusWeeks(2));

        ClosedStation closedStationA = new ClosedStation(station, dateRangeA, true, null, null);
        ClosedStation closedStationB = new ClosedStation(station, dateRangeB, true, null, null);

        assertNotEquals(closedStationA, closedStationB);
        assertFalse(closedStationA.overlaps(closedStationB));
    }


}
