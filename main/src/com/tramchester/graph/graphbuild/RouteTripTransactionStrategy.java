package com.tramchester.graph.graphbuild;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class RouteTripTransactionStrategy implements TransactionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(RouteTripTransactionStrategy.class);

    private final GraphDatabase graphDatabase;
    private final boolean eachTrip;
    private final int numTrips;

    private MutableGraphTransaction transaction;
    private Instant startRoute;
    private IdFor<Route> routeId;

    public RouteTripTransactionStrategy(GraphDatabase graphDatabase, TramchesterConfig config, int numTrips) {
        this.graphDatabase = graphDatabase;
        if (config.getTransportModes().contains(TransportMode.Bus)) {
            eachTrip = (numTrips>800);
        } else {
            eachTrip = false;
        }
        this.numTrips = numTrips;
    }

    @Override
    public void routeBegin(Route route) {
        startRoute  = Instant.now();
        routeId = route.getId();
        if (!eachTrip) {
            transaction = graphDatabase.beginTxMutable();
        }
    }

    @Override
    public void routeDone() {
        if (!eachTrip) {
            transaction.commit();
        }
        Instant finish = Instant.now();
        logger.info(String.format("TIMING: %s TOOK: %s ms (Commit per trip: %s, number of trips %s)",
                routeId, Duration.between(startRoute, finish).toMillis(), eachTrip, numTrips));
    }

    @Override
    public void tripBegin(Trip trip) {
        if (eachTrip) {
            transaction = graphDatabase.beginTxMutable();;
        }
    }

    @Override
    public void tripDone() {
        if (eachTrip) {
            transaction.commit();
        }
    }

    @Override
    public MutableGraphTransaction currentTxn() {
        return transaction;
    }
}
