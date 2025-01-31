package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.StationListConfig;
import com.tramchester.config.StationsConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.closures.ClosedStationFactory;
import com.tramchester.domain.closures.Closure;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
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
    private final GraphFilter graphFilter;

    private final Set<ClosedStation> closedStations;
    private final Map<IdFor<Station>, Set<Closure>> closuresForStation;

    @Inject
    public ClosedStationsRepository(TramchesterConfig config, ClosedStationFactory closedStationFactory, GraphFilter graphFilter) {
        this.config = config;
        this.closedStationFactory = closedStationFactory;
        this.graphFilter = graphFilter;
        closuresForStation = new HashMap<>();
        closedStations = new HashSet<>();
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
        logger.warn("Added " + closuresForStation.size() + " stations closures");
        logger.info("Started");
    }

    public void captureClosedStationsFromConfig(final Set<StationClosures> closureConfigs) {

        logger.info("Capturing closures from " + closureConfigs.size() + " config elements");

        // capture closed stations and date ranges
        closureConfigs.forEach(closureConfig -> {
            final Closure closure = closedStationFactory.createFor(closureConfig);
            final StationsConfig stations = closureConfig.getStations();
            if (stations instanceof StationListConfig stationListConfig) {
                stationListConfig.getStations().forEach(stationId -> {
                    if (!closuresForStation.containsKey(stationId)) {
                        closuresForStation.put(stationId, new HashSet<>());
                    }
                    final Set<Closure> closures = closuresForStation.get(stationId);
                    guardAgainstOverlap(closures, closure);
                    closures.add(closure);
                });
            } else {
                throw new RuntimeException("TODO");
            }

        });

        // capture details of each closure
        closureConfigs.forEach(closure -> {
                    final Set<ClosedStation> toAdd = toClosedStations(closure);
                    closedStations.addAll(toAdd);
                });
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

    private Set<ClosedStation> toClosedStations(final StationClosures closureConfig) {
        final StationsConfig stationClosureConfig = closureConfig.getStations();

        if (stationClosureConfig instanceof StationListConfig stationListConfig) {
            return stationListConfig.getStations().stream().
                    map(closedStationId -> closedStationFactory.createClosedStation(closureConfig, closedStationId,
                    diversionStation -> shouldIncludeDiversion(diversionStation, closureConfig.getDateRange()))).
                    collect(Collectors.toSet());
        } else {
            throw new RuntimeException("todo for " + closureConfig);
        }
    }

    private boolean shouldIncludeDiversion(final Station diversionStation, final DateRange dateRange) {
        if (graphFilter.shouldInclude(diversionStation)) {
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

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        closuresForStation.clear();
        logger.info("Stopped");
    }

    public boolean hasClosuresOn(final TramDate date) {
        return closuresForStation.values().stream().flatMap(Collection::stream).anyMatch(closure -> closure.activeFor(date));
    }

    public Set<ClosedStation> getClosedStationsFor(final DataSourceID sourceId) {
        return closedStations.stream().
                filter(closedStation -> closedStation.getStation().getDataSourceID().equals(sourceId)).
                collect(Collectors.toSet());
    }

    private boolean isPlatformClosed(Platform platform, TramDate date) {
        return isStationClosed(platform.getStation(), date);
    }

    private boolean isPlatformClosed(Platform platform, TramDate date, TimeRange timeRange) {
        return isStationClosed(platform.getStation(), date, timeRange);
    }

    private boolean isStationClosed(Station station, TramDate date) {
        return isStationClosed(station.getId(), date);
    }

    public boolean isGroupClosed(final StationGroup group, final TramDate date) {
        return allClosed(group.getAllContained(), date);
    }

    public boolean isGroupClosed(final StationGroup group, final TramDate date, TimeRange timeRange) {
        if (timeRange.allDay()) {
            return allClosed(group.getAllContained(), date);
        }
        else {
            return allClosed(group.getAllContained(), date, timeRange);
        }
    }

    public boolean allClosed(final LocationCollection locations, TramDate date) {
        return locations.locationStream().allMatch(location -> isClosed(location, date));
    }

    private boolean allClosed(final LocationCollection locations, TramDate date, TimeRange tramRange) {
        return locations.locationStream().allMatch(location -> isClosed(location, date, tramRange));
    }

//    /***
//     * Can only have one closure per station per date currently, hence no time here
//     * @param location must be a station
//     * @param date the date we need to check
//     * @return ClosedStation
//     */
//    @Deprecated
//    public ClosedStation getClosedStation(final Location<?> location, final TramDate date, TramTime time) {
//        guardForStationOnly(location);
//        final Station station = (Station) location;
//
//        final Optional<ClosedStation> maybe = closedStations.stream().
//                filter(closedStation -> closedStation.getStationId().equals(station.getId())).
//                filter(closedStation -> closedStation.getDateTimeRange().contains(date, time)).
//                findFirst();
//
//        if (maybe.isEmpty()) {
//            String msg = station.getId() + " is not closed on " + date;
//            logger.error(msg);
//            throw new RuntimeException(msg);
//        }
//
//        return maybe.get();
//    }


    public ClosedStation getClosedStation(final Location<?> location, final TramDate date, final TimeRange timeRange) {
        guardForStationOnly(location);

        final Station station = (Station) location;
        final Optional<ClosedStation> maybe = closedStations.stream().
                filter(closedStation -> closedStation.getDateTimeRange().contains(date)).
                filter(closedStation -> closedStation.getStationId().equals(station.getId())).
                filter(closedStation -> closedStation.getDateTimeRange().fullyContains(timeRange)).
                findFirst();

        if (maybe.isEmpty()) {
            String msg = station.getId() + " is not closed on " + date;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        return maybe.get();

    }

    private static void guardForStationOnly(Location<?> location) {
        if (location.getLocationType()!=LocationType.Station) {
            String msg = "Not a station " + location.getId() + " " + location.getLocationType();
            logger.error(msg);
            throw new RuntimeException(msg);
        }
    }

    public boolean anyStationOpen(final LocationSet<Station> locations, final TramDate date) {
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


        return closedStations.stream().
                filter(closedStation -> locationsWithAClosure.contains(closedStation.getStationId())).
                anyMatch(closedStation -> !closedStation.closedWholeDay());

//        // count have many closed are fully closed
//        long fullyClosedCount = locationsWithAClosure.stream().
//                map(station -> getClosedStation(station, date)).
//                filter(ClosedStation::isFullyClosed).
//                count();
//
//        // all fully closed
//        return fullyClosedCount!=locationsWithAClosure.size();

    }

    public Set<ClosedStation> getAnyWithClosure(final TramDate tramDate) {
        final IdSet<Station> hasClosure = closuresForStation.entrySet().stream().
                filter(entry -> entry.getValue().stream().anyMatch(range -> range.activeFor(tramDate))).
                map(Map.Entry::getKey).
                collect(IdSet.idCollector());

       return closedStations.stream().
                filter(closedStation -> hasClosure.contains(closedStation.getStationId())).
                filter(closedStation -> closedStation.getDateTimeRange().contains(tramDate)).
                collect(Collectors.toSet());
    }


    public boolean isClosed(Location<?> location, TramDate date, TimeRange timeRange) {
        return switch (location.getLocationType()) {
            case Station -> isStationClosed((Station)location, date, timeRange);
            case StationGroup -> isGroupClosed((StationGroup)location, date, timeRange);
            case Platform -> isPlatformClosed((Platform)location, date, timeRange);
            case Postcode, MyLocation -> false;
        };
    }

    public boolean isClosed(final Location<?> location, final TramDate date) {
        return switch (location.getLocationType()) {
            case Station -> isStationClosed((Station)location, date);
            case StationGroup -> isGroupClosed((StationGroup)location, date);
            case Platform -> isPlatformClosed((Platform)location, date);
            case Postcode, MyLocation -> false;
        };
    }

    private boolean isStationClosed(final Station station, final TramDate date, final TimeRange timeRange) {
        return isStationClosed(station.getId(), date, timeRange);
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
}
