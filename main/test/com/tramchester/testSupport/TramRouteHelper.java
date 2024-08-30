package com.tramchester.testSupport;

import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.reference.KnownBusRoute;
import com.tramchester.testSupport.reference.KnownTramRoute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
//                            stream().
//                            collect(Collectors.toSet());
            knownRouteToRoutes.put(knownRoute, routesByShortName);
        }
    }

    /***
     * Note: Use version that takes a date to get more consistent results
     * @param knownRoute the route to find
     * @return set of matching routes
     */
    @Deprecated
    public Set<Route> get(KnownTramRoute knownRoute) {
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
        return result.get(0);
    }

    public Route getOneRoute(KnownBusRoute knownRoute, TramDate date) {
        List<Route> result = routeRepository.findRoutesByName(knownRoute.getAgencyId(), knownRoute.getName()).stream().
                filter(route -> route.isAvailableOn(date)).toList();
        if (result.size()>1) {
            throw new RuntimeException(format("Found two many routes matching date %s and known route %s", date, knownRoute));
        }
        if (result.isEmpty()) {
            throw new RuntimeException(format("Found no routes matching date %s and known route %s", date, knownRoute));
        }
        return result.get(0);
    }

    public IdSet<Route> getId(KnownTramRoute knownRoute) {
        guard(knownRoute);
        return knownRouteToRoutes.get(knownRoute).stream().collect(IdSet.collector());
    }


    private void guard(KnownTramRoute knownRoute) {
        if (!knownRouteToRoutes.containsKey(knownRoute)) {
            throw new RuntimeException("Missing " + knownRoute);
        }
    }
}
