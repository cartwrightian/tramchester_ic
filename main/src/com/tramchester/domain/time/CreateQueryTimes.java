package com.tramchester.domain.time;

import com.tramchester.config.TramchesterConfig;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

public class CreateQueryTimes {
    private final TramchesterConfig config;

    @Inject
    public CreateQueryTimes(TramchesterConfig config) {
        this.config = config;
    }

//    public List<TramTime> generate(TramTime initialQueryTime, Set<StationWalk> walksAtStart) {
//
//        return generate(initialQueryTime);
//
//    }

    public List<TramTime> generate(final TramTime initialQueryTime) {
        List<TramTime> result = new ArrayList<>();

        final int interval = config.getQueryInterval();
        final int numberQueries = config.getNumberQueries();

        int minsToAdd = 0;
        for (int i = 0; i < numberQueries; i++) {
            result.add(initialQueryTime.plusMinutes(minsToAdd));
            minsToAdd = minsToAdd + interval;
        }

        return result;
    }
}
