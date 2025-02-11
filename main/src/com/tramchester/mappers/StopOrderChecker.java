package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.domain.RailRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.BoundingBox;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@LazySingleton
public class StopOrderChecker {
    private static final Logger logger = LoggerFactory.getLogger(StopOrderChecker.class);

    private final BoundingBox bounds;
    private final RouteRepository routeRepository;
    private final StationRepository stationRepository;
    private final CRSRepository crsRepository;

    @Inject
    public StopOrderChecker(TramchesterConfig config, RouteRepository routeRepository, StationRepository stationRepository, CRSRepository crsRepository) {
        this.bounds = config.getBounds();
        this.routeRepository = routeRepository;
        this.stationRepository = stationRepository;
        this.crsRepository = crsRepository;
    }

    /***
     *
     * @param date date to check for
     * @param begin station
     * @param middleId station
     * @param endId station
     * @return true iff there is a route where these stations appear in trip sequence order begin->middle->end
     */
    public boolean check(final TramDate date, final Station begin, final IdFor<Station> middleId, final IdFor<Station> endId) {
        if (!bounds.contained(begin)) {
            logger.error("being station " + begin.getId() + " was out of bounds");
            return false;
        }

        final Station middle = stationRepository.hasStationId(middleId) ? stationRepository.getStationById(middleId) : crsRepository.getStationFor(middleId);
        final Station end = stationRepository.hasStationId(endId) ? stationRepository.getStationById(endId) : crsRepository.getStationFor(endId);

        // cannot check bounds like this....since station never loaded into the repository if OOB
        final boolean middleOutOfBounds = !bounds.contained(middle);
        final boolean endOutOfBounds = !bounds.contained(end);

        if (middleOutOfBounds && endOutOfBounds) {
            logger.error("Cannot have both middle " + middleId + " and end " + endId + " out of bounds");
            return false;
        }

        if (endOutOfBounds) {
            return checkWithEndOutOfBounds(date, begin, middle, end);
        }
        if (middleOutOfBounds) {
            throw new RuntimeException("Not implemented yet");
        }

        final Set<Route> beginRoutes = begin.getPickupRoutes();
        final Set<Route> middleRoutes = middle.getDropoffRoutes();
        final Set<Route> endRoutes = end.getDropoffRoutes();

        final SetUtils.SetView<Route> beginAndMiddle = SetUtils.intersection(beginRoutes, middleRoutes);
        final SetUtils.SetView<Route> beginMiddleAndEnd = SetUtils.intersection(beginAndMiddle, endRoutes);

        return beginMiddleAndEnd.stream().
                filter(route -> route.isAvailableOn(date)).
                anyMatch(route -> appearInOrder(date, begin, middle, end, route));

    }

    private boolean checkWithEndOutOfBounds(TramDate date, Station begin, Station middle, Station end) {

//        if (!end.servesMode(TransportMode.Train)) {
//            logger.error("Can on only process end station out of bounds for trains " + end.getId());
//            return false;
//        }

        final Set<Route> beginRoutes = begin.getPickupRoutes();
        final Set<Route> middleRoutes = middle.getDropoffRoutes();
        final SetUtils.SetView<Route> beginAndMiddle = SetUtils.intersection(beginRoutes, middleRoutes);

        return beginAndMiddle.stream().
                filter(route -> route.isAvailableOn(date)).
                filter(route -> route.getTransportMode()==TransportMode.Train).
                anyMatch(route -> railRouteCallsAt(route.getId(), middle, end));


    }

    private boolean railRouteCallsAt(final IdFor<Route> routeId, final Station middle, final Station end) {
        Route route = routeRepository.getRouteById(routeId);
        if (route instanceof RailRoute railRoute) {
            return railRoute.callsAtInOrder(middle, end);
        }
        return false;
    }

    private boolean appearInOrder(TramDate date, Station begin, Station middle, Station end, Route route) {
        return route.getTrips().stream().
                filter(trip -> trip.operatesOn(date)).
                anyMatch(trip -> appearInOrder(begin.getId(), middle.getId(), end.getId(), trip));
    }

    private boolean appearInOrder(IdFor<Station> begin, IdFor<Station> middle, IdFor<Station> end, Trip trip) {
        if (trip.callsAt(begin) && trip.callsAt(middle) && trip.callsAt(end)) {
            return trip.isAfter(begin, middle) && trip.isAfter(middle, end);
        }
        return false;
    }
}
