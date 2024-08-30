package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.closures.ClosedStationFactory;
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

    private final Set<ClosedStation> closedStations;
    private final Map<IdFor<Station>, Set<DateRange>> closureDates;
    private final TramchesterConfig config;
    private final ClosedStationFactory closedStationFactory;
    private final GraphFilter graphFilter;

    @Inject
    public ClosedStationsRepository(TramchesterConfig config, ClosedStationFactory closedStationFactory, GraphFilter graphFilter) {
        this.config = config;
        this.closedStationFactory = closedStationFactory;
        this.graphFilter = graphFilter;
        closureDates = new HashMap<>();
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
        logger.warn("Added " + closureDates.size() + " stations closures");
        logger.info("Started");
    }

    public void captureClosedStationsFromConfig(final Set<StationClosures> closures) {

        logger.info("Capturing closures from " + closures.size() + " config elements");

        // capture closed stations and date ranges
        closures.forEach(closure -> {
            final DateRange dateRange = closure.getDateRange();
            closure.getStations().forEach(stationId -> {
                if (!closureDates.containsKey(stationId)) {
                    closureDates.put(stationId, new HashSet<>());
                }
                closureDates.get(stationId).add(dateRange);
            });
        });

        // capture details of each closure
        closures.forEach(closure -> {
            final Set<ClosedStation> toAdd = closure.getStations().stream().
                    map(closedStationId -> closedStationFactory.createClosedStation(closure, closedStationId,
                            diversionStation -> shouldIncludeDiversion(diversionStation, closure.getDateRange()))).
                    collect(Collectors.toSet());
            guardAgainstOverlap(toAdd);
            closedStations.addAll(toAdd);
        });
    }


    private void guardAgainstOverlap(final Set<ClosedStation> toAdd) {
        toAdd.forEach(toCheck -> {
            final Station station = toCheck.getStation();
            closedStations.stream().
                    filter(closedStation -> closedStation.getStation().equals(station)).
                    forEach(sameStationClosure -> {
                        if (sameStationClosure.overlaps(toCheck)) {
                            String msg = format("Cannot add closure for %s since closure %s overlaps with existing %s", station.getId(), sameStationClosure, toCheck);
                            logger.error(msg);
                            throw new RuntimeException(msg);
                        }
                    });
        });
    }

    private boolean shouldIncludeDiversion(final Station diversionStation, final DateRange dateRange) {
        if (graphFilter.shouldInclude(diversionStation)) {
            final IdFor<Station> stationId = diversionStation.getId();
            if (closureDates.containsKey(stationId)) {
                // diversion station also has closures, make sure not closed on same dates
                final Set<DateRange> closedRanges = closureDates.get(stationId);
                boolean noOverlap = closedRanges.stream().noneMatch(dateRange::overlapsWith);
                if (!noOverlap) {
                    logger.debug(format("Diversion station %s has overlapping closures %s with %s",
                            stationId, closedRanges, dateRange));
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
        closureDates.clear();
        logger.info("Stopped");
    }

    public boolean hasClosuresOn(final TramDate date) {
        return closureDates.values().stream().flatMap(Collection::stream).anyMatch(dateRange -> dateRange.contains(date));
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
        return isClosed(station.getId(), date);
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
                filter(closureDates::containsKey).
                collect(IdSet.idCollector());

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
        final IdSet<Station> hasClosure = closureDates.entrySet().stream().
                filter(entry -> entry.getValue().stream().anyMatch(range -> range.contains(tramDate))).
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

    private boolean isStationClosed(Station station, TramDate date, TimeRange timeRange) {
        final boolean closedOnDay = isStationClosed(station, date);
        if (!closedOnDay) {
            return false;
        }
        // else we know station has a closure
        if (timeRange.allDay()) {
            return true;
        }
        // else we need to check if timeRange is fully enclosed by the closures in effect today
        final List<ClosedStation> foundClosures = closedStations.stream().
                filter(closedStation -> closedStation.getStation().equals(station)).
                filter(closedStation -> closedStation.getDateTimeRange().contains(date)).
                toList();

        if (foundClosures.size()!=1) {
            String msg = format("Got the wrong number of closures for %s on date %s, found %s", station.getId(), date, foundClosures);
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        ClosedStation found = foundClosures.get(0);
        return found.getDateTimeRange().fullyContains(timeRange);
    }

    public boolean isClosed(final IdFor<Station> stationId, final TramDate date) {
        if (!closureDates.containsKey(stationId)) {
            // none match
            return false;
        }

        final Set<DateRange> forStation = closureDates.get(stationId);
        return forStation.stream().anyMatch(range -> range.contains(date));

    }
}
