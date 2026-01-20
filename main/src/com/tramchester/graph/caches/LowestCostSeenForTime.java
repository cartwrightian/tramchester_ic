package com.tramchester.graph.caches;

import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.ArrivalHandler;
import com.tramchester.graph.search.ImmutableJourneyState;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LowestCostSeenForTime implements ArrivalHandler {

    private final ConcurrentMap<TramTime, TramDuration> lowestCostForQuery;
    private final ConcurrentMap<TramTime, Integer> lowestNumChangesForQuery;
    private final ConcurrentMap<TramTime, AtomicInteger> arrivalCounts;
    private TramDuration shortestEver;
    private int leastChangesEver;
    private final int arrivalsLimit;

    //private final AtomicInteger arrived;

    public LowestCostSeenForTime(int arrivalsLimit) {
        this.arrivalsLimit = arrivalsLimit;
        lowestCostForQuery = new ConcurrentHashMap<>();
        lowestNumChangesForQuery = new ConcurrentHashMap<>();
        arrivalCounts = new ConcurrentHashMap<>();
        shortestEver = TramDuration.MAX_VALUE;
        leastChangesEver = Integer.MAX_VALUE;
    }

    @Override
    public int getArrivalsLimit() {
        return arrivalsLimit;
    }

    @Override
    public void recordArrival(final TramTime queryTime) {
        arrivalCounts.computeIfAbsent(queryTime, key -> new AtomicInteger(0));
        arrivalCounts.get(queryTime).incrementAndGet();
    }

    @Override
    public boolean overArrivalsLimit(TramTime queryTime) {
        if (arrivalCounts.containsKey(queryTime)) {
            return arrivalCounts.get(queryTime).get() >= arrivalsLimit;
        }
        return false;
    }

    public Outcome checkDuration(final TramTime time, final ImmutableJourneyState journeyState) {

        final TramDuration durationForState = journeyState.getTotalDurationSoFar();

        if (lowestCostForQuery.containsKey(time)) {
            final TramDuration lowestForQuery  = lowestCostForQuery.get(time);
            if (durationForState.lessThan(lowestForQuery)) {
                return Outcome.Better;
            }
            if (durationForState.equals(lowestForQuery)) {
                return Outcome.Same;
            }

            return Outcome.Worse;

        } else {
            if (durationForState.lessThan(shortestEver)) {
                return Outcome.Better;
            } else if (durationForState.equals(shortestEver)) {
                return Outcome.Same;
            }
            // todo check against lowest ever
            //final TramDuration currentLowest = getLowestEverDuration();
            return Outcome.Worse;
        }

    }

    @Override
    public Outcome checkChanges(final TramTime time, final int numberChanges) {
        if (lowestNumChangesForQuery.containsKey(time)) {
            final int lowest = lowestNumChangesForQuery.get(time);
            if (numberChanges<lowest) {
                return Outcome.Better;
            } else if (numberChanges==lowest) {
                return Outcome.Same;
            } else {
                return Outcome.Worse;
            }
        } else {
            if (numberChanges<leastChangesEver) {
                return Outcome.Better;
            } else if (numberChanges==leastChangesEver) {
                return Outcome.Same;
            }
            return Outcome.Worse;
        }

    }

    @Override
    public boolean alreadyLonger(final TramTime time, final TramDuration totalCostSoFar) {
        if (lowestCostForQuery.containsKey(time)) {
            final TramDuration lowestCostSeen = lowestCostForQuery.get(time);
            return Durations.greaterThan(totalCostSoFar, lowestCostSeen);
        } else {
            return false;
            //return totalCostSoFar.moreThan(shortestEver);
        }
    }

    @Override
    public boolean alreadyMoreChanges(TramTime time, int numberChanges) {
        if (lowestNumChangesForQuery.containsKey(time)) {
            return numberChanges>lowestNumChangesForQuery.get(time);
        } else {
            return false;
        }
    }


    public synchronized void setLowestCost(final TramTime time, final ImmutableJourneyState journeyState) {

        int numberChanges = journeyState.getNumberChanges();
        lowestNumChangesForQuery.put(time, numberChanges);
        if (numberChanges<leastChangesEver) {
            leastChangesEver = numberChanges;
        }

        final TramDuration durationSoFar = journeyState.getTotalDurationSoFar();
        lowestCostForQuery.put(time, durationSoFar);
        if (durationSoFar.lessThan(shortestEver)) {
            shortestEver = durationSoFar;
        }
    }

    @Override
    public String toString() {
        return "LowestCostSeenForQueryTime{" +
                "cost=" + lowestCostForQuery +
                ", changes=" + lowestNumChangesForQuery +
                ", arrivalCounts=" + arrivalCounts +
                '}';
    }

}
