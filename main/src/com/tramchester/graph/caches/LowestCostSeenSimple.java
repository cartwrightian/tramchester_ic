package com.tramchester.graph.caches;

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

    private int getLowestNumChanges() {
        return lowestNumChanges.get();
    }

    private boolean everArrived() {
        return arrived.get()>0;
    }

    private boolean isLower(final ImmutableJourneyState journeyState) {
        // <= equals so we include multiple options and routes in the results
        // An alternative to this would be to search over a finer grained list of times and catch alternatives
        // that way

        final boolean durationLower = journeyState.getTotalDurationSoFar().compareTo(getLowestDuration()) <= 0;
        return  durationLower && journeyState.getNumberChanges() <= getLowestNumChanges();
    }


    @Override
    public String toString() {
        return "LowestCostSeen{" +
                "cost=" + lowestCost +
                ", changes=" + lowestNumChanges +
                ", arrived=" + arrived +
                '}';
    }

    @Override
    public int getArrivalsLimit() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void recordArrival(TramTime queryTime) {
        // no-op
    }

    @Override
    public Outcome checkDuration(TramTime tramTime, ImmutableJourneyState journeyState) {
        boolean existingfunctionallity = isLower(journeyState);
        if (existingfunctionallity) {
            return Outcome.Better;
        } else {
            return Outcome.Worse;
        }
    }

    @Override
    public Outcome checkChanges(TramTime tramTime, int numberChanges) {
        if (numberChanges<lowestNumChanges.get()) {
            return Outcome.Better;
        } else {
            return Outcome.Worse;
        }
    }

    @Override
    public synchronized void setLowestCost(TramTime tramTime, final ImmutableJourneyState journeyState) {
        arrived.incrementAndGet();
        lowestNumChanges.getAndSet(journeyState.getNumberChanges());
        lowestCost.getAndSet(journeyState.getTotalDurationSoFar());
    }

    @Override
    public boolean alreadyLonger(TramTime tramTime, TramDuration totalCostSoFar) {
        return totalCostSoFar.moreThan(lowestCost.get());
    }

    @Override
    public boolean alreadyMoreChanges(TramTime tramTime, int numberChanges) {
        return false;
    }

    @Override
    public boolean overArrivalsLimit(TramTime tramTime) {
        return false;
    }
}
