package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.NeighbourConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.mappers.Geography;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import jakarta.inject.Inject;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Walk;
import static java.lang.String.format;

// TODO Location<?> not Station

@LazySingleton
public class Neighbours implements NeighboursRepository {
    private static final Logger logger = LoggerFactory.getLogger(Neighbours.class);

    // ONLY link stations of different types
    private static final boolean DIFF_MODES_ONLY = true;

    private final StationRepository stationRepository;
    private final StationLocationsRepository stationLocations;
    private final Geography geography;

    private final Map<IdFor<Station>, Set<StationToStationConnection>> neighbours;
    private final boolean enabled;
    private final NeighbourConfig config;

    @Inject
    public Neighbours(StationRepository stationRepository, StationLocationsRepository stationLocations,
                      TramchesterConfig config, Geography geography) {
        this.stationRepository = stationRepository;
        this.stationLocations = stationLocations;
        this.geography = geography;

        enabled = config.hasNeighbourConfig();

        if (enabled) {
            this.config = config.getNeighbourConfig();
        } else {
            this.config = null;
        }
        neighbours = new HashMap<>();
    }

    @PostConstruct
    private void start() {
        if (!enabled) {
            logger.warn("Disabled in config");
            return;
        }
        logger.info("Starting");
        createNeighbours();
        addFromConfig();
        logger.info("Started");
    }

    private void createNeighbours() {
        final MarginInMeters marginInMeters = MarginInMeters.ofKM(config.getDistanceToNeighboursKM());

        logger.info(format("Adding neighbouring stations for range %s and diff modes only %s",
                marginInMeters, DIFF_MODES_ONLY));

        final EnumSet<TransportMode> walk = EnumSet.of(Walk);
        final StationToStationConnection.LinkType linkType = StationToStationConnection.LinkType.Neighbour;

        stationRepository.getActiveStationStream().
                filter(station -> station.getGridPosition().isValid()).
                forEach(begin -> {
                    final EnumSet<TransportMode> beginModes = begin.getTransportModes();
                    // nearby could be any transport mode
                    Set<StationToStationConnection> links = stationLocations.nearestStationsUnsorted(begin, marginInMeters).
                            filter(nearby -> !nearby.equals(begin)).
                            filter(nearby -> DIFF_MODES_ONLY && noOverlapModes(beginModes, nearby.getTransportModes())).
                            map(nearby -> StationToStationConnection.createForWalk(begin, nearby, walk, linkType, geography)).
                            collect(Collectors.toSet());
                    if (!links.isEmpty()) {
                        neighbours.put(begin.getId(), links);
                    }
                });

        logger.info("Added " + neighbours.size() + " station with neighbours");

    }

    private void addFromConfig() {
        List<StationIdPair> additional = config.getAdditional();
        if (additional.isEmpty()) {
            logger.info("No additional neighbours found in config");
        } else {
            logger.info("Attempt to add neighbours for " + additional);
        }

        List<StationPair> toAdd = additional.stream().
                filter(this::bothValid).
                map(stationRepository::getStationPair).
                toList();

        if (additional.size() != toAdd.size()) {
            logger.warn("Not adding all of the requested additional neighbours, some were invalid, check the logs above");
        }

        toAdd.forEach(this::addFromConfig);
    }

    private void addFromConfig(final StationPair pair) {
        final EnumSet<TransportMode> walk = EnumSet.of(Walk);
        final StationToStationConnection.LinkType linkType = StationToStationConnection.LinkType.Neighbour;

        final Station begin = pair.getBegin();
        final Station end = pair.getEnd();

        if (areNeighbours(begin, end)) {
            logger.warn("Config contains pair that were already present as neighbours, skipping " + pair);
            return;
        }

        logger.info("Adding " + pair + " as neighbours");

        final Quantity<Length> distance = geography.getDistanceBetweenInMeters(begin, end);
        final Duration walkingDuration = geography.getWalkingDuration(begin, end);

        addNeighbour(begin, new StationToStationConnection(begin, end, walk, linkType, distance, walkingDuration));
        addNeighbour(end, new StationToStationConnection(end, begin, walk, linkType, distance, walkingDuration));
    }

    void addNeighbour(final Station startStation, final StationToStationConnection link) {
        final IdFor<Station> startStationId = startStation.getId();
        if (neighbours.containsKey(startStationId)) {
            neighbours.get(startStationId).add(link);
        } else {
            Set<StationToStationConnection> links = new HashSet<>();
            links.add(link);
            neighbours.put(startStationId, links);
        }
    }

    private boolean bothValid(final StationIdPair stationIdPair) {
        if (!stationRepository.hasStationId(stationIdPair.getBeginId())) {
            logger.warn(format("begin station id for pair %s is invalid", stationIdPair));
            return false;
        }
        if (!stationRepository.hasStationId(stationIdPair.getEndId())) {
            logger.warn(format("end station id for pair %s is invalid", stationIdPair));
            return false;
        }
        return true;
    }

    @PreDestroy
    private void stop() {
        logger.info("Stopping");
        neighbours.clear();
        logger.info("stopped");
    }

    @Override
    public Set<StationToStationConnection> getAll() {
        return neighbours.values().stream().flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<Station> getNeighboursFor(final IdFor<Station> id) {
        return neighbours.get(id).stream().map(StationToStationConnection::getEnd).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<StationToStationConnection> getNeighbourLinksFor(final IdFor<Station> id) {
        return neighbours.get(id);
    }

    @Override
    public boolean hasNeighbours(final IdFor<Station> id) {
        return neighbours.containsKey(id);
    }

    @Override
    public boolean areNeighbours(final Location<?> start, final Location<?> destination) {
        if (start.getLocationType()==LocationType.Station && destination.getLocationType()==LocationType.Station) {
            final IdFor<Station> startId = StringIdFor.convert(start.getId(), Station.class);
            final IdFor<Station> destinationId = StringIdFor.convert(destination.getId(), Station.class);
            if (!hasNeighbours(startId)) {
                return false;
            }
            return getNeighboursFor(startId).stream().anyMatch(neighbourId -> destinationId.equals(neighbourId.getId()));
        }
        return false;
    }

    @Override
    public boolean areNeighbours(final LocationSet<Station> starts, final LocationSet<Station> destinations) {
        return starts.stream().
                map(Location::getId).
                filter(this::hasNeighbours).
                map(this::getNeighboursFor).
                anyMatch(neighbours -> destinations.stream().anyMatch(neighbours::contains));
    }

    @Override
    public boolean differentModesOnly() {
        return DIFF_MODES_ONLY;
    }



    private boolean noOverlapModes(EnumSet<TransportMode> modesA, EnumSet<TransportMode> modesB) {
        return !TransportMode.intersects(modesA, modesB);
//        boolean aNotInB = modesA.stream().noneMatch(modesB::contains);
//        boolean bNotInA = modesB.stream().noneMatch(modesA::contains);
//        return aNotInB && bNotInA;
    }
}
