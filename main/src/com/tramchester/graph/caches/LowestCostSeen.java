package com.tramchester.graph.caches;

import com.tramchester.graph.search.ImmutableJourneyState;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class LowestCostSeen {
    private final AtomicReference<Duration> lowestCost;
    private final AtomicInteger lowestNumChanges;
    private final AtomicInteger arrived;

    public LowestCostSeen() {
        lowestCost = new AtomicReference<>(Duration.ofMinutes(Integer.MAX_VALUE));
        lowestNumChanges = new AtomicInteger(Integer.MAX_VALUE);
        arrived = new AtomicInteger(0);
    }

    public Duration getLowestDuration() {
        return lowestCost.get();
    }

    public int getLowestNumChanges() {
        return lowestNumChanges.get();
    }

    public boolean everArrived() {
        return arrived.get()>0;
    }

    public boolean isLower(final ImmutableJourneyState journeyState) {
        // <= equals so we include multiple options and routes in the results
        // An alternative to this would be to search over a finer grained list of times and catch alternatives
        // that way

        final boolean durationLower = journeyState.getTotalDurationSoFar().compareTo(getLowestDuration()) <= 0;
        return  durationLower && journeyState.getNumberChanges() <= getLowestNumChanges();

    }

    public synchronized void setLowestCost(final ImmutableJourneyState journeyState) {
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
