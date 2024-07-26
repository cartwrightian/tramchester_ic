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
    private final Set<String> diversionsAround;
    private final Set<String> diversionsToFrom;

    public StationClosuresConfigForTest(TramStations station, TramDate begin, TramDate end, boolean fullyClosed, Set<String> diversionsAround, Set<String> diversionsToFrom) {
        this.station = station;
        this.begin = begin;
        this.end = end;
        this.fullyClosed = fullyClosed;
        this.diversionsAround = diversionsAround;
        this.diversionsToFrom = diversionsToFrom;
    }

    public StationClosuresConfigForTest(TramStations tramStations, TramDate begin, TramDate end, boolean fullyClosed) {
        this(tramStations, begin, end, fullyClosed, null, null);
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
    public boolean hasDiversionsAroundClosure() {
        return diversionsAround!=null;
    }

    @Override
    public IdSet<Station> getDiversionsAroundClosure() {
        return diversionsAround.stream().map(Station::createId).collect(IdSet.idCollector());
    }

    @Override
    public boolean hasDiversionsToFromClosure() {
        return diversionsToFrom!=null;
    }

    @Override
    public IdSet<Station> getDiversionsToFromClosure() {
        return diversionsToFrom.stream().map(Station::createId).collect(IdSet.idCollector());
    }

    @Override
    public String toString() {
        return "StationClosuresConfigForTest{" +
                "station=" + station +
                ", begin=" + begin +
                ", end=" + end +
                ", fullyClosed=" + fullyClosed +
                ", diversionsAround=" + diversionsAround +
                ", diversionsToFrom=" + diversionsToFrom +
                '}';
    }
}