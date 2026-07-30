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

public class LowestCostSeenForTimeAlternative implements ArrivalHandler {
    private static final Logger logger = LoggerFactory.getLogger(LowestCostSeenForTimeAlternative.class);

    private final ConcurrentMap<TramTime, TramDuration> lowestCostForFirstBoardTime;
    private final ConcurrentMap<TramTime, Integer> lowestChangesForFirstBoardTime;
    private final ConcurrentMap<TramTime, AtomicInteger> arrivalCounts;
    private TramTime earliestArrival;
    private TramTime earliestEverBoardingTime;
    private TramDuration shortestEver;
    private int leastChangesEver;
    private final int arrivalsLimit; // for a specific time

    public LowestCostSeenForTimeAlternative(int arrivalsLimit) {
        this.arrivalsLimit = arrivalsLimit;
        lowestCostForFirstBoardTime = new ConcurrentHashMap<>();
        lowestChangesForFirstBoardTime = new ConcurrentHashMap<>();
        arrivalCounts = new ConcurrentHashMap<>();
        shortestEver = TramDuration.MAX_VALUE;
        earliestEverBoardingTime = TramTime.invalid();
        earliestArrival = TramTime.invalid();
        leastChangesEver = Integer.MAX_VALUE;
    }

    @Override
    public int getArrivalsLimit() {
        return arrivalsLimit;
    }

    @Override
    public void recordArrival(final ImmutableJourneyState journeyState) {
        final TramTime arrivalTime = journeyState.getJourneyClock();
        arrivalCounts.computeIfAbsent(arrivalTime, key -> new AtomicInteger(0));
        arrivalCounts.get(arrivalTime).incrementAndGet();
    }

    private static TramTime firstBoardTime(final ImmutableJourneyState journeyState) {
        return journeyState.getFirstBoardTime();
    }

    @Override
    public boolean overArrivalsLimit(final ImmutableJourneyState journeyState) {
        final TramTime firstBoardTime = firstBoardTime(journeyState);
        final boolean result;
        if (arrivalCounts.containsKey(firstBoardTime)) {
            result = arrivalCounts.get(firstBoardTime).get() >= arrivalsLimit;
        } else {
            result = false;
        }
        // TODO into debug, but for now: why is this called so often?!
        if (result) {
            logger.info("Seen " + firstBoardTime + " too many times before");
        }
        return result;
    }

    @Override
    public Outcome checkDuration(final ImmutableJourneyState journeyState) {

        final TramDuration durationForState = journeyState.getTotalDurationSoFar();
        final TramTime earliestBoarding = firstBoardTime(journeyState);

        final Outcome result;
        if (lowestCostForFirstBoardTime.containsKey(earliestBoarding)) {
            final TramDuration lowestForQuery  = lowestCostForFirstBoardTime.get(earliestBoarding);
            if (durationForState.lessThan(lowestForQuery)) {
                result = Outcome.Better;
            } else if (durationForState.equals(lowestForQuery)) {
                result = Outcome.Same;
            }  else {
                result = Outcome.Worse;
            }
            logger.info("Check duration for boarding time, seen " + earliestBoarding + " before, result = " + result);
        } else {
            // never had a journey for this boarding time
            final TramTime time = journeyState.getFirstBoardTime();
            if (time.isBefore(earliestBoarding)) {
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
            logger.info("Check duration for earliest boarding time, unique " + earliestBoarding + " seen, result = " + result);
        }
        return result;
    }

    @Override
    public Outcome checkChanges(final ImmutableJourneyState journeyState, final int numberChanges) {

        final TramTime firstBoardTime = firstBoardTime(journeyState);

        final Outcome result;
        if (lowestChangesForFirstBoardTime.containsKey(firstBoardTime)) {
            final int lowest = lowestChangesForFirstBoardTime.get(firstBoardTime);
            if (numberChanges<lowest) {
                result = Outcome.Better;
            } else if (numberChanges==lowest) {
                result = Outcome.Same;
            } else {
                result = Outcome.Worse;
            }
            logger.info("Check changes ("+numberChanges+"), seen " + firstBoardTime + " before, result = " + result);

        } else {
            if (numberChanges<leastChangesEver) {
                result = Outcome.Better;
            } else if (numberChanges==leastChangesEver) {
                result = Outcome.Same;
            } else {
                result = Outcome.Worse;
            }
            logger.info("Check changes ("+numberChanges+"), unique " + firstBoardTime + " seen, result = " + result);
        }

        return result;
    }

    @Override
    public boolean alreadyLonger(final ImmutableJourneyState journeyState) {
        final TramTime firstBoardTime = firstBoardTime(journeyState);
        final TramDuration totalCostSoFar = journeyState.getTotalDurationSoFar();

        if (lowestCostForFirstBoardTime.containsKey(firstBoardTime)) {
            final TramDuration lowestCostSeen = lowestCostForFirstBoardTime.get(firstBoardTime);
            return Durations.greaterThan(totalCostSoFar, lowestCostSeen);
        } else {
            return false;
        }
    }

    @Override
    public boolean alreadyMoreChanges(final ImmutableJourneyState journeyState, final int numberChanges) {
        final TramTime firstBoardTime = firstBoardTime(journeyState);
        if (lowestChangesForFirstBoardTime.containsKey(firstBoardTime)) {
            return numberChanges > lowestChangesForFirstBoardTime.get(firstBoardTime);
        } else {
            return false;
        }
    }


    @Override
    public synchronized void setLowestCost(final ImmutableJourneyState journeyState) {
        final TramTime firstBoardTime = firstBoardTime(journeyState);

        int numberChanges = journeyState.getNumberChanges();
        lowestChangesForFirstBoardTime.put(firstBoardTime, numberChanges);
        if (numberChanges<leastChangesEver) {
            leastChangesEver = numberChanges;
        }

        final TramDuration durationSoFar = journeyState.getTotalDurationSoFar();
        lowestCostForFirstBoardTime.put(firstBoardTime, durationSoFar);
        if (durationSoFar.lessThan(shortestEver)) {
            shortestEver = durationSoFar;
        }

        final TramTime firstBoarding = journeyState.getFirstBoardTime();
        if (firstBoarding.isBefore(this.earliestEverBoardingTime)) {
            earliestEverBoardingTime = firstBoarding;
        }

        final TramTime clock = journeyState.getJourneyClock();
        if (clock.isBefore(earliestArrival)) {
            earliestArrival = clock;
        }
    }

    @Override
    public String toString() {
        return "LowestCostSeenForTime{" +
                "lowestCostForFirstBoardTime=" + lowestCostForFirstBoardTime +
                ", lowestChangesForFirstBoardTime=" + lowestChangesForFirstBoardTime +
                ", arrivalCounts=" + arrivalCounts +
                ", earliestArrival=" + earliestArrival +
                ", shortestEver=" + shortestEver +
                ", leastChangesEver=" + leastChangesEver +
                ", arrivalsLimit=" + arrivalsLimit +
                '}';
    }
}
