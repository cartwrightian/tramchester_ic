package com.tramchester.graph.graphbuild;

import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.RouteRepository;

public interface GraphFilter {
    boolean isFiltered();
    boolean shouldInclude(RouteRepository routeRepository, Route route);
    boolean shouldInclude(Service service);
    boolean shouldInclude(Station station);
    boolean shouldInclude(StopCall stopCall);
    boolean shouldInclude(IdFor<Station> stationId);
    boolean shouldInclude(Agency agency);
}
