package com.tramchester.graph.caches;

import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.ImmutableJourneyState;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class LowestCostSeenSimple implements com.tramchester.graph.search.ArrivalHandler {
    private final AtomicReference<TramDuration> lowestCost;
    private final AtomicInteger lowestNumChanges;
    private final AtomicInteger arrived;

    public LowestCostSeenSimple() {
        lowestCost = new AtomicReference<>(TramDuration.ofMinutes(Integer.MAX_VALUE));
        lowestNumChanges = new AtomicInteger(Integer.MAX_VALUE);
        arrived = new AtomicInteger(0);
    }

    private TramDuration getLowestDuration() {
        return lowestCost.get();
    }

    @Override
    public int getLowestNumChanges() {
        return lowestNumChanges.get();
    }

    private boolean everArrived() {
        return arrived.get()>0;
    }

    @Override
    public boolean isLower(final TramTime queryTime, final ImmutableJourneyState journeyState) {
        // <= equals so we include multiple options and routes in the results
        // An alternative to this would be to search over a finer grained list of times and catch alternatives
        // that way

        final boolean durationLower = journeyState.getTotalDurationSoFar().compareTo(getLowestDuration()) <= 0;
        return  durationLower && journeyState.getNumberChanges() <= getLowestNumChanges();

    }

    @Override
    public boolean alreadyLonger(final TramDuration totalCostSoFar) {
        if (everArrived()) { // Not arrived for current journey, but we have seen at least one prior success
            final TramDuration lowestCostSeen = getLowestDuration();
            return Durations.greaterThan(totalCostSoFar, lowestCostSeen);
        }
        return false;
    }

    @Override
    public synchronized void setLowestCost(TramTime queryTime, final ImmutableJourneyState journeyState) {
        arrived.incrementAndGet();
        lowestNumChanges.getAndSet(journeyState.getNumberChanges());
        lowestCost.getAndSet(journeyState.getTotalDurationSoFar());
    }

    @Override
    public String toString() {
        return "LowestCostSeen{" +
                "cost=" + lowestCost +
                ", changes=" + lowestNumChanges +
                ", arrived=" + arrived +
                '}';
    }

}
