package com.tramchester.graph.search;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.LowestCostSeenSimple;

public interface ArrivalHandler {

    static ArrivalHandler get() {
        return new LowestCostSeenSimple();
    }

    int getLowestNumChanges();

    boolean isLower(TramTime queryTime, ImmutableJourneyState journeyState);

    void setLowestCost(TramTime queryTime, ImmutableJourneyState journeyState);

    boolean alreadyLonger(TramDuration totalCostSoFar);


}
