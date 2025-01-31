package com.tramchester.unit.domain;

import com.tramchester.domain.closures.ClosedStation;
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

public class ClosedStationTest {

    private final TramDate when = TestEnv.testDay();

    @Test
    void shouldCreateWithAllDayTimeRangeWhenNoneProvided() {
        Station station = ExchangeSquare.fake();
        DateRange dateRange = DateRange.of(when, when.plusDays(3));
        ClosedStation closedStation = new ClosedStation(station, dateRange, null, null);

        assertTrue(closedStation.getDateTimeRange().contains(when, TramTime.of(0,1)));
        assertTrue(closedStation.getDateTimeRange().contains(when, TramTime.of(23,59)));

    }

    @Test
    void shouldCreateWithTimeRange() {
        Station station = ExchangeSquare.fake();
        DateRange dateRange = DateRange.of(when, when.plusDays(3));
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(9,45), TramTime.of(14,13));
        ClosedStation closedStation = new ClosedStation(station, dateRange, timeRange, null,
                null);

        assertFalse(closedStation.getDateTimeRange().contains(when, TramTime.of(0,1)));
        assertFalse(closedStation.getDateTimeRange().contains(when, TramTime.of(23,59)));
        assertTrue(closedStation.getDateTimeRange().contains(when, TramTime.of(13,18)));

    }
}
