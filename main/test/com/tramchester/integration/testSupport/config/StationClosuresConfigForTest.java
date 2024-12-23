package com.tramchester.integration.testSupport.config;

import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.testSupport.reference.FakeStation;
import com.tramchester.testSupport.reference.TramStations;

import java.util.Objects;
import java.util.Set;

public class StationClosuresConfigForTest implements StationClosures {

    private final TramStations station;
    private final DateRange dateRange;
    private final boolean fullyClosed;
    private final Set<TramStations> diversionsAround;
    private final Set<TramStations> diversionsToFrom;
    private TimeRange timeRange;

//      - stations: [ "9400ZZMAPGD" ]
//    dateRange:
//    begin: 2024-08-28
//    end: 2024-09-16
//    fullyClosed: false
//    diversionsAroundClosure: []
//    diversionsToFromClosure: []

    public StationClosuresConfigForTest(TramStations station, DateRange dateRange, boolean fullyClosed,
                                        Set<TramStations> diversionsAround, Set<TramStations> diversionsToFrom) {
        this.station = station;
        this.dateRange = dateRange;
        this.fullyClosed = fullyClosed;
        this.diversionsAround = diversionsAround;
        this.diversionsToFrom = diversionsToFrom;
        this.timeRange = null;
    }

    public StationClosuresConfigForTest(TramStations tramStations, DateRange dateRange, boolean fullyClosed) {
        this(tramStations, dateRange, fullyClosed, null, null);
    }

    @Override
    public IdSet<Station> getStations() {
        return IdSet.singleton(station.getId());
    }

    @Override
    public boolean isFullyClosed() {
        return fullyClosed;
    }

    @Override
    public DateRange getDateRange() {
        return dateRange;
    }

    @Override
    public boolean hasTimeRange() {
        return timeRange!=null;
    }

    @Override
    public TimeRange getTimeRange() {
        return timeRange;
    }

    @Override
    public boolean hasDiversionsAroundClosure() {
        return diversionsAround!=null;
    }

    @Override
    public IdSet<Station> getDiversionsAroundClosure() {
        return diversionsAround.stream().map(FakeStation::getId).collect(IdSet.idCollector());
    }

    @Override
    public boolean hasDiversionsToFromClosure() {
        return diversionsToFrom!=null;
    }

    @Override
    public IdSet<Station> getDiversionsToFromClosure() {
        return diversionsToFrom.stream().map(FakeStation::getId).collect(IdSet.idCollector());
    }

    @Override
    public String toString() {
        return "StationClosuresConfigForTest{" +
                "station=" + station.getId() +
                ", dateRange=" + dateRange +
                ", fullyClosed=" + fullyClosed +
                ", timeRange=" + timeRange +
                ", diversionsAround=" + diversionsAround +
                ", diversionsToFrom=" + diversionsToFrom +
                '}';
    }

    @Override
    public boolean equals(Object o) {
       return StationClosures.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(station, getDateRange(), isFullyClosed(), diversionsAround, diversionsToFrom, getTimeRange());
    }

    public void setTimeRange(TimeRange timeRange) {
        this.timeRange = timeRange;
    }
}