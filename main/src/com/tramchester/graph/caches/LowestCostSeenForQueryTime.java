package com.tramchester.graph.caches;

import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.ArrivalHandler;
import com.tramchester.graph.search.ImmutableJourneyState;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LowestCostSeenForQueryTime implements ArrivalHandler {
//    private final AtomicReference<TramDuration> lowestCost;
//    private final AtomicInteger lowestNumChanges;
    private final ConcurrentMap<TramTime, TramDuration> lowestCostForQuery; // for a query time
    private final ConcurrentMap<TramTime, Integer> lowestNumChangesForQuery; // for a query time

    private final AtomicInteger arrived;

    public LowestCostSeenForQueryTime() {
        lowestCostForQuery = new ConcurrentHashMap<>(); //new AtomicReference<>(TramDuration.ofMinutes(Integer.MAX_VALUE));
        lowestNumChangesForQuery = new ConcurrentHashMap<>(); //new AtomicInteger(Integer.MAX_VALUE);
//        lowestCost = new AtomicReference<>(TramDuration.MAX_VALUE);
        arrived = new AtomicInteger(0);
    }

    private TramDuration getLowestDuration() {
        return lowestCostForQuery.values().stream().min(TramDuration::compareTo).orElse(TramDuration.MAX_VALUE);
    }

    public int getLowestNumChanges() {
        return lowestNumChangesForQuery.values().stream().min(Integer::compareTo).orElse(Integer.MAX_VALUE);
    }

    private boolean everArrived() {
        return arrived.get()>0;
    }

    public boolean isLower(final TramTime queryTime, final ImmutableJourneyState journeyState) {
        final TramDuration durationForState = journeyState.getTotalDurationSoFar();
        final TramDuration currentLowest = getLowestDuration();

        final boolean lowerCost;
        final boolean sameCost;
        if (lowestCostForQuery.containsKey(queryTime)) {
            final TramDuration lowestSoFar  = this.lowestCostForQuery.get(queryTime);
            lowerCost = durationForState.lessThan(lowestSoFar);
            sameCost = (!lowerCost) && durationForState.equals(lowestSoFar);
        } else {
            lowerCost = durationForState.lessThan(currentLowest);
            sameCost = false;
        }

        if (lowerCost) {
            return true;
        }

        if (sameCost) {
            final boolean fewerChanges;
            if (lowestCostForQuery.containsKey(queryTime)) {
                final int lowestChangesForQuery  = lowestNumChangesForQuery.get(queryTime);
                fewerChanges = journeyState.getNumberChanges() <= lowestChangesForQuery;
            } else {
                fewerChanges = true;
            }

            return fewerChanges;
        }

        return false;


//        if (journeyState.getTotalDurationSoFar().compareTo(lowestCostForQuery) >= 0) {
//            // longer
//            return false;
//        }

//        final boolean durationLower = journeyState.getTotalDurationSoFar().compareTo(getLowestDuration()) <= 0;
//        return  durationLower && journeyState.getNumberChanges() <= getLowestNumChanges();

    }

    @Override
    public boolean alreadyLonger(final TramDuration totalCostSoFar) {
        if (everArrived()) { // Not arrived for current journey, but we have seen at least one prior success
            final TramDuration lowestCostSeen = getLowestDuration();
            return Durations.greaterThan(totalCostSoFar, lowestCostSeen);
        }
        return false;
    }

    public synchronized void setLowestCost(final TramTime queryTime, final ImmutableJourneyState journeyState) {
        arrived.incrementAndGet();
        lowestNumChangesForQuery.put(queryTime, journeyState.getNumberChanges());
        lowestCostForQuery.put(queryTime, journeyState.getTotalDurationSoFar());
//        lowestNumChanges.getAndSet(journeyState.getNumberChanges());
//        lowestCost.getAndSet(journeyState.getTotalDurationSoFar());
    }

    @Override
    public String toString() {
        return "LowestCostSeen{" +
                "cost=" + lowestCostForQuery +
                ", changes=" + lowestNumChangesForQuery +
                ", arrived=" + arrived +
                '}';
    }

}
