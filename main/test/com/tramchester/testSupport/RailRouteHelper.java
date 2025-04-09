package com.tramchester.testSupport;

import com.tramchester.ComponentContainer;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.dataimport.rail.repository.RailRouteIds;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.RouteRepository;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RailRouteHelper {
    private final RailRouteIds railRouteRepository;
    private final RouteRepository routeRepository;

    public RailRouteHelper(ComponentContainer componentContainer) {
        this.railRouteRepository = componentContainer.get(RailRouteIds.class);
        this.routeRepository = componentContainer.get(RouteRepository.class);
    }

    public RailRouteId getRouteId(TrainOperatingCompanies operatingCompany, RailStationIds begin, RailStationIds end, final int index) {
        Optional<RailRouteId> query = railRouteRepository.getForAgency(operatingCompany.getAgencyId()).stream().
                filter(railRouteId -> railRouteId.getBegin().equals(begin.getId())).
                filter(railRouteId -> railRouteId.getEnd().equals(end.getId())).
                filter(railRouteId -> railRouteId.getIndex() == index).findFirst();
        if (query.isEmpty()) {
            throw new RuntimeException(String.format("Unable to find RailRouteId for %s %s %s %s", operatingCompany, begin, end, index));
        }
        return query.get();
    }

    /***
     * Too unpredictable across data versions, use
     * @see RailRouteHelper::getRoutes
     * @param operatingCompany Train Operating Company
     * @param start start station
     * @param end end station
     * @param index assigned index
     * @return matching route
     */
    @Deprecated
    public Route getRoute(TrainOperatingCompanies operatingCompany, RailStationIds start, RailStationIds end, final int index) {
        final RailRouteId id = getRouteId(operatingCompany, start, end, index);
        return routeRepository.getRouteById(id);
    }

    public Set<Route> getRoutes(final TrainOperatingCompanies operatingCompany, final RailStationIds start, final RailStationIds end) {
        return railRouteRepository.getForAgency(operatingCompany.getAgencyId()).stream().
                filter(id -> id.getBegin().equals(start.getId())).
                filter(id -> id.getEnd().equals(end.getId())).
                map(routeRepository::getRouteById).
                collect(Collectors.toSet());
    }
}
