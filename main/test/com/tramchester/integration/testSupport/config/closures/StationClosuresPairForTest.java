package com.tramchester.integration.testSupport.config.closures;

import com.tramchester.config.StationPairConfig;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.testSupport.reference.TramStations;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class StationClosuresPairForTest extends StationClosuresTestConfig {

    private final Pair<TramStations, TramStations> pair;

//    dateRange:
//    begin: 2024-08-28
//    end: 2024-09-16
//    fullyClosed: false
//    diversionsAroundClosure: []
//    diversionsToFromClosure: []

    public StationClosuresPairForTest(Pair<TramStations, TramStations> pair, DateRange dateRange, boolean fullyClosed,
                                      Set<TramStations> diversionsAround, Set<TramStations> diversionsToFrom) {
        super(dateRange, fullyClosed, diversionsAround, diversionsToFrom);
        this.pair = pair;
    }

    @Override
    public StationPairConfig getStations() {
        return new StationPairConfig(pair.getLeft().getRawId(), pair.getRight().getRawId());
    }

    @Override
    public String toString() {
        return "StationClosuresConfigForTest{" +
                "pair=" + pair.toString() +
                '}';
    }

}