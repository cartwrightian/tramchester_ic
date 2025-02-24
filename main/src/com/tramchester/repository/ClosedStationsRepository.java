package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.StationListConfig;
import com.tramchester.config.StationPairConfig;
import com.tramchester.config.StationsConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.closures.ClosedStationFactory;
import com.tramchester.domain.closures.Closure;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.graph.filters.GraphFilter;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class ClosedStationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClosedStationsRepository.class);

    private final TramchesterConfig config;
    private final ClosedStationFactory closedStationFactory;
    private final StationRepository stationRepository;
    private final StopCallRepository stopCallRepository;

    private final ClosedStationContainer closedStationContainer;

    @Inject
    public ClosedStationsRepository(TramchesterConfig config, ClosedStationFactory closedStationFactory,
                                    StationRepository stationRepository, StopCallRepository stopCallRepository, GraphFilter graphFilter) {
        this.config = config;
        this.closedStationFactory = closedStationFactory;
        this.stationRepository = stationRepository;
        this.stopCallRepository = stopCallRepository;
        closedStationContainer = new ClosedStationContainer(closedStationFactory, graphFilter);
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        config.getGTFSDataSource().forEach(source -> {
            Set<StationClosures> closures = new HashSet<>(source.getStationClosures());
            if (!closures.isEmpty()) {
                captureClosedStationsFromConfig(closures);
            } else {
                logger.info("No closures for " + source.getName());
            }
        });
        logger.warn("Added " + closedStationContainer.countStations() + " stations closures");
        logger.info("Started");
    }

    public void captureClosedStationsFromConfig(final Set<StationClosures> closureConfigs) {

        logger.info("Capturing closures from " + closureConfigs.size() + " config elements");

        // capture closed stations and date ranges
        closureConfigs.forEach(closureConfig -> {
            final StationsConfig stations = closureConfig.getStations();
            if (stations instanceof StationListConfig stationListConfig) {
                final IdSet<Station> configStationIds = stationListConfig.getStations();

                final Closure closure = closedStationFactory.createFor(closureConfig, configStationIds);
                closedStationContainer.addStationsFor(closure, configStationIds);
            } else if (stations instanceof StationPairConfig stationPairConfig) {
                final StationIdPair pair = stationPairConfig.getStationPair();
                final List<IdFor<Station>> stationIdsBetween = stopCallRepository.getClosedBetween(pair.getBeginId(), pair.getEndId());
                final IdSet<Station> closedStationsIds = stationIdsBetween.stream().collect(IdSet.idCollector());

                final Closure closure = closedStationFactory.createFor(closureConfig, closedStationsIds);
                closedStationContainer.addStationsFor(closure, stationIdsBetween);
            } else {
                throw new RuntimeException("Unexpected type for " + closureConfig);
            }

        });

        closureConfigs.forEach(this::createClosedStations);

    }


    private void createClosedStations(final StationClosures closures) {
        final StationsConfig stationClosureConfig = closures.getStations();

        if (stationClosureConfig instanceof StationListConfig stationListConfig) {
            stationListConfig.getStations().forEach(stationId -> {
                final Station station = stationRepository.getStationById(stationId);
                closedStationContainer.add(closures, station);
            });
        } else if (stationClosureConfig instanceof StationPairConfig stationPairConfig) {
            StationIdPair idPair = stationPairConfig.getStationPair();
            List<IdFor<Station>> stationIds = stopCallRepository.getClosedBetween(idPair.getBeginId(), idPair.getEndId());
            stationIds.forEach(stationId -> {
                final Station station = stationRepository.getStationById(stationId);
                closedStationContainer.add(closures, station);
            });
        }
        else {
            throw new RuntimeException("unexpected type " + stationClosureConfig);
        }
    }



    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        closedStationContainer.clear();
        logger.info("Stopped");
    }


    public Set<ClosedStation> getClosedStationsFor(final DataSourceID sourceId) {
        return closedStationContainer.getFor(sourceId);
    }

    private static void guardForStationOnly(Location<?> location) {
        if (location.getLocationType()!=LocationType.Station) {
            String msg = "Not a station " + location.getId() + " " + location.getLocationType();
            logger.error(msg);
            throw new RuntimeException(msg);
        }
    }

    public boolean anyStationOpen(final LocationSet<Station> locations, final TramDate date) {
        return closedStationContainer.anyStationOpen(locations, date);

    }

    public Set<ClosedStation> getAnyWithClosure(final TramDate date) {
        return closedStationContainer.getAnyWithClosure(date);
    }


    public boolean isClosed(Location<?> location, TramDate date, TimeRange timeRange) {
        return switch (location.getLocationType()) {
            case Station -> isStationClosed((Station)location, date, timeRange);
            case StationGroup -> isGroupClosed((StationLocalityGroup)location, date, timeRange);
            case Platform -> isPlatformClosed((Platform)location, date, timeRange);
            case Postcode, MyLocation -> false;
        };
    }

    public boolean isClosed(final Location<?> location, final TramDate date) {
        return switch (location.getLocationType()) {
            case Station -> isStationClosed((Station)location, date);
            case StationGroup -> isGroupClosed((StationLocalityGroup)location, date);
            case Platform -> isPlatformClosed((Platform)location, date);
            case Postcode, MyLocation -> false;
        };
    }

    private boolean isPlatformClosed(final Platform platform, final TramDate date) {
        return isStationClosed(platform.getStation(), date);
    }

    private boolean isPlatformClosed(final Platform platform, final TramDate date, final TimeRange timeRange) {
        return isStationClosed(platform.getStation(), date, timeRange);
    }

    private boolean isStationClosed(final Station station, final TramDate date) {
        return closedStationContainer.isStationClosed(station.getId(), date);
    }

    public boolean isGroupClosed(final StationLocalityGroup group, final TramDate date) {
        return allClosed(group.getAllContained(), date);
    }

    public boolean isGroupClosed(final StationLocalityGroup group, final TramDate date, final TimeRange timeRange) {
        if (timeRange.allDay()) {
            return allClosed(group.getAllContained(), date);
        }
        else {
            return allClosed(group.getAllContained(), date, timeRange);
        }
    }

    public boolean allClosed(final LocationCollection locations, final TramDate date) {
        return locations.locationStream().allMatch(location -> isClosed(location, date));
    }

    private boolean allClosed(final LocationCollection locations, final TramDate date, final TimeRange tramRange) {
        return locations.locationStream().allMatch(location -> isClosed(location, date, tramRange));
    }

    private boolean isStationClosed(final Station station, final TramDate date, final TimeRange timeRange) {
        return closedStationContainer.isStationClosed(station.getId(), date, timeRange);
    }

    public boolean hasClosuresOn(final TramDate date) {
        return closedStationContainer.hasClosuresOn(date);
    }

    public boolean isStationClosed(final IdFor<Station> id, final TramDate date) {
        return closedStationContainer.isStationClosed(id, date);
    }

    public ClosedStation getClosedStation(final Location<?> location, final TramDate date, final TimeRange timeRange) {
        return closedStationContainer.getClosedStation(location, date, timeRange);
    }

    public Set<Closure> getClosuresFor(final TramDate date) {
        return closedStationContainer.getClosuresFor(date);
    }

    public static class ClosedStationContainer {
        private final Map<DataSourceID, IdSet<Station>> forDatasource;
        private final Map<IdFor<Station>, ClosedStation> closedStations;
        private final Map<IdFor<Station>, Set<Closure>> closuresForStation;
        private final ClosedStationFactory closedStationFactory1;
        private final GraphFilter graphFilter1;

        public ClosedStationContainer(ClosedStationFactory closedStationFactory, GraphFilter graphFilter) {
            closedStationFactory1 = closedStationFactory;
            graphFilter1 = graphFilter;
            closuresForStation = new HashMap<>();
            closedStations = new HashMap<>();
            forDatasource = new HashMap<>();
        }

        public void clear() {
            forDatasource.clear();
            closedStations.clear();
            closuresForStation.clear();
        }

        private void add(StationClosures closureConfig, final Station station) {
            final IdFor<Station> stationId = station.getId();
            final DataSourceID dataSourceID = station.getDataSourceID();

            final ClosedStation closedStation = closedStationFactory1.createClosedStation(closureConfig, stationId,
                    diversionStation -> shouldIncludeDiversion(diversionStation, closureConfig.getDateRange()));
            closedStations.put(stationId, closedStation);
            if (!forDatasource.containsKey(dataSourceID)) {
                forDatasource.put(dataSourceID, new IdSet<>());
            }
            forDatasource.get(dataSourceID).add(stationId);
        }

        public void addStationsFor(final Closure closure, final Iterable<IdFor<Station>> configStationIds) {
            configStationIds.forEach(stationId -> add(stationId, closure));
        }

        private void add(final IdFor<Station> stationId, final Closure closure) {
            if (!closuresForStation.containsKey(stationId)) {
                closuresForStation.put(stationId, new HashSet<>());
            }
            final Set<Closure> existingClosuresForStation = closuresForStation.get(stationId);
            guardAgainstOverlap(existingClosuresForStation, closure);
            existingClosuresForStation.add(closure);
        }

        public Set<ClosedStation> getFor(final DataSourceID sourceId) {
            if (!forDatasource.containsKey(sourceId)) {
                logger.info("No closures for " + sourceId);
                return Collections.emptySet();
            }

            final IdSet<Station> closedStationsIds = forDatasource.get(sourceId);
            return closedStationsIds.stream().map(closedStations::get).collect(Collectors.toSet());

//        return closedStations.stream().
//                filter(closedStation -> closedStation.getStation().getDataSourceID().equals(sourceId)).
//                collect(Collectors.toSet());
        }

        private boolean shouldIncludeDiversion(final Station diversionStation, final DateRange dateRange) {
            if (graphFilter1.shouldInclude(diversionStation)) {
                final IdFor<Station> stationId = diversionStation.getId();
                if (closuresForStation.containsKey(stationId)) {
                    // diversion station also has closures, make sure not closed on same dates
                    final Set<Closure> closures = closuresForStation.get(stationId);
                    boolean noOverlap = closures.stream().noneMatch(closure -> closure.overlapsWith(dateRange));
                    if (!noOverlap) {
                        logger.debug(format("Diversion station %s has overlapping closures %s with %s",
                                stationId, closures, dateRange));
                    }
                    return noOverlap;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        }

        public int countStations() {
            return closedStations.size();
        }

        private boolean isStationClosed(final IdFor<Station> stationId, final TramDate date, final TimeRange timeRange) {
            if (closuresForStation.containsKey(stationId)) {
                return closuresForStation.get(stationId).
                        stream().anyMatch(closure -> closure.activeFor(date, timeRange));
            }
            return false;
        }

        public boolean isStationClosed(final IdFor<Station> stationId, final TramDate date) {
            if (closuresForStation.containsKey(stationId)) {
                return closuresForStation.get(stationId).stream().anyMatch(closure -> closure.activeFor(date));
            }
            return false;
        }

        public Set<Closure> getClosuresFor(final TramDate date) {
            return closuresForStation.values().stream().
                    flatMap(Collection::stream).
                    filter(closure -> closure.activeFor(date)).
                    collect(Collectors.toSet());
        }

        private void guardAgainstOverlap(final Set<Closure> closures, final Closure candidate) {
            final Set<Closure> overlapping = closures.stream().
                    filter(closure -> closure.overlapsWith(candidate)).
                    collect(Collectors.toSet());
            if (overlapping.isEmpty()) {
                return;
            }

            final String msg = format("Cannot add closure %s since overlaps with existing %s", candidate, overlapping);
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        public boolean hasClosuresOn(final TramDate date) {
            return closuresForStation.values().stream().flatMap(Collection::stream).anyMatch(closure -> closure.activeFor(date));
        }

        public ClosedStation getClosedStation(final Location<?> location, final TramDate date, final TimeRange timeRange) {
            guardForStationOnly(location);

            final Station station = (Station) location;
            final IdFor<Station> stationId = station.getId();
            if (closedStations.containsKey(stationId)) {
                final ClosedStation closed = closedStations.get(stationId);
                final DateTimeRange dateTimeRange = closed.getDateTimeRange();
                if (dateTimeRange.contains(date) && dateTimeRange.fullyContains(timeRange)) {
                    return closed;
                } else {
                    logger.warn("Found closed station " + station + " but did not match " + date + " and " + timeRange);
                }
            } else {
                logger.warn("Did not find a closed station for " + stationId);
            }
//        final Optional<ClosedStation> maybe = closedStations.stream().
//                filter(closedStation -> closedStation.getDateTimeRange().contains(date)).
//                filter(closedStation -> closedStation.getStationId().equals(station.getId())).
//                filter(closedStation -> closedStation.getDateTimeRange().fullyContains(timeRange)).
//                findFirst();
//
//        if (maybe.isEmpty()) {
            String msg = station.getId() + " is not closed on " + date;
            logger.error(msg);
            throw new RuntimeException(msg);
//        }
//
//        return maybe.get();

        }

        public Set<ClosedStation> getAnyWithClosure(TramDate date) {
            return closedStations.values().stream().
                    filter(closedStation -> closedStation.getDateTimeRange().getDateRange().contains(date)).collect(Collectors.toSet());
        }

        public boolean anyStationOpen(final LocationSet<Station> locations, TramDate date) {
            if (!hasClosuresOn(date)) {
                return true;
            }

            final IdSet<Station> locationsWithAClosure = locations.stream().
                    map(HasId::getId).
                    filter(closuresForStation::containsKey).
                    collect(IdSet.idCollector());

            // at least one of the provided did not have a related closure
            if (locationsWithAClosure.size() != locations.size()) {
                return true;
            }
            // ELSE all locations have a closure, so check if any of those are not all day long

            return locationsWithAClosure.stream().
                    filter(closedStations::containsKey).
                    map(closedStations::get).
                    anyMatch(closedStation -> !closedStation.closedWholeDay());


//        return closedStations.stream().
//                filter(closedStation -> locationsWithAClosure.contains(closedStation.getStationId())).
//                anyMatch(closedStation -> !closedStation.closedWholeDay());


        }

    }


}
