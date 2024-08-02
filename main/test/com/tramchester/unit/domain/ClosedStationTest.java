package com.tramchester.unit.domain;

import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Test;

import static com.tramchester.testSupport.reference.TramStations.ExchangeSquare;
import static org.junit.jupiter.api.Assertions.*;

public class ClosedStationTest {

    @Test
    void shouldHaveSameClosure() {
        Station station = ExchangeSquare.fake();
        DateRange dateRange = DateRange.of(TestEnv.testDay(), TestEnv.testDay().plusDays(3));
        ClosedStation closedStationA = new ClosedStation(station, dateRange, true, null, null);
        ClosedStation closedStationB = new ClosedStation(station, dateRange, true, null, null);

        assertEquals(closedStationA, closedStationB);
    }

    @Test
    void shouldMatchIfTimeRangesAndDateRangesSame() {
        Station station = ExchangeSquare.fake();
        DateRange dateRange = DateRange.of(TestEnv.testDay(), TestEnv.testDay().plusDays(3));
        TimeRange timeRange = TimeRange.of(TramTime.of(1,30), TramTime.of(2,30));

        ClosedStation closedStationA = new ClosedStation(station, dateRange, timeRange, true, null, null);
        ClosedStation closedStationB = new ClosedStation(station, dateRange, timeRange, true, null, null);

        assertEquals(closedStationA, closedStationB);
    }

    @Test
    void shouldNotMatchIfTimeRangesDifferent() {
        Station station = ExchangeSquare.fake();
        DateRange dateRange = DateRange.of(TestEnv.testDay(), TestEnv.testDay().plusDays(3));
        TimeRange timeRangeA = TimeRange.of(TramTime.of(1,30), TramTime.of(2,30));
        TimeRange timeRangeB = TimeRange.of(TramTime.of(15,30), TramTime.of(22,30));

        ClosedStation closedStationA = new ClosedStation(station, dateRange, timeRangeA, true, null, null);
        ClosedStation closedStationB = new ClosedStation(station, dateRange, timeRangeB, true, null, null);

        assertNotEquals(closedStationA, closedStationB);
    }

    @Test
    void shouldNotBeSameClosureIfDatesDontOverlap() {
        Station station = ExchangeSquare.fake();
        DateRange dateRangeA = DateRange.of(TestEnv.testDay(), TestEnv.testDay().plusDays(3));
        DateRange dateRangeB = DateRange.of(TestEnv.testDay().plusWeeks(1), TestEnv.testDay().plusWeeks(2));

        ClosedStation closedStationA = new ClosedStation(station, dateRangeA, true, null, null);
        ClosedStation closedStationB = new ClosedStation(station, dateRangeB, true, null, null);

        assertNotEquals(closedStationA, closedStationB);
    }
}
