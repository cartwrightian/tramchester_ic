package com.tramchester.integration.testSupport.config;

import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.testSupport.reference.TramStations;

import java.util.Set;

public class StationClosuresConfigForTest implements StationClosures {

    private final TramStations station;
    private final TramDate begin;
    private final TramDate end;
    private final boolean fullyClosed;
    private final Set<String> diversionOnly;

    public StationClosuresConfigForTest(TramStations station, TramDate begin, TramDate end, boolean fullyClosed, Set<String> diversionOnly) {
        this.station = station;
        this.begin = begin;
        this.end = end;
        this.fullyClosed = fullyClosed;
        this.diversionOnly = diversionOnly;
    }

    @Override
    public IdSet<Station> getStations() {
        return IdSet.singleton(station.getId());
    }

    @Override
    public TramDate getBegin() {
        return begin;
    }

    @Override
    public TramDate getEnd() {
        return end;
    }

    @Override
    public boolean isFullyClosed() {
        return fullyClosed;
    }

    @Override
    public DateRange getDateRange() {
        return new DateRange(begin, end);
    }

    @Override
    public IdSet<Station> getDiversionsOnly() {
        return diversionOnly.stream().map(Station::createId).collect(IdSet.idCollector());
    }
}