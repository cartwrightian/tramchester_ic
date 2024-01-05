package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ServedRoute {

    private final Set<RouteAndService> routeAndServices;
    private final Map<RouteAndService, TimeRange> timeWindows;

    private final IdSet<Route> routeIds; // for performance, significant
    private final EnumSet<TransportMode> allServedModes; // for performance, significant

    public ServedRoute() {
        routeAndServices = new HashSet<>();
        timeWindows = new HashMap<>();
        routeIds = new IdSet<>();
        allServedModes = EnumSet.noneOf(TransportMode.class);
    }

    public void add(final Route route, final Service service, final TramTime callingTime) {
        final RouteAndService routeAndService = new RouteAndService(route, service);
        routeAndServices.add(routeAndService);

        if (callingTime.isValid()) {
            if (timeWindows.containsKey(routeAndService)) {
                timeWindows.get(routeAndService).updateToInclude(callingTime);
            } else {
                timeWindows.put(routeAndService, new TimeRange(callingTime));
            }
        }

        routeIds.add(route.getId());
        allServedModes.add(route.getTransportMode());
    }

    public boolean isEmpty() {
        return routeAndServices.isEmpty();
    }

    // TODO Remove date filtering here?
    public Set<Route> getRoutes(final TramDate date, final TimeRange range, final EnumSet<TransportMode> modes) {
        final Set<Route> results = getRouteForDateAndTimeRange(date, range, modes);
        if (range.intoNextDay()) {
            TimeRange nextDayRange = range.forFollowingDay();
            TramDate followingDay = date.plusDays(1);
            results.addAll(getRouteForDateAndTimeRange(followingDay, nextDayRange, modes));
        } else {
            // Cope with services from previous day that run into current date and range
            TramDate previousDay = date.minusDays(1);
            TimeRange previousDayRange = range.transposeToNextDay();
            results.addAll(getRouteForDateAndTimeRange(previousDay, previousDayRange, modes));
        }
        return results;
    }

    // TODO Remove date filtering here?
    @NotNull
    private Set<Route> getRouteForDateAndTimeRange(final TramDate date, final TimeRange range, final EnumSet<TransportMode> modes) {
        return routeAndServices.stream().
                filter(routeAndService -> routeAndService.isAvailableOn(date)).
                filter(routeAndService -> hasTimeRangerOverlap(range, routeAndService)).
                map(RouteAndService::getRoute).
                filter(route -> modes.contains(route.getTransportMode())).
                collect(Collectors.toSet());
    }

    public boolean anyAvailable(TramDate when, TimeRange timeRange, EnumSet<TransportMode> requestedModes) {
        // todo optimise this
        return !getRoutes(when, timeRange, requestedModes).isEmpty();
    }

    private boolean hasTimeRangerOverlap(TimeRange range, RouteAndService routeAndService) {
        return timeWindows.get(routeAndService).anyOverlap(range);
    }

    /***
     * Use the form that takes a date
     * @param route the route
     * @return true if route present
     */
    @Deprecated
    public boolean contains(Route route) {
        return routeIds.contains(route.getId());
    }

    @Override
    public String toString() {
        return "ServedRoute{" +
                "routeAndServices=" + routeAndServices +
                ", timeWindows=" + timeWindows +
                '}';
    }

    public Set<TransportMode> getTransportModes() {
        return Collections.unmodifiableSet(allServedModes);
    }

}
