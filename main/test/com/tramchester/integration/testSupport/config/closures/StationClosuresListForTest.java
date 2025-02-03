package com.tramchester.integration.testSupport.config.closures;

import com.tramchester.config.StationListConfig;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.testSupport.reference.TramStations;

import java.util.Collections;
import java.util.Set;

public class StationClosuresListForTest extends StationClosuresTestConfig {

    private final TramStations station;

//      - stations: [ "9400ZZMAPGD" ]
//    dateRange:
//    begin: 2024-08-28
//    end: 2024-09-16
//    fullyClosed: false
//    diversionsAroundClosure: []
//    diversionsToFromClosure: []

    public StationClosuresListForTest(TramStations station, DateRange dateRange, boolean fullyClosed,
                                      Set<TramStations> diversionsAround, Set<TramStations> diversionsToFrom) {
        super(dateRange, fullyClosed, diversionsAround, diversionsToFrom);
        this.station = station;
    }

    public StationClosuresListForTest(TramStations tramStations, DateRange dateRange, boolean fullyClosed) {
        this(tramStations, dateRange, fullyClosed, null, null);
    }

    @Override
    public StationListConfig getStations() {
        return new StationListConfig(Collections.singleton(station.getRawId()));
    }

    @Override
    public String toString() {
        return "StationClosuresConfigForTest{" +
                "station=" + station.getId() +
                '}';
    }

}