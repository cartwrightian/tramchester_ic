package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.*;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Stream;

@LazySingleton
public class RunningRoutesAndServices {
    private static final Logger logger = LoggerFactory.getLogger(RunningRoutesAndServices.class);

    private final ServiceRepository serviceRepository;
    private final RouteRepository routeRepository;

    @Inject
    public RunningRoutesAndServices(ServiceRepository serviceRepository, RouteRepository routeRepository) {
        this.serviceRepository = serviceRepository;
        this.routeRepository = routeRepository;
    }

    public FilterForDate getFor(final JourneyRequest journeyRequest) {
        return getFor(journeyRequest.getDate(), journeyRequest.getRequestedModes());
    }

    public FilterForDate getFor(final TramDate date, final ImmutableEnumSet<TransportMode> modes) {
        final Set<Service> servicesForMode = serviceRepository.getServices(modes);
        final Set<Route> routesForMode = routeRepository.getRoutes(modes);

        final IdMap<Service> serviceIds = getServicesFor(date, servicesForMode);
        final IdMap<Route> routeIds = getRoutesFor(date, routesForMode);

        final TramDate nextDay = date.plusDays(1);
        final IdMap<Service> runningServicesNextDay = getServicesFor(nextDay, servicesForMode);
        final IdMap<Route> runningRoutesNextDay = getRoutesFor(nextDay, routesForMode);

        final TramDate previousDay = date.minusDays(1);
        final IdMap<Service> previousDaySvcs = servicesIntoNextDay(previousDay, servicesForMode);
        final IdMap<Route> previousDayRoutes = routesIntoNextDayFor(previousDay, routesForMode);

        return new FilterForDate(date, serviceIds, routeIds, runningServicesNextDay, runningRoutesNextDay,
                previousDaySvcs, previousDayRoutes);
    }

    private IdMap<Route> routesIntoNextDayFor(final TramDate date, final Set<Route> routes) {
        final Stream<Route> onDate = routes.stream().filter(route -> route.isAvailableOn(date));
        return intoNextDay(onDate);
    }

    private IdMap<Service> servicesIntoNextDay(final TramDate date, final Set<Service> services) {
        final Stream<Service> onDate = services.stream().filter(service -> service.getCalendar().operatesOn(date));
        return intoNextDay(onDate);
    }

    private <T extends HasId<T> & CoreDomain & CrossesDay> IdMap<T> intoNextDay(final Stream<T> items) {
        return items.
                filter(CrossesDay::intoNextDay).
                collect(IdMap.collector());
    }

    @NotNull
    private IdMap<Route> getRoutesFor(final TramDate date, final Set<Route> routes) {
        final IdMap<Route> onDate = routes.stream().
                filter(route -> route.isAvailableOn(date)).
                collect(IdMap.collector());
        if (onDate.isEmpty()) {
            logger.warn("No running routes found on " + date);
        } else {
            logger.info("Found " + onDate.size() + " running routes for " + date);
        }
        return onDate;
    }

    @NotNull
    private IdMap<Service> getServicesFor(final TramDate date, final Set<Service> services) {
        final IdMap<Service> onDate = services.stream().
                filter(service -> service.getCalendar().operatesOn(date)).
                collect(IdMap.collector());
        if (onDate.isEmpty()) {
            logger.warn("No running services found on " + date);
        } else {
            logger.info("Found " + onDate.size() + " running services for " + date);
        }
        return onDate;
    }


    public static class FilterForDate {
        private final TramDate date;
        private final IdMap<Service> servicesPreviousDay;
        private final IdMap<Route> routesPreviousDay;
        private final IdMap<Service> servicesToday;
        private final IdMap<Route> routesToday;
        private final IdMap<Service> servicesNextDay;
        private final IdMap<Route> routesNextDay;

        private FilterForDate(TramDate date, IdMap<Service> servicesToday, IdMap<Route> routesToday,
                              IdMap<Service> servicesNextDay, IdMap<Route> routesNextDay,
                              IdMap<Service> servicesPreviousDay, IdMap<Route> routesPreviousDay) {
            this.date = date;
            this.servicesToday = servicesToday;
            this.routesToday = routesToday;
            this.servicesNextDay = servicesNextDay;
            this.routesNextDay = routesNextDay;
            this.servicesPreviousDay = servicesPreviousDay;
            this.routesPreviousDay = routesPreviousDay;
        }

        public boolean isServiceRunningByDate(IdFor<Service> serviceId, boolean nextDay) {
            if (servicesToday.hasId(serviceId)) {
                return true;
            }

            if (nextDay) {
                return servicesNextDay.hasId(serviceId);
            } else {
                return servicesPreviousDay.hasId(serviceId);
            }
        }

        public boolean isRouteRunning(IdFor<Route> routeId, boolean nextDay) {
            if (routesToday.hasId(routeId)) {
                return true;
            }

            if (nextDay) {
                return routesNextDay.hasId(routeId);
            } else {
                return routesPreviousDay.hasId(routeId);
            }
        }

        @Override
        public String toString() {
            return "FilterForDate{" +
                    "date=" + date +
                    ", servicesPreviousDay=" + servicesPreviousDay.size() +
                    ", routesPreviousDay=" + routesPreviousDay.size() +
                    ", servicesToday=" + servicesToday.size() +
                    ", routesToday=" + routesToday.size() +
                    ", servicesNextDay=" + servicesNextDay.size() +
                    ", routesNextDay=" + routesNextDay.size() +
                    '}';
        }

        public boolean isServiceRunningByTime(final IdFor<Service> serviceId, final TramTime time, final int maxWait) {

            if (servicesToday.hasId(serviceId)) {
                final Service todaySvc = servicesToday.get(serviceId);
                if (serviceOperatingWithin(todaySvc, time, maxWait)) {
                    return true;
                }
            }

            final int hourOfDay = time.getHourOfDay();
            final int minuteOfHour = time.getMinuteOfHour();

            if (time.isNextDay()) {
                if (servicesNextDay.hasId(serviceId)) {
                    // remove next day offset to get time for the following day
                    final TramTime timeForNextDay = TramTime.of(hourOfDay, minuteOfHour);
                    final Service nextDaySvc = servicesNextDay.get(serviceId);
                    return serviceOperatingWithin(nextDaySvc, timeForNextDay, maxWait);
                }
            } else {
                if (servicesPreviousDay.hasId(serviceId)) {
                    // use next day time, do any of previous days services run into today
                    final TramTime timeForPreviousDay = TramTime.nextDay(hourOfDay, minuteOfHour);
                    final Service previousDayService = servicesPreviousDay.get(serviceId);
                    return serviceOperatingWithin(previousDayService, timeForPreviousDay, maxWait);
                }
            }

            return false;
        }

        private boolean serviceOperatingWithin(final Service service, final TramTime time, final int maxWait) {
            final TramTime finishTime = service.getFinishTime();
            final TramTime startTime = service.getStartTime();

            final TimeRange withinService = TimeRange.of(startTime, finishTime);
            if (withinService.contains(time)) {
                return true;
            }

            // check if within wait time
            final TimeRange range = TimeRangePartial.of(startTime, TramDuration.ofMinutes(maxWait), TramDuration.ZERO);
            return range.contains(time);
        }
    }
}
