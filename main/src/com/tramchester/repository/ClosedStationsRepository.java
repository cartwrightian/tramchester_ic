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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class ClosedStationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClosedStationsRepository.class);

    private final Set<ClosedStation> closed;
    private final IdSet<Station> hasAClosure;
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
        closed = new HashSet<>();
        hasAClosure = new IdSet<>();
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
        logger.warn("Added " + closed.size() + " stations closures");
        logger.info("Started");
    }

    private void captureClosedStationsFromConfig(final Set<StationClosures> closures) {
        final MarginInMeters range = config.getWalkingDistanceRange();

        closures.forEach(closure -> {
            final DateRange dateRange = closure.getDateRange();
            final boolean fullyClosed = closure.isFullyClosed();
            final IdSet<Station> diversionsOnly = closure.getDiversionsOnly();
            Set<ClosedStation> closedStations = closure.getStations().stream().
                    map(stationId -> createClosedStation(stationId, dateRange, fullyClosed, range, diversionsOnly)).
                    collect(Collectors.toSet());
            closed.addAll(closedStations);
        });
    }

    private ClosedStation createClosedStation(IdFor<Station> stationId, DateRange dateRange, boolean fullyClosed,
                                              MarginInMeters range, IdSet<Station> diversionsOnly) {
        hasAClosure.add(stationId);
        final Station station = stationRepository.getStationById(stationId);

        if (diversionsOnly.isEmpty()) {
            Set<Station> nearbyOpenStations = getNearbyStations(station, range);
            logger.info("Only nearby station diversions " + HasId.asIds(nearbyOpenStations) + " for closed station " + station.getId());

            return new ClosedStation(station, dateRange, fullyClosed, nearbyOpenStations);
        } else {
            Set<Station> diversions = diversionsOnly.stream().map(stationRepository::getStationById).collect(Collectors.toSet());
            logger.info("Only adding diversions " + HasId.asIds(diversions) + " for closed station " + station.getId());
            return new ClosedStation(station, dateRange, fullyClosed, diversions);
        }

    }

    private Set<Station> getNearbyStations(Station station, MarginInMeters range) {
        final Stream<Station> withinRange = stationLocations.nearestStationsUnsorted(station, range);

        final Set<Station> found = withinRange.
                filter(filter::shouldInclude).
                filter(nearby -> !nearby.equals(station)).
                collect(Collectors.toSet());

        logger.info("Found " + found.size() + " stations linked and within range of " + station.getId());

        return found;
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        closed.clear();
        hasAClosure.clear();
        logger.info("Stopped");
    }

    public Set<ClosedStation> getFullyClosedStationsFor(final TramDate date) {
        return getClosures(date, true).collect(Collectors.toSet());
    }

    private Stream<ClosedStation> getClosures(TramDate date, boolean fullyClosed) {
        return closed.stream().
                filter(closure -> closure.isFullyClosed() == fullyClosed).
                filter(closure -> closure.getDateRange().contains(date));
    }

    public boolean hasClosuresOn(final TramDate date) {
        // todo maybe pre-compute by date as well?
        return getClosures(date, true).findAny().isPresent() || getClosures(date, false).findAny().isPresent();
    }

    public Set<ClosedStation> getClosedStationsFor(DataSourceID sourceId) {
        return closed.stream().
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
        if (!hasAClosure.contains(stationId)) {
            return false;
        }

        return closed.stream().
                filter(closedStation -> closedStation.getStationId().equals(stationId)).
                anyMatch(closedStation -> closedStation.getDateRange().contains(date));
    }

    public ClosedStation getClosedStation(final Location<?> location, final TramDate date) {
        if (location.getLocationType()!=LocationType.Station) {
            String msg = "Not a station " + location.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        final Station station = (Station) location;

        final Optional<ClosedStation> maybe = closed.stream().
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

    public boolean isFullyClosed(Station station, TramDate date) {
        if (!hasClosuresOn(date)) {
            return false;
        }
        if (!hasAClosure.contains(station.getId())) {
            return false;
        }
        ClosedStation closedStation = getClosedStation(station, date);
        return closedStation.isFullyClosed();
    }

    public boolean anyStationOpen(final LocationSet<Station> locations, final TramDate date) {
        if (!hasClosuresOn(date)) {
            return true;
        }

        final Set<Station> haveAClosure = locations.stream().
                filter(station -> hasAClosure.contains(station.getId())).
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
