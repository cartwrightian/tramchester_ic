package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;

public interface HasTowardsStationId {
    IdFor<Station> getTowards();
}
