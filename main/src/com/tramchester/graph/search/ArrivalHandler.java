package com.tramchester.graph.search;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.LowestCostSeenSimple;

public interface ArrivalHandler {

    int getArrivalsLimit();

    void recordArrival(TramTime queryTime);

    enum Outcome {
        Better,
        Same,
        Worse
    }

    static ArrivalHandler get() {
        return new LowestCostSeenSimple();
        //return new LowestCostSeenForTime(1);
    }

    Outcome checkDuration(TramTime tramTime, ImmutableJourneyState journeyState);
    Outcome checkChanges(TramTime tramTime, int numberChanges);

    void setLowestCost(TramTime tramTime, ImmutableJourneyState journeyState);

    boolean alreadyLonger(TramTime tramTime, TramDuration totalCostSoFar);
    boolean alreadyMoreChanges(TramTime tramTime, int numberChanges);

    boolean overArrivalsLimit(TramTime tramTime);

}
