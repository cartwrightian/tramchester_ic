package com.tramchester.graph.search;

import com.tramchester.graph.caches.LowestCostSeenForTime;

public interface ArrivalHandler {

    enum Outcome {
        Better,
        Same,
        Worse
    }

    static ArrivalHandler get() {
        // TODO Define correct number here
        //return new LowestCostSeenForTime(5);
        return new LowestCostSeenForTime(1);
    }

    Outcome checkDuration(ImmutableJourneyState journeyState);
    Outcome checkChanges(ImmutableJourneyState journeyState, int numberChanges);

    void setLowestCost(ImmutableJourneyState journeyState);

    boolean alreadyLonger(ImmutableJourneyState journeyState);
    boolean alreadyMoreChanges(ImmutableJourneyState journeyState, int numberChanges);

    // arrivals tracking
    int getArrivalsLimit();
    void recordArrival(ImmutableJourneyState journeyState);
    boolean overArrivalsLimit(ImmutableJourneyState journeyState);

}
