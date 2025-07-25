package com.tramchester.testSupport;

import com.tramchester.ComponentContainer;
import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.TFGMRouteNames;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.reference.KnownBusRoute;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TestRoute;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

/***
 * Test helper only
 */
public class TramRouteHelper {

    private Map<TestRoute, Set<Route>> knownRouteToRoutes;
    private final RouteRepository routeRepository;

    private TramRouteHelper(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
        createMap();
    }

    public TramRouteHelper(ComponentContainer componentContainer) {
        this(componentContainer.get(RouteRepository.class));
        componentContainer.registerCallbackFor(() -> knownRouteToRoutes.clear());
    }

    private void createMap() {
        knownRouteToRoutes = new HashMap<>();
        TestRoute[] knownTramRoutes = KnownTramRoute.values(); // ignores date
        for (TestRoute knownRoute : knownTramRoutes) {
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
    public Set<Route> get(final TestRoute knownRoute) {
        guard(knownRoute);
        return knownRouteToRoutes.get(knownRoute);
    }

    public Route getOneRoute(final TFGMRouteNames line, final TramDate date) {
        TestRoute knownRoute = KnownTramRoute.findFor(line, date);
        return getOneRouteFor(knownRoute, date);
    }

    private Route getOneRouteFor(final TestRoute knownRoute, final TramDate date) {
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

    public IdSet<Route> getId(final TestRoute knownRoute) {
        guard(knownRoute);
        return knownRouteToRoutes.get(knownRoute).stream().collect(IdSet.collector());
    }


    private void guard(final TestRoute knownRoute) {
        if (!knownRouteToRoutes.containsKey(knownRoute)) {
            throw new RuntimeException("Missing " + knownRoute);
        }
    }

    /***
     * Use version that passes KnownTramLine
     * @param knownTramRoute
     * @param when
     * @return
     */
    @Deprecated
    public Route getOneRoute(@NotNull TestRoute knownTramRoute, TramDate when) {
        return getOneRouteFor(knownTramRoute, when);
    }

    public Route getRed(TramDate date) {
        return getOneRoute(TFGMRouteNames.Red, date);
    }

    public Route getNavy(TramDate date) {
        return getOneRoute(TFGMRouteNames.Navy, date);
    }

    public Route getBlue(TramDate date) {
        return getOneRoute(TFGMRouteNames.Blue, date);
    }

    public Route getPink(TramDate date) {
        return getOneRoute(TFGMRouteNames.Pink, date);
    }

    public Route getGreen(TramDate date) {
        return getOneRoute(TFGMRouteNames.Green, date);
    }

    public Route getYellow(TramDate date) {
        return getOneRoute(TFGMRouteNames.Yellow, date);
    }

    public Route getPurple(TramDate date) {
        return getOneRoute(TFGMRouteNames.Purple, date);
    }


}
