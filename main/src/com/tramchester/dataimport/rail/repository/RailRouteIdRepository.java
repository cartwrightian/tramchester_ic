package com.tramchester.dataimport.rail.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.places.Station;

import java.util.List;

@ImplementedBy(RailRouteIds.class)
public interface RailRouteIdRepository {
    RailRouteId getRouteIdFor(IdFor<Agency> agencyId, List<Station> callingStations);
    RailRouteIds.RailRouteCallingPointsWithRouteId find(IdFor<Agency> agencyId, IdFor<Route> routeId);
}
