package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServedRoute {

    // TODO TEST ME PROPERLY!

    private final LocationId<?> locationId;
    private final Set<RouteAndService> routeAndServices;
    private final Map<RouteAndService, TimeRange> timeWindows;

    private final IdSet<Route> routeIds; // for performance, significant
    private final EnumSet<TransportMode> allServedModes; // for performance, significant

    public ServedRoute(final LocationId<?> locationId) {
        this.locationId = locationId;
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
                timeWindows.put(routeAndService, TimeRangePartial.of(callingTime));
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
            final TimeRange nextDayRange = range.forFollowingDay();
            final TramDate followingDay = date.plusDays(1);
            results.addAll(getRouteForDateAndTimeRange(followingDay, nextDayRange, modes));
        } else {
            // Cope with services from previous day that run into current date and range
            final TramDate previousDay = date.minusDays(1);
            final TimeRange previousDayRange = range.transposeToNextDay();
            results.addAll(getRouteForDateAndTimeRange(previousDay, previousDayRange, modes));
        }
        return results;
    }

    public Stream<TimeRange> getTimeRanges(final TramDate tramDate, final EnumSet<TransportMode> modes) {
        return routeAndServices.stream().
                filter(routeAndService -> routeAndService.isAvailableOn(tramDate)).
                filter(routeAndService -> modes.contains(routeAndService.getTransportMode())).
                map(timeWindows::get);
    }

    // TODO Remove date filtering here?
    @NotNull
    private Set<Route> getRouteForDateAndTimeRange(final TramDate date, final TimeRange range, final EnumSet<TransportMode> modes) {
        return routeAndServices.stream().
                filter(routeAndService -> modes.contains(routeAndService.getTransportMode())).
                filter(routeAndService -> routeAndService.isAvailableOn(date)).
                filter(routeAndService -> hasTimeRangerOverlap(range, routeAndService)).
                map(RouteAndService::getRoute).
                collect(Collectors.toSet());
    }

    private boolean anyRouteForDateAndTimeRange(final TramDate date, final TimeRange range, final EnumSet<TransportMode> modes) {
        return routeAndServices.stream().
                filter(routeAndService -> modes.contains(routeAndService.getTransportMode())).
                filter(routeAndService -> routeAndService.isAvailableOn(date)).
                anyMatch(routeAndService -> hasTimeRangerOverlap(range, routeAndService));
    }

    public boolean anyAvailable(final TramDate date, final TimeRange range, final EnumSet<TransportMode> modes) {
        if (anyRouteForDateAndTimeRange(date, range, modes)) {
            return true;
        }
        if (range.intoNextDay()) {
            final TramDate followingDay = date.plusDays(1);
            final TimeRange nextDayRange = range.forFollowingDay();
            return anyRouteForDateAndTimeRange(followingDay, nextDayRange, modes);
        } else {
            // Cope with services from previous day that run into current date and range
            final TramDate previousDay = date.minusDays(1);
            final TimeRange previousDayRange = range.transposeToNextDay();
            return anyRouteForDateAndTimeRange(previousDay, previousDayRange, modes);
        }
    }

    private boolean hasTimeRangerOverlap(final TimeRange range, final RouteAndService routeAndService) {
        return timeWindows.get(routeAndService).anyOverlap(range);
    }

    @Override
    public String toString() {
        return "ServedRoute{" +
                "locationId=" + locationId +
                "routeAndServices=" + routeAndServices +
                ", timeWindows=" + timeWindows +
                '}';
    }

    public Set<TransportMode> getTransportModes() {
        return Collections.unmodifiableSet(allServedModes);
    }


}
