package com.tramchester.dataimport.rail.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.ExtractAgencyCallingPointsFromLocationRecords;
import com.tramchester.dataimport.rail.ProvidesRailTimetableRecords;
import com.tramchester.dataimport.rail.RailRouteIDBuilder;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.places.Station;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.ReportsCacheStats;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/***
 * Used to create view of rail routes as part of rail data import
 * NOTE:
 * is used as part of TransportData load so cannot depend (via dep injection) on that data, hence
 * ExtractAgencyCallingPointsFromLocationRecords being used here
 */
@LazySingleton
public class RailRouteIds implements ReportsCacheStats, RailRouteIdRepository {
    private static final Logger logger = LoggerFactory.getLogger(RailRouteIds.class);
    public static final int CACHED_ROUTES_SIZE = 8000; // approx 6000 rail routes currently Jan 2023

    private final RailStationRecordsRepository stationRecordsRepository;
    private final ProvidesRailTimetableRecords providesRailTimetableRecords;
    private final RailRouteIDBuilder railRouteIDBuilder;
    private final boolean enabled;
    private final CacheMetrics cacheMetrics;

    private Map<IdFor<Agency>, Set<RailRouteCallingPointsWithRouteId>> routeIdsForAgency;

    // many repeated calls during rail data load, caching helps significantly with performance
    private Cache<RailRouteCallingPoints, RailRouteId> cachedIds;

    @Inject
    public RailRouteIds(RailStationRecordsRepository stationRecordsRepository, ProvidesRailTimetableRecords providesRailTimetableRecords,
                        RailRouteIDBuilder railRouteIDBuilder, TramchesterConfig config,
                        CacheMetrics cacheMetrics) {
        this.stationRecordsRepository = stationRecordsRepository;
        this.providesRailTimetableRecords = providesRailTimetableRecords;
        this.railRouteIDBuilder = railRouteIDBuilder;
        enabled = config.hasRailConfig();
        this.cacheMetrics = cacheMetrics;
    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            logger.info("Rail is not enabled");
            return;
        }

        routeIdsForAgency = new HashMap<>();
        // only used during load, hence very short duration
        cachedIds = Caffeine.newBuilder().maximumSize(CACHED_ROUTES_SIZE).expireAfterAccess(2, TimeUnit.MINUTES).
                recordStats().build();

        cacheMetrics.register(this);

        logger.info("Starting");
        createRouteIdsFor(providesRailTimetableRecords, stationRecordsRepository);
        logger.info("Started");
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        if (enabled) {
            routeIdsForAgency.clear();
            cachedIds.invalidateAll();
        }
        logger.info("stopped");
    }

    private void createRouteIdsFor(ProvidesRailTimetableRecords providesRailTimetableRecords, RailStationRecordsRepository stationRecordsRepository) {
        List<RailRouteCallingPoints> loadedCallingPoints = ExtractAgencyCallingPointsFromLocationRecords.
                loadCallingPoints(providesRailTimetableRecords, stationRecordsRepository);
        createRouteIdsFor(loadedCallingPoints);
        loadedCallingPoints.clear();
    }

    private void createRouteIdsFor(final List<RailRouteCallingPoints> agencyCallingPoints) {

        logger.info("Create possible route ids for " + agencyCallingPoints.size() + " calling points combinations");

        // efficiency: group calling points by agency id
        final Map<IdFor<Agency>, List<RailRouteCallingPoints>> callingPointsByAgency = new HashMap<>();

        agencyCallingPoints.forEach(points -> {
            final IdFor<Agency> agencyId = points.getAgencyId();
            if (!callingPointsByAgency.containsKey(agencyId)) {
                callingPointsByAgency.put(agencyId, new ArrayList<>());
            }
            callingPointsByAgency.get(agencyId).add(points);
        });

        final List<Integer> totals = new ArrayList<>();

        callingPointsByAgency.forEach((agencyId, callingPoints) -> {
            final Set<RailRouteCallingPointsWithRouteId> results = railRouteIDBuilder.getRouteIdsFor(agencyId, callingPoints);
            routeIdsForAgency.put(agencyId, results);
            totals.add(results.size());
        });

        int total = totals.stream().reduce(Integer::sum).orElse(0);

        logger.info("Created " + total + " ids from " + agencyCallingPoints.size() + " sets of calling points");

        callingPointsByAgency.clear();
    }

    /***
     * Only use during rail data import, use RouteRepository otherwise
     * @param agencyId agency id for this rail route
     * @param callingStations stations this route calls at
     * @return the MutableRoute to use
     */
    @Override
    public RailRouteId getRouteIdFor(final IdFor<Agency> agencyId, final List<Station> callingStations) {
        // to a list, order matters
        final List<IdFor<Station>> callingStationsIds = callingStations.stream().map(Station::getId).collect(Collectors.toList());

        final RailRouteCallingPoints agencyCallingPoints = new RailRouteCallingPoints(agencyId, callingStationsIds);
        return cachedIds.get(agencyCallingPoints, unused -> getRouteId(agencyCallingPoints));
    }

    @Override
    public RailRouteCallingPointsWithRouteId find(IdFor<Agency> agencyId, IdFor<Route> routeId) {
        Set<RailRouteCallingPointsWithRouteId> forAgency = routeIdsForAgency.get(agencyId);
        Optional<RailRouteCallingPointsWithRouteId> find = forAgency.stream().
                filter(callingPoints -> callingPoints.routeId.equals(routeId)).findFirst();
        return find.orElse(null);
    }

    /** test support **/
    public RailRouteId getRouteIdUncached(final IdFor<Agency> agencyId, final List<Station> callingStations) {
        final List<IdFor<Station>> callingStationsIds = callingStations.stream().map(Station::getId).collect(Collectors.toList());
        final RailRouteCallingPoints agencyCallingPoints = new RailRouteCallingPoints(agencyId, callingStationsIds);
        return getRouteId(agencyCallingPoints);
    }

    public Set<RailRouteId> getForAgency(final IdFor<Agency> agencyId) {
        return routeIdsForAgency.get(agencyId).stream().map(RailRouteCallingPointsWithRouteId::getRouteId).collect(Collectors.toSet());
    }

    /***
     * main mechanism for route id allocation to a callings points on a route
     * @param agencyCallingPoints places the route calls at
     * @return an id for the route
     */
    public RailRouteId getRouteId(final RailRouteCallingPoints agencyCallingPoints) {

        final IdFor<Agency> agencyId = agencyCallingPoints.getAgencyId();

        if (!routeIdsForAgency.containsKey(agencyId)) {
            Set<IdFor<Agency>> ids = routeIdsForAgency.keySet();
            String msg = "Did not find agency "+ agencyId +" in routes, cache: " + ids;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        final StationIdPair beginEnd = agencyCallingPoints.getBeginEnd();

        // existing routes and corresponding IDs
        // NOTE: was just selecting the Max match on calling points here, but his created indeterminate results when
        // calling points had the same length, so now sort by calling points and route index to address this
        List<RailRouteCallingPointsWithRouteId> matching = routeIdsForAgency.get(agencyId).stream().
                filter(callingPoints -> callingPoints.getBeginEnd().equals(beginEnd)).
                filter(callingPoints -> callingPoints.contains(agencyCallingPoints)).
                sorted(this::compareMatching).
                toList();

        if (matching.isEmpty()) {
            throw new RuntimeException("Could not find a route id for " + agencyCallingPoints);
        }

        return matching.getFirst().getRouteId();
    }

    private int compareMatching(RailRouteCallingPointsWithRouteId routeA, RailRouteCallingPointsWithRouteId routeB) {
        // begin/end will be the same here, as will agency, so index should be the only difference if number of calling
        // points are the same
        int sizeCompare = Integer.compare(routeA.callingPoints.numberCallingPoints(), routeA.callingPoints.numberCallingPoints());
        if (sizeCompare==0) {
            return Integer.compare(routeA.routeId.getIndex(), routeB.routeId.getIndex());
        }
        return sizeCompare;
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        List<Pair<String,CacheStats>> result = new ArrayList<>();
        result.add(Pair.of("routeIdCache", cachedIds.stats()));
        return result;
    }

    public Set<RailRouteCallingPointsWithRouteId> getCallingPointsFor(IdFor<Agency> agencyId) {
        return routeIdsForAgency.get(agencyId);
    }

    public int getSize() {
        return routeIdsForAgency.values().stream().
                mapToInt(Set::size).
                sum();
    }

    public Set<RailRouteId> getFor(final IdFor<Station> begin, final IdFor<Station> end) {
        final StationIdPair pair = StationIdPair.of(begin, end);
        return routeIdsForAgency.values().stream().flatMap(Collection::stream).
                filter(callingPointsWithRouteId -> callingPointsWithRouteId.getBeginEnd().equals(pair)).
                map(callingPointsWithRouteId -> callingPointsWithRouteId.routeId).
                collect(Collectors.toSet());
    }

    public static class RailRouteCallingPointsWithRouteId {

        private final RailRouteId routeId;
        private final RailRouteCallingPoints callingPoints;

        public RailRouteCallingPointsWithRouteId(RailRouteCallingPoints callingPoints, RailRouteId routeId) {
            this.callingPoints = callingPoints;
            this.routeId = routeId;
        }

        public RailRouteId getRouteId() {
            return routeId;
        }

        @Override
        public String toString() {
            return "RailRouteCallingPointsWithRouteId{" +
                    "routeId=" + routeId +
                    ", callingPoints=" + callingPoints +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            RailRouteCallingPointsWithRouteId that = (RailRouteCallingPointsWithRouteId) o;
            return routeId.equals(that.routeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), routeId);
        }

        public StationIdPair getBeginEnd() {
            return callingPoints.getBeginEnd();
        }

        public boolean contains(RailRouteCallingPoints points) {
            return callingPoints.contains(points);
        }

        public int numberCallingPoints() {
            return callingPoints.numberCallingPoints();
        }

        public List<IdFor<Station>> getCallingPoints() {
            return callingPoints.getCallingPoints();
        }
    }
}
