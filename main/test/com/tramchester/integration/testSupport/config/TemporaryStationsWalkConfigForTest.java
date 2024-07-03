package com.tramchester.integration.testSupport.config;

import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.TemporaryStationsWalk;
import com.tramchester.domain.dates.DateRange;

import java.util.Objects;

public class TemporaryStationsWalkConfigForTest implements TemporaryStationsWalk {

    private final StationIdPair stations;
    private final DateRange dateRange;

    public TemporaryStationsWalkConfigForTest(StationIdPair stations, DateRange dateRange) {

        this.stations = stations;
        this.dateRange = dateRange;
    }

    @Override
    public DateRange getDateRange() {
        return dateRange;
    }

    @Override
    public StationIdPair getStationPair() {
        return stations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemporaryStationsWalkConfigForTest that = (TemporaryStationsWalkConfigForTest) o;
        return Objects.equals(stations, that.stations) && Objects.equals(getDateRange(), that.getDateRange());
    }

    @Override
    public int hashCode() {
        return Objects.hash(stations, getDateRange());
    }
}
