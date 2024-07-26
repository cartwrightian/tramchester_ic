package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.filters.GraphFilter;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class ClosedStationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClosedStationsRepository.class);

    private final Set<ClosedStation> closedStations;
//    private final IdSet<Station> hasXAClosure;
    private final Map<IdFor<Station>, Set<DateRange>> closureDates;
    private final TramchesterConfig config;
    private final StationRepository stationRepository;
    private final StationLocations stationLocations;
    private final GraphFilter filter;

    @Inject
    public ClosedStationsRepository(TramchesterConfig config, StationRepository stationRepository, StationLocations stationLocations,
                                    GraphFilter filter) {
        this.config = config;
        this.stationRepository = stationRepository;
        this.stationLocations = stationLocations;

        this.filter = filter;
        closureDates = new HashMap<>();
        closedStations = new HashSet<>();
//        closed = new HashSet<>();
//        hasAClosure = new IdSet<>();
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

    private void captureClosedStationsFromConfig(final Set<StationClosures> closures) {
        final MarginInMeters range = config.getWalkingDistanceRange();

        // capture closed stations and date ranges
        closures.forEach(closure -> {
            DateRange dateRange = closure.getDateRange();
            closure.getStations().forEach(stationId -> {
                if (!closureDates.containsKey(stationId)) {
                    closureDates.put(stationId, new HashSet<>());
                }
                closureDates.get(stationId).add(dateRange);
            });
        });

        // details
        closures.forEach(closure -> {
            final boolean needNearby = (!closure.hasDiversionsAroundClosure()) || (!closure.hasDiversionsToFromClosure());
            final Set<ClosedStation> toAdd = closure.getStations().stream().
                    map(stationId -> createClosedStation(closure, stationId, range, needNearby)).
                    collect(Collectors.toSet());
            closedStations.addAll(toAdd);
        });
    }

    private ClosedStation createClosedStation(StationClosures closure, IdFor<Station> stationId,
                                              MarginInMeters margin, boolean needNearby) {
        final Station station = stationRepository.getStationById(stationId);
        final DateRange dateRange = closure.getDateRange();

        // potentially expensive, only populate if needed
        final Set<Station> nearbyOpenStations = needNearby ? getNearbyOpenStations(station, margin, dateRange) : Collections.emptySet();

        final boolean fullyClosed = closure.isFullyClosed();

//        hasAClosure.add(stationId);

        final Set<Station> diversionsAround;
        if (closure.hasDiversionsAroundClosure()) {
            final IdSet<Station> diversionsAroundClosure = closure.getDiversionsAroundClosure();
            logger.info(format("Using config provided stations %s for diversions around closed station %s", diversionsAroundClosure, stationId));
            diversionsAround = getStations(diversionsAroundClosure);
        } else {
            logger.info(format("Using discovered nearby stations %s for diversions around closed station %s", HasId.asIds(nearbyOpenStations), stationId));
            diversionsAround = nearbyOpenStations;
        }

        final Set<Station> diversionsToFrom;
        if (closure.hasDiversionsToFromClosure()) {
            final IdSet<Station> diversionsToFromClosure = closure.getDiversionsToFromClosure();
            logger.info(format("Using config provided stations %s for diversions to/from closed station %s", diversionsToFromClosure, stationId));
            diversionsToFrom = getStations(diversionsToFromClosure);
        } else {
            logger.info(format("Using discovered nearby stations %s for diversions to/from closed station %s", HasId.asIds(nearbyOpenStations), stationId));
            diversionsToFrom = nearbyOpenStations;
        }

        return new ClosedStation(station, dateRange, fullyClosed, diversionsAround, diversionsToFrom);

    }

    private Set<Station> getStations(IdSet<Station> stationIds) {
        return stationIds.stream().map(stationRepository::getStationById).collect(Collectors.toSet());
    }

    private Set<Station> getNearbyOpenStations(final Station station, final MarginInMeters range, final DateRange dateRange) {
        final Stream<Station> withinRange = stationLocations.nearestStationsUnsorted(station, range);

        final Set<Station> found = withinRange.
                filter(filter::shouldInclude).
                filter(nearby -> !nearby.equals(station)).
                filter(nearby -> openForRange(nearby.getId(), dateRange)).
                collect(Collectors.toSet());

        logger.info("Found " + found.size() + " stations linked and within range of " + station.getId());

        return found;
    }

    private boolean openForRange(final IdFor<Station> stationId, final DateRange dateRange) {
        if (closureDates.containsKey(stationId)) {
            final Set<DateRange> closedRanges = closureDates.get(stationId);
            return closedRanges.stream().noneMatch(dateRange::overlapsWith);
        }
        return true;
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
//        closed.clear();
//        hasAClosure.clear();
        closureDates.clear();
        logger.info("Stopped");
    }

    public Set<ClosedStation> getFullyClosedStationsFor(final TramDate date) {
        return getClosures(date, true).collect(Collectors.toSet());
    }

    private Stream<ClosedStation> getClosures(TramDate date, boolean fullyClosed) {
        return closedStations.stream().
                filter(closure -> closure.isFullyClosed() == fullyClosed).
                filter(closure -> closure.getDateRange().contains(date));
    }

    public boolean hasClosuresOn(final TramDate date) {
        return closureDates.values().stream().flatMap(Collection::stream).anyMatch(dateRange -> dateRange.contains(date));
//        return getClosures(date, true).findAny().isPresent() || getClosures(date, false).findAny().isPresent();
    }

    public Set<ClosedStation> getClosedStationsFor(final DataSourceID sourceId) {
        return closedStations.stream().
                filter(closedStation -> closedStation.getStation().getDataSourceID().equals(sourceId)).
                collect(Collectors.toSet());
    }

    public boolean isClosed(final Location<?> location, final TramDate date) {
        return switch (location.getLocationType()) {
            case Station -> isStationClosed((Station)location, date);
            case StationGroup -> isGroupClosed((StationGroup)location, date);
            case Platform -> isPlatformClosed((Platform)location, date);
            case Postcode, MyLocation -> false;
        };
    }

    private boolean isPlatformClosed(Platform platform, TramDate date) {
        return isStationClosed(platform.getStation(), date);
    }

    private boolean isStationClosed(Station station, TramDate date) {
        return isClosed(station.getId(), date);
    }

    public boolean isGroupClosed(final StationGroup group, final TramDate date) {
        return allClosed(group.getAllContained(), date);
    }

    public boolean allClosed(final LocationCollection locations, TramDate date) {
        return locations.locationStream().allMatch(location -> isClosed(location, date));
    }

    public boolean isClosed(IdFor<Station> stationId, TramDate date) {
        // todo maybe pre-compute by date as well?
        if (!closureDates.containsKey(stationId)) {
            return false;
        }

        final Set<DateRange> forStation = closureDates.get(stationId);

        return forStation.stream().anyMatch(range -> range.contains(date));

//        return closed.stream().
//                filter(closedStation -> closedStation.getStationId().equals(stationId)).
//                anyMatch(closedStation -> closedStation.getDateRange().contains(date));
    }

    public ClosedStation getClosedStation(final Location<?> location, final TramDate date) {
        if (location.getLocationType()!=LocationType.Station) {
            String msg = "Not a station " + location.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        final Station station = (Station) location;

        final Optional<ClosedStation> maybe = closedStations.stream().
                filter(closedStation -> closedStation.getStationId().equals(station.getId())).
                filter(closedStation -> closedStation.getDateRange().contains(date)).
                findFirst();

        if (maybe.isEmpty()) {
            String msg = station.getId() + " is not closed on " + date;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        return maybe.get();
    }

//    public boolean isFullyClosed(Station station, TramDate date) {
//        if (!hasClosuresOn(date)) {
//            return false;
//        }
//        if (!hasAClosure.contains(station.getId())) {
//            return false;
//        }
//        ClosedStation closedStation = getClosedStation(station, date);
//        return closedStation.isFullyClosed();
//    }

    public boolean anyStationOpen(final LocationSet<Station> locations, final TramDate date) {
        if (!hasClosuresOn(date)) {
            return true;
        }

        final Set<Station> haveAClosure = locations.stream().
                filter(station -> closureDates.containsKey(station.getId())).
                collect(Collectors.toSet());

        if (haveAClosure.size() != locations.size()) {
            return true;
        }

        // count have many closed are fully closed
        long fullyClosedCount = haveAClosure.stream().
                map(station -> getClosedStation(station, date)).
                filter(ClosedStation::isFullyClosed).
                count();

        // all fully closed
        return fullyClosedCount!=haveAClosure.size();

    }
}
