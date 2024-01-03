package com.tramchester.graph.graphbuild;

import com.tramchester.domain.Route;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.facade.MutableGraphTransaction;

public interface TransactionStrategy {

    void routeBegin(Route route);
    void routeDone();

    void tripBegin(Trip trip);
    void tripDone();

    MutableGraphTransaction currentTxn();

}
