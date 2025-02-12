package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.reference.TransportMode;

public interface TestRoute {
    TransportMode mode();
    String shortName();
    IdFor<Route> getId();
    IdForDTO dtoId();
    Route fake();
    String name();
}
