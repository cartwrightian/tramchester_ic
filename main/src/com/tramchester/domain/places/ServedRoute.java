package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServedRoute {

    // TODO TEST ME PROPERLY!

    private final LocationId<?> locationId;
    private final Set<RouteAndService> routeAndServices;
    private final Map<TimeWindowsKey, TimeRange> timeWindows;

    private final IdSet<Route> routeIds; // for performance, significant
    private final EnumSet<TransportMode> allServedModes; // for performance, significant

    public ServedRoute(final LocationId<?> locationId) {
        this.locationId = locationId;
        routeAndServices = new HashSet<>();
        timeWindows = new HashMap<>();
        routeIds = new IdSet<>();
        allServedModes = EnumSet.noneOf(TransportMode.class);
    }



    public boolean isEmpty() {
        return routeAndServices.isEmpty();
    }

    // TODO Remove date filtering here?
    public Set<Route> getRoutes(final TramDate date, final TimeRange range, final ImmutableEnumSet<TransportMode> modes) {
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

    public Stream<TimeRange> getTimeRanges(final TramDate tramDate, final ImmutableEnumSet<TransportMode> modes) {
        return routeAndServices.stream().
                filter(routeAndService -> routeAndService.isAvailableOn(tramDate)).
                filter(routeAndService -> modes.contains(routeAndService.getTransportMode())).
                map(TimeWindowsKey::from).
                map(timeWindows::get);
    }

    // TODO Remove date filtering here?
    @NotNull
    private Set<Route> getRouteForDateAndTimeRange(final TramDate date, final TimeRange range, final ImmutableEnumSet<TransportMode> modes) {
        return routeAndServices.stream().
                filter(routeAndService -> modes.contains(routeAndService.getTransportMode())).
                filter(routeAndService -> routeAndService.isAvailableOn(date)).
                filter(routeAndService -> hasTimeRangerOverlap(range, routeAndService)).
                map(RouteAndService::getRoute).
                collect(Collectors.toSet());
    }

    private boolean anyRouteForDateAndTimeRange(final TramDate date, final TimeRange range, final ImmutableEnumSet<TransportMode> modes) {
        return routeAndServices.stream().
                filter(routeAndService -> modes.contains(routeAndService.getTransportMode())).
                filter(routeAndService -> routeAndService.isAvailableOn(date)).
                anyMatch(routeAndService -> hasTimeRangerOverlap(range, routeAndService));
    }

    public boolean anyAvailable(final TramDate date, final TimeRange range, final ImmutableEnumSet<TransportMode> modes) {
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
        return timeWindows.get(TimeWindowsKey.from(routeAndService)).anyOverlap(range);
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

    public boolean addFor(final IdFor<Station> id, final Set<Route> routes, final Function<StopCall, TramTime> getTime) {

        final Set<Trip> callingTrips = routes.stream().
                flatMap(route -> route.getTrips().stream()).
                filter(trip -> trip.callsAt(id)).
                collect(Collectors.toSet());

        final Stream<StopCall> stationStopCalls = callingTrips.stream().
                map(Trip::getStopCalls).
                map(stopCalls -> stopCalls.getStopFor(id)).
                filter(StopCall::callsAtStation);

        return stationStopCalls.
                map(stopCall -> addForStopCall(stopCall, getTime)).
                reduce(false, (a,b) -> a || b);
    }

    public boolean addFor(final InterchangeStation interchangeStation,
                           final Set<Route> routes, final Function<StopCall, TramTime> getTime) {

        // trips via routes so include linked pick up routes for interchanges
        final Set<Trip> callingTrips = routes.stream().
                flatMap(route -> route.getTrips().stream()).
                filter(trip -> trip.callsAt(interchangeStation)).
                collect(Collectors.toSet());

        // TODO just a stream
        final Stream<StopCall> stationStopCalls = callingTrips.stream().
                map(Trip::getStopCalls).
                map(stopCalls -> stopCalls.getStopFor(interchangeStation)).
                filter(StopCall::callsAtStation);

        return stationStopCalls.
                map(stopCall -> addForStopCall(stopCall, getTime)).
                reduce(false, (a,b) -> a || b);
    }

    private boolean addForStopCall(final StopCall stopCall, final Function<StopCall, TramTime> getTime) {
        final TramTime time = getTime.apply(stopCall);
        if (!time.isValid()) {
            //logger.warn(format("Invalid time %s for %s %s", time, locationId, stopCall));
            return false;
        }
        add(stopCall, time);
        return true;
    }

    private void add(final StopCall stopCall, final TramTime callingTime) {

        final Trip trip = stopCall.getTrip();

        final Route route = trip.getRoute();
        final Service service = trip.getService();

        final RouteAndService routeAndService = new RouteAndService(route, service);
        routeAndServices.add(routeAndService);

        final TimeWindowsKey key = TimeWindowsKey.from(routeAndService);
        if (callingTime.isValid()) {
            if (timeWindows.containsKey(key)) {
                timeWindows.get(key).updateToInclude(callingTime);
            } else {
                timeWindows.put(key, TimeRangePartial.of(callingTime));
            }
        }

        routeIds.add(route.getId());
        allServedModes.add(route.getTransportMode());
    }

    private record TimeWindowsKey (IdFor<Route> routeId, IdFor<Service> serviceId) {
        public TimeWindowsKey(RouteAndService routeAndService) {
            this(routeAndService.getRoute().getId(), routeAndService.getServiceId());
        }

        public static TimeWindowsKey from(RouteAndService routeAndService) {
            return new TimeWindowsKey(routeAndService);
        }
    }

}
