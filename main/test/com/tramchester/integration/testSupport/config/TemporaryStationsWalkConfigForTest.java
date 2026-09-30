package com.tramchester.integration.testSupport.config;

import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.time.TramDuration;

import java.util.Objects;

public class TemporaryStationsWalkConfigForTest implements TemporaryStationsWalkIds {

    private final StationIdPair stations;
    private final DateRange dateRange;
    private final TramDuration costOverride;

    public TemporaryStationsWalkConfigForTest(StationIdPair stations, DateRange dateRange) {
        this(stations, dateRange, null);
    }

    public TemporaryStationsWalkConfigForTest(StationIdPair stations, DateRange dateRange, TramDuration costOverride) {
        this.stations = stations;
        this.dateRange = dateRange;
        this.costOverride = costOverride;
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
    public boolean hasCostOverride() {
        return costOverride!=null;
    }

    @Override
    public TramDuration getCostOverride() {
        return costOverride;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemporaryStationsWalkIds that)) return false;
        return TemporaryStationsWalkIds.areEqual(this, that);
        //return Objects.equals(stations, that.stations) && Objects.equals(dateRange, that.dateRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stations, dateRange);
    }

    @Override
    public String toString() {
        return TemporaryStationsWalkIds.asString(this);
//        return "TemporaryStationsWalkConfigForTest{" +
//                "stations=" + stations +
//                ", dateRange=" + dateRange +
//                '}';
    }
}
