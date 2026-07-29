package com.tramchester.graph.caches;

import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.ArrivalHandler;
import com.tramchester.graph.search.ImmutableJourneyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LowestCostSeenForTime implements ArrivalHandler {
    private static final Logger logger = LoggerFactory.getLogger(LowestCostSeenForTime.class);

    private final ConcurrentMap<TramTime, TramDuration> lowestCostForQuery;
    private final ConcurrentMap<TramTime, Integer> lowestNumChangesForQuery;
    private final ConcurrentMap<TramTime, AtomicInteger> arrivalCounts;
    private TramTime earliestEver;
    private TramDuration shortestEver;
    private int leastChangesEver;
    private final int arrivalsLimit; // for a specific time

    public LowestCostSeenForTime(int arrivalsLimit) {
        this.arrivalsLimit = arrivalsLimit;
        lowestCostForQuery = new ConcurrentHashMap<>();
        lowestNumChangesForQuery = new ConcurrentHashMap<>();
        arrivalCounts = new ConcurrentHashMap<>();
        shortestEver = TramDuration.MAX_VALUE;
        earliestEver = TramTime.invalid();
        leastChangesEver = Integer.MAX_VALUE;
    }

    @Override
    public int getArrivalsLimit() {
        return arrivalsLimit;
    }

    @Override
    public void recordArrival(final ImmutableJourneyState journeyState) {
        final TramTime time = timeFromState(journeyState);
        arrivalCounts.computeIfAbsent(time, key -> new AtomicInteger(0));
        arrivalCounts.get(time).incrementAndGet();
    }

    private static TramTime timeFromState(final ImmutableJourneyState journeyState) {
        return journeyState.getFirstBoardTime();
    }

    @Override
    public boolean overArrivalsLimit(final ImmutableJourneyState journeyState) {
        final TramTime time = timeFromState(journeyState);
        final boolean result;
        if (arrivalCounts.containsKey(time)) {
            result = arrivalCounts.get(time).get() >= arrivalsLimit;
        } else {
            result = false;
        }
        // TODO into debug, but why is this called so often?
        if (result) {
            logger.info("Seen " + time + " too many times before");
        }
        return result;
    }

    @Override
    public Outcome checkDuration(final ImmutableJourneyState journeyState) {

        final TramDuration durationForState = journeyState.getTotalDurationSoFar();
        final TramTime time = timeFromState(journeyState);

        final Outcome result;
        if (lowestCostForQuery.containsKey(time)) {
            final TramDuration lowestForQuery  = lowestCostForQuery.get(time);
            if (durationForState.lessThan(lowestForQuery)) {
                result = Outcome.Better;
            } else if (durationForState.equals(lowestForQuery)) {
                result = Outcome.Same;
            }  else {
                result = Outcome.Worse;
            }
            logger.info("Check time, seen " + time + " before, result = " + result);
        } else {
            // never had an journey for this time
            if (time.isBefore(earliestEver)) {
                result = Outcome.Better;
            } else if (durationForState.lessThan(shortestEver)) {
                result = Outcome.Better;
            } else if (durationForState.equals(shortestEver)) {
                result = Outcome.Better;
            } else {
                // todo check against lowest ever
                //final TramDuration currentLowest = getLowestEverDuration();
                result = Outcome.Worse;
            }
            logger.info("Check time, unique " + time + " seen, result = " + result);
        }
        return result;
    }

    @Override
    public Outcome checkChanges(final ImmutableJourneyState journeyState, final int numberChanges) {

        final TramTime time = timeFromState(journeyState);

        final Outcome result;
        if (lowestNumChangesForQuery.containsKey(time)) {
            final int lowest = lowestNumChangesForQuery.get(time);
            if (numberChanges<lowest) {
                result = Outcome.Better;
            } else if (numberChanges==lowest) {
                result = Outcome.Same;
            } else {
                result = Outcome.Worse;
            }
            logger.info("Check changes ("+numberChanges+"), seen " + time + " before, result = " + result);

        } else {
            if (numberChanges<leastChangesEver) {
                result = Outcome.Better;
            } else if (numberChanges==leastChangesEver) {
                result = Outcome.Same;
            } else {
                result = Outcome.Worse;
            }
            logger.info("Check changes ("+numberChanges+"), unique " + time + " seen, result = " + result);
        }

        return result;
    }

    @Override
    public boolean alreadyLonger(final ImmutableJourneyState journeyState) {
        final TramTime time = timeFromState(journeyState);
        final TramDuration totalCostSoFar = journeyState.getTotalDurationSoFar();

        if (lowestCostForQuery.containsKey(time)) {
            final TramDuration lowestCostSeen = lowestCostForQuery.get(time);
            return Durations.greaterThan(totalCostSoFar, lowestCostSeen);
        } else {
            return false;
        }
    }

    @Override
    public boolean alreadyMoreChanges(final ImmutableJourneyState journeyState, final int numberChanges) {
        final TramTime time = timeFromState(journeyState);
        if (lowestNumChangesForQuery.containsKey(time)) {
            return numberChanges>lowestNumChangesForQuery.get(time);
        } else {
            return false;
        }
    }


    @Override
    public synchronized void setLowestCost(final ImmutableJourneyState journeyState) {
        final TramTime time = timeFromState(journeyState);

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
        if (time.isBefore(earliestEver)) {
            earliestEver = time;
        }
    }

    @Override
    public String toString() {
        return "LowestCostSeenForTime{" +
                "cost=" + lowestCostForQuery +
                ", changes=" + lowestNumChangesForQuery +
                ", arrivalCounts=" + arrivalCounts +
                '}';
    }

}
