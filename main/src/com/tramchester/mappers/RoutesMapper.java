package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.repository.RouteRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.id.HasId.asIds;

@LazySingleton
public class RoutesMapper {
    private static final Logger logger = LoggerFactory.getLogger(RoutesMapper.class);

    private final RouteRepository routeRepository;
    private final DTOFactory DTOFactory;

    @Inject
    public RoutesMapper(RouteRepository routeRepository, DTOFactory DTOFactory) {
        this.DTOFactory = DTOFactory;
        this.routeRepository = routeRepository;
    }

    @PostConstruct
    private void start() {
        logger.info("Starting");

        logger.info("started");
    }

    public List<RouteDTO> getRouteDTOs(final TramDate date) {
        final Set<Route> routesOnDate = routeRepository.getRoutesRunningOn(date);
        logger.info("Get routeDTOs for " + date + " from " + asIds(routesOnDate));

        return routesOnDate.stream().map(route -> getLocationsAlong(route, true)).toList();
    }

    @NotNull
    private RouteDTO getLocationsAlong(final Route route, final boolean includeNotStopping) {
        final IdSet<Station> startStations = route.getStartStations();

        // Note: assumption is that longest sequence of stations found for a route is correct......
        Optional<List<Station>> longestSequence = startStations.stream().
                map(startId -> getStationsOn(route, includeNotStopping, startId)).
                max(Comparator.comparingLong(List::size));

        List<Station> stationsForRoute = longestSequence.orElse(Collections.emptyList());

        List<LocationRefWithPosition> stationDTOs = stationsForRoute.stream().map(DTOFactory::createLocationRefWithPosition).toList();

        return new RouteDTO(route, stationDTOs);
    }

    // use for visualisation in the front-end routes map
    public List<Station> getStationsOn(Route route, boolean includeNotStopping, IdFor<Station> startStation) {
        Set<Trip> tripsForRoute = route.getTrips();

        Set<Trip> startingAt = tripsForRoute.stream().filter(trip -> trip.firstStation().equals(startStation)).collect(Collectors.toSet());

        Optional<Trip> maybeLongest = startingAt.stream().
                max(Comparator.comparingLong(a -> a.getStopCalls().totalNumber()));

        if (maybeLongest.isEmpty()) {
            logger.error("Found no longest trip for route " + route.getId());
            return Collections.emptyList();
        }

        Trip longestTrip = maybeLongest.get();
        StopCalls stops = longestTrip.getStopCalls();
        return stops.getStationSequence(includeNotStopping);
    }

}
