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
    private final ConcurrentMap<TramTime, AtomicInteger> boardingTimes;
    private TramTime earliestBoarding;
    private TramDuration shortestEver;
    private int leastChangesEver;
    private final int arrivalsLimit; // for a specific time

    public LowestCostSeenForTime(final int arrivalsLimit) {
        this.arrivalsLimit = arrivalsLimit;

        lowestCostForQuery = new ConcurrentHashMap<>();
        lowestNumChangesForQuery = new ConcurrentHashMap<>();
        boardingTimes = new ConcurrentHashMap<>();
        shortestEver = TramDuration.getMax();
        earliestBoarding = TramTime.invalid();
        leastChangesEver = Integer.MAX_VALUE;
    }

    @Override
    public int getArrivalsLimit() {
        return arrivalsLimit;
    }

    @Override
    public void recordArrival(final ImmutableJourneyState journeyState) {
        final TramTime earliestBoard = earliestBoardFrom(journeyState);
        boardingTimes.computeIfAbsent(earliestBoard, key -> new AtomicInteger(0));
        boardingTimes.get(earliestBoard).incrementAndGet();
    }

    private static TramTime earliestBoardFrom(final ImmutableJourneyState journeyState) {
        return journeyState.getFirstBoardTime();
    }

    @Override
    public boolean overArrivalsLimit(final ImmutableJourneyState journeyState) {
        final TramTime earliestBoard = earliestBoardFrom(journeyState);
        final boolean result;
        if (boardingTimes.containsKey(earliestBoard)) {
            result = boardingTimes.get(earliestBoard).get() >= arrivalsLimit;
        } else {
            result = false;
        }
        // TODO into debug, but why is this called so often?
        if (result) {
            logger.info("Seen " + earliestBoard + " too many times before");
        }
        return result;
    }

    @Override
    public Outcome checkDuration(final ImmutableJourneyState journeyState) {

        final TramDuration durationForState = journeyState.getTotalDurationSoFar();
        final TramTime earliestBoard = earliestBoardFrom(journeyState);

        final Outcome result;
        if (lowestCostForQuery.containsKey(earliestBoard)) {
            final TramDuration lowestForQuery  = lowestCostForQuery.get(earliestBoard);
            if (durationForState.lessThan(lowestForQuery)) {
                result = Outcome.Better;
            } else if (durationForState.equals(lowestForQuery)) {
                result = Outcome.Same;
            }  else {
                result = Outcome.Worse;
            }
            logger.info("Check time, seen " + earliestBoard + " before, result = " + result);
        } else {
            // never had an journey for this time
            if (earliestBoard.isBefore(earliestBoarding)) {
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
            logger.info("Check time, unique " + earliestBoard + " seen, result = " + result);
        }
        return result;
    }

    @Override
    public Outcome checkChanges(final ImmutableJourneyState journeyState, final int numberChanges) {

        final TramTime earliestBoard = earliestBoardFrom(journeyState);

        final Outcome result;
        if (lowestNumChangesForQuery.containsKey(earliestBoard)) {
            final int lowest = lowestNumChangesForQuery.get(earliestBoard);
            if (numberChanges<lowest) {
                result = Outcome.Better;
            } else if (numberChanges==lowest) {
                result = Outcome.Same;
            } else {
                result = Outcome.Worse;
            }
            logger.info("Check changes ("+numberChanges+"), seen " + earliestBoard + " before, result = " + result);

        } else {
            if (numberChanges<leastChangesEver) {
                result = Outcome.Better;
            } else if (numberChanges==leastChangesEver) {
                result = Outcome.Same;
            } else {
                result = Outcome.Worse;
            }
            logger.info("Check changes ("+numberChanges+"), unique " + earliestBoard + " seen, result = " + result);
        }

        return result;
    }

    @Override
    public boolean alreadyLonger(final ImmutableJourneyState journeyState) {
        final TramTime earliestBoard = earliestBoardFrom(journeyState);
        final TramDuration totalCostSoFar = journeyState.getTotalDurationSoFar();

        if (lowestCostForQuery.containsKey(earliestBoard)) {
            final TramDuration lowestCostSeen = lowestCostForQuery.get(earliestBoard);
            return Durations.greaterThan(totalCostSoFar, lowestCostSeen);
        } else {
            return false;
        }
    }

    @Override
    public boolean alreadyMoreChanges(final ImmutableJourneyState journeyState, final int numberChanges) {
        final TramTime earliestBoard = earliestBoardFrom(journeyState);
        if (lowestNumChangesForQuery.containsKey(earliestBoard)) {
            return numberChanges>lowestNumChangesForQuery.get(earliestBoard);
        } else {
            return false;
        }
    }


    @Override
    public synchronized void setLowestCost(final ImmutableJourneyState journeyState) {
        final TramTime earliestBoard = earliestBoardFrom(journeyState);

        final int numberChanges = journeyState.getNumberChanges();
        lowestNumChangesForQuery.put(earliestBoard, numberChanges);
        if (numberChanges<leastChangesEver) {
            leastChangesEver = numberChanges;
        }

        final TramDuration durationSoFar = journeyState.getTotalDurationSoFar();
        lowestCostForQuery.put(earliestBoard, durationSoFar);
        if (durationSoFar.lessThan(shortestEver)) {
            shortestEver = durationSoFar;
        }
        if (earliestBoard.isBefore(earliestBoarding)) {
            earliestBoarding = earliestBoard;
        }
    }

    @Override
    public String toString() {
        return "LowestCostSeenForTime{" +
                "lowestCostForQuery=" + lowestCostForQuery +
                ", lowestNumChangesForQuery=" + lowestNumChangesForQuery +
                ", boardingTimes=" + boardingTimes +
                ", earliestBoarding=" + earliestBoarding +
                ", shortestEver=" + shortestEver +
                ", leastChangesEver=" + leastChangesEver +
                ", arrivalsLimit=" + arrivalsLimit +
                '}';
    }
}
