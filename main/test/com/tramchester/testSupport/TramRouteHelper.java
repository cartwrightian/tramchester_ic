package com.tramchester.testSupport;

import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.reference.KnownBusRoute;
import com.tramchester.testSupport.reference.KnownTramRoute;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/***
 * Test helper only
 */
public class TramRouteHelper {

    private Map<KnownTramRoute, Set<Route>> knownRouteToRoutes;
    private final RouteRepository routeRepository;

    public TramRouteHelper(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
        createMap();
    }

    private void createMap() {
        knownRouteToRoutes = new HashMap<>();
        KnownTramRoute[] knownTramRoutes = KnownTramRoute.values(); // ignores date
        for (KnownTramRoute knownRoute : knownTramRoutes) {
            final Set<Route> routesByShortName = routeRepository.findRoutesByShortName(MutableAgency.METL, knownRoute.shortName());
            knownRouteToRoutes.put(knownRoute, routesByShortName);
        }
    }

    /***
     * Note: Use version that takes a date to get more consistent results
     * @param knownRoute the route to find
     * @return set of matching routes
     */
    @Deprecated
    public Set<Route> get(final KnownTramRoute knownRoute) {
        guard(knownRoute);
        return knownRouteToRoutes.get(knownRoute);
    }

    public Route getOneRoute(final KnownTramRoute knownRoute, final TramDate date) {
        guard(knownRoute);
        final Set<Route> routes = knownRouteToRoutes.get(knownRoute);
        final List<Route> result = routes.stream().filter(route -> route.isAvailableOn(date)).toList();
        if (result.size()>1) {
            throw new RuntimeException(format("Found two many routes %s matching date %s and known route %s", HasId.asIds(result), date, knownRoute));
        }
        if (result.isEmpty()) {
            throw new RuntimeException(format("Found no routes matching date %s and known route %s", date, knownRoute));
        }
        return result.getFirst();
    }

    public Route getOneRoute(final KnownBusRoute knownRoute, final TramDate date) {
        List<Route> result = routeRepository.findRoutesByName(knownRoute.getAgencyId(), knownRoute.getName()).stream().
                filter(route -> route.isAvailableOn(date)).toList();
        if (result.size()>1) {
            throw new RuntimeException(format("Found two many routes matching date %s and known route %s", date, knownRoute));
        }
        if (result.isEmpty()) {
            throw new RuntimeException(format("Found no routes matching date %s and known route %s", date, knownRoute));
        }
        return result.getFirst();
    }

    public IdSet<Route> getId(final KnownTramRoute knownRoute) {
        guard(knownRoute);
        return knownRouteToRoutes.get(knownRoute).stream().collect(IdSet.collector());
    }


    private void guard(final KnownTramRoute knownRoute) {
        if (!knownRouteToRoutes.containsKey(knownRoute)) {
            throw new RuntimeException("Missing " + knownRoute);
        }
    }

    public List<IdFor<Station>> getClosedBetween(final IdFor<Station> begin, final IdFor<Station> end) {
        final Set<StopCalls> allStopCalls = routeRepository.getRoutes().stream().
                flatMap(route -> route.getTrips().stream()).
                filter(trip -> trip.callsAt(begin) && trip.callsAt(end)).
                map(Trip::getStopCalls).
                collect(Collectors.toSet());

        if (allStopCalls.isEmpty()) {
            throw new RuntimeException("No stop calls");
        }

        final Set<List<IdFor<Station>>> uniqueSequences = allStopCalls.stream().
                map(stopCalls -> stationsBetween(stopCalls, begin, end)).
                collect(Collectors.toSet());

        if (uniqueSequences.size()!=1) {
            throw new RuntimeException("Did not find unambiguous set of sequences between " + begin + " and " +
                    end + " got " + uniqueSequences);
        }

        return uniqueSequences.iterator().next();
    }

    private List<IdFor<Station>> stationsBetween(final StopCalls stopCalls, final IdFor<Station> begin, final IdFor<Station> end) {
        int beginIndex = stopCalls.getStopFor(begin).getGetSequenceNumber();
        int endIndex = stopCalls.getStopFor(end).getGetSequenceNumber();

        if (beginIndex>endIndex) {
            final int temp = beginIndex;
            beginIndex = endIndex;
            endIndex = temp;
        }

        final List<IdFor<Station>> result = new ArrayList<>();
        for (int i = beginIndex; i <= endIndex; i++) {
            StopCall call = stopCalls.getStopBySequenceNumber(i);
            result.add(call.getStationId());
        }

        final IdFor<Station> firstResult = result.getFirst();
        final IdFor<Station> finalResult = result.getLast();

        // need a well defined ordering as trams might go between begin and end in either direction
        if (firstResult.getGraphId().compareTo(finalResult.getGraphId())<0) {
            return result.reversed();
        } else {
            return result;
        }
    }
}
