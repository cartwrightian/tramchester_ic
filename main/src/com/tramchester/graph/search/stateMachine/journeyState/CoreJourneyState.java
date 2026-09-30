package com.tramchester.graph.search.stateMachine.journeyState;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;

public interface CoreJourneyState {
    IdFor<Trip> getCurrentTrip();
}
