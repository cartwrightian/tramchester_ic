package com.tramchester.integration.testSupport.config.closures;

import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.testSupport.reference.FakeStation;
import com.tramchester.testSupport.reference.TramStations;

import java.util.Set;

public abstract class StationClosuresTestConfig implements StationClosures  {
    private final DateRange dateRange;
    private final boolean fullyClosed;
    private final Set<TramStations> diversionsAround;
    private final Set<TramStations> diversionsToFrom;
    private TimeRange timeRange;

    protected StationClosuresTestConfig(DateRange dateRange, boolean fullyClosed, Set<TramStations> diversionsAround,
                                        Set<TramStations> diversionsToFrom) {
        this.dateRange = dateRange;
        this.fullyClosed = fullyClosed;
        this.diversionsAround = diversionsAround;
        this.diversionsToFrom = diversionsToFrom;
        this.timeRange = null;
    }

    public void setTimeRange(TimeRange timeRange) {
        this.timeRange = timeRange;
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

}
