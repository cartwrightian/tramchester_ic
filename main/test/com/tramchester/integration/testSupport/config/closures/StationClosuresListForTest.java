package com.tramchester.integration.testSupport.config.closures;

import com.tramchester.config.StationListConfig;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.HasId;
import com.tramchester.testSupport.reference.TramStations;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class StationClosuresListForTest extends StationClosuresTestConfig {

    private final Set<TramStations> stations;

    /**
    stationClosures:
        - stations:
            ids: [ "9400ZZMASHU", "9400ZZMAMKT" ]
        dateRange:
            begin: 2025-03-25
            end: 2025-04-24
        fullyClosed: false
        diversionsAroundClosure: [ ]
        diversionsToFromClosure: [ ]
     **/

    public StationClosuresListForTest(Collection<TramStations> stations, DateRange dateRange, boolean fullyClosed,
                                      Set<TramStations> diversionsAround, Set<TramStations> diversionsToFrom) {
        super(dateRange, fullyClosed, diversionsAround, diversionsToFrom);
        this.stations = new HashSet<>(stations);
    }

    public StationClosuresListForTest(TramStations tramStation, DateRange dateRange, boolean fullyClosed,
                                      Set<TramStations> diversionsAround, Set<TramStations> diversionsToFrom) {
        this(Collections.singleton(tramStation), dateRange, fullyClosed, diversionsAround, diversionsToFrom);
    }

    public StationClosuresListForTest(Collection<TramStations> stations, DateRange dateRange, boolean fullyClosed) {
        this(stations, dateRange, fullyClosed, null, null);
    }

    public StationClosuresListForTest(TramStations tramStation, DateRange dateRange, boolean fullyClosed) {
        this(Collections.singleton(tramStation), dateRange, fullyClosed, null, null);
    }

    @Override
    public StationListConfig getStations() {
        final Set<String> textIds = stations.stream().map(TramStations::getRawId).collect(Collectors.toSet());
        return new StationListConfig(textIds);
    }

    @Override
    public String toString() {
        return "StationClosuresConfigForTest{" +
                "stations=" + HasId.asIds(stations) +
                super.toString() +
                '}';
    }

}