package com.tramchester.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.metrics.CacheMetrics;
import jakarta.inject.Inject;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class StopCallRepository  {
    private static final Logger logger = LoggerFactory.getLogger(StopCallRepository.class);

    private final TripRepository tripRepository;
    private final StationRepository stationRepository;
    private final ServiceRepository serviceRepository;
    private final GraphFilterActive graphFilter;

    private final Map<Station, Set<StopCall>> stopCalls;
    private final Cache<CacheKey, Costs> cachedCosts;

    @Inject
    public StopCallRepository(TripRepository tripRepository, StationRepository stationRepository,
                              ServiceRepository serviceRepository, CacheMetrics cacheMetrics,
                              GraphFilterActive graphFilter) {
        this.tripRepository = tripRepository;
        this.stationRepository = stationRepository;
        this.serviceRepository = serviceRepository;
        this.graphFilter = graphFilter;
        stopCalls = new HashMap<>();

        // cost calcs potentially expensive, but only used during graph build
        cachedCosts = Caffeine.newBuilder().maximumSize(20000).expireAfterAccess(10, TimeUnit.MINUTES).
                recordStats().build();
        cacheMetrics.register(this::reportStats);
    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        stationRepository.getAllStationStream().forEach(station -> stopCalls.put(station, new HashSet<>()));

        final Set<Trip> allTrips = tripRepository.getTrips();

        final Set<StopCall> missingStations = allTrips.stream().
                flatMap(trip -> trip.getStopCalls().stream()).
                filter(stopCall -> !stationRepository.hasStationId(stopCall.getStationId())).
                collect(Collectors.toSet());

        if (!missingStations.isEmpty()) {
            final String message = "Missing stations found in stopcalls " + missingStations.size();
            logger.error(message);
            throw new RuntimeException(message);
        }

        allTrips.stream().
                flatMap(trip -> trip.getStopCalls().stream()).
                forEach(stopCall -> stopCalls.get(stopCall.getStation()).add(stopCall));

        final long noStops = stopCalls.entrySet().stream().
                filter(entry -> entry.getValue().isEmpty()).
                count();

        logger.info("Added stopcalls for " + (stopCalls.size() - noStops) + " stations");
        if (noStops > 0) {
            logger.warn(noStops + " stations have no StopCalls");
        }
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        stopCalls.clear();
        cachedCosts.cleanUp();
        logger.info("Stopped");
    }

    // visualisation of frequency support
    public Set<StopCall> getStopCallsFor(Station station, TramDate date, TramTime begin, TramTime end) {
        final Set<Service> runningOnDate = serviceRepository.getServicesOnDate(date, station.getTransportModes());
        final Set<StopCall> callsForStation = stopCalls.get(station);

        return callsForStation.stream().
                filter(stopCall -> stopCall.getPickupType().equals(GTFSPickupDropoffType.Regular)).
                filter(stopCall -> runningOnDate.contains(stopCall.getService())).
                filter(stopCall -> stopCall.getArrivalTime().between(begin, end)).
                collect(Collectors.toSet());
    }

    public Costs getCostsBetween(final Route route, final Station first, final Station second) {
        final CacheKey key = new CacheKey(route, first, second);
        return cachedCosts.get(key, id -> calculateCosts(route, first, second));
    }

    @NotNull
    private Costs calculateCosts(final Route route, final Station first, final Station second) {
        final boolean graphFilterActive = graphFilter.isActive();
        final List<Duration> allCosts = route.getTrips().stream().
                flatMap(trip -> trip.getStopCalls().getLegs(graphFilterActive).stream()).
                filter(leg -> leg.getFirstStation().equals(first) && leg.getSecondStation().equals(second)).
                map(StopCalls.StopLeg::getCost).
                collect(Collectors.toList());

        if (allCosts.isEmpty()) {
            String msg = format("Found no costs (stop legs) for stations %s and %s on route %s. Are they adjacent stations?",
                    first.getId(), second.getId(), route.getId());
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        return new Costs(allCosts, route.getId(), first.getId(), second.getId());
    }

    private List<Pair<String, CacheStats>> reportStats() {
        return Collections.singletonList(Pair.of("CachedCosts", cachedCosts.stats()));
    }

    public List<IdFor<Station>> getStopcallsBetween(final IdFor<Station> beginId, final IdFor<Station> endId, final DateRange dateRange) {
        final Set<List<IdFor<Station>>> uniqueSequencesForDateRange = dateRange.stream().
                map(date -> getStopcallsBetween(beginId, endId, date)).collect(Collectors.toSet());
        if (uniqueSequencesForDateRange.isEmpty()) {
            logger.warn(format("Found no call stop calls between %s and %s for %s", beginId, endId, dateRange));
            return Collections.emptyList();
        }
        if (uniqueSequencesForDateRange.size()>1) {
            String messgage = format("No unique sequence between %s and %s for %s got: %s", beginId, endId, dateRange,
                    uniqueSequencesForDateRange);
            logger.error(messgage);
            throw new RuntimeException(messgage);
        }
        return uniqueSequencesForDateRange.iterator().next();
    }

    public List<IdFor<Station>> getStopcallsBetween(final IdFor<Station> beginId, final IdFor<Station> endId, final TramDate date) {
        final Station begin = stationRepository.getStationById(beginId);
        final Station end = stationRepository.getStationById(endId);

        final Set<Route> routesForwards =
                SetUtils.intersection(begin.getPickupRoutes(), end.getDropoffRoutes()).stream().
                        filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());
        final Set<Route> routesBackwards =
                SetUtils.intersection(end.getPickupRoutes(), begin.getDropoffRoutes()).stream().
                filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        Set<StopCalls> allStopCalls = tripRepository.getTrips().stream().
                filter(trip -> trip.serviceOperatesOn(date)).
                filter(trip -> routesForwards.contains(trip.getRoute()) || routesBackwards.contains(trip.getRoute())).
                filter(trip -> trip.callsAt(beginId) && trip.callsAt(endId)).
                filter(trip -> trip.isAfter(beginId, endId)).
                map(Trip::getStopCalls).
                collect(Collectors.toSet());

        if (allStopCalls.isEmpty()) {
            String message = format("Found no call stop calls between %s and %s for %s", beginId, endId, date);
            logger.error(message);
            throw new RuntimeException(message);
        }

        final Set<List<IdFor<Station>>> uniqueSequences = allStopCalls.stream().
                map(stopCalls -> stationsBetween(stopCalls, beginId, endId)).
                collect(Collectors.toSet());

        if (uniqueSequences.size()!=1) {
            String message = "Did not find unambiguous set of sequences between " + beginId + " and " + endId + " got " + uniqueSequences;
            logger.error(message);
            throw new RuntimeException(message);
        }

        return uniqueSequences.iterator().next();
    }

    private List<IdFor<Station>> stationsBetween(final StopCalls stopCalls, final IdFor<Station> begin, final IdFor<Station> end) {
        int beginIndex = stopCalls.getStopFor(begin).getGetSequenceNumber();
        int endIndex = stopCalls.getStopFor(end).getGetSequenceNumber();

        if (beginIndex>endIndex) {
            String message = format("% (%s) is after %s (%s)", begin, beginIndex, end, endIndex);
            logger.error(message);
            throw new RuntimeException(message);

//            final int temp = beginIndex;
//            beginIndex = endIndex;
//            endIndex = temp;
        }

        final List<IdFor<Station>> result = new ArrayList<>();
        for (int i = beginIndex; i <= endIndex; i++) {
            final StopCall call = stopCalls.getStopBySequenceNumber(i);
            result.add(call.getStationId());
        }

        return result;
    }

    public static class Costs {

        private final List<Duration> costs;
        private final IdFor<Route> route;
        private final IdFor<Station> startId;
        private final IdFor<Station> endId;

        public Costs(List<Duration> costs, IdFor<Route> route, IdFor<Station> startId, IdFor<Station> endId) {
            this.costs = costs;
            this.route = route;
            this.startId = startId;
            this.endId = endId;
        }

        public Duration min() {
            return costs.stream().min(Duration::compareTo).orElse(Duration.ZERO);
        }

        public Duration max() {
            return costs.stream().max(Duration::compareTo).orElse(Duration.ZERO);
        }

        public Duration average() {
            double avg = costs.stream().
                    mapToLong(Duration::getSeconds).average().orElse(0D);
            @SuppressWarnings("WrapperTypeMayBePrimitive")
            final Double ceil = Math.ceil(avg);
            return Duration.ofSeconds( ceil.intValue() );
        }

        @Override
        public String toString() {
            return "Costs{" +
                    " route=" + route +
                    ", startId=" + startId +
                    ", endId=" + endId +
                    ", costs=" + costs +
                    '}';
        }

        public boolean isEmpty() {
            return costs.isEmpty();
        }

        public boolean consistent() {
            return costs.stream().distinct().count()==1L;
        }

    }

    private static class CacheKey {
        private final Route route;
        private final Station first;
        private final Station second;

        public CacheKey(Route route, Station first, Station second) {

            this.route = route;
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (!route.equals(cacheKey.route)) return false;
            if (!first.equals(cacheKey.first)) return false;
            return second.equals(cacheKey.second);
        }

        @Override
        public int hashCode() {
            int result = route.hashCode();
            result = 31 * result + first.hashCode();
            result = 31 * result + second.hashCode();
            return result;
        }
    }
}
