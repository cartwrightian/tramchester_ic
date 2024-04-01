package com.tramchester.geo;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.MixedLocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.naptan.NaptanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import jakarta.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@LazySingleton
public class StationLocations implements StationLocationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationLocations.class);

    private static final int DEPTH_LIMIT = 20;
    private static final int GRID_SIZE_METERS = 1000;

    private final StationRepository stationRepository;
    private final PlatformRepository platformRepository;
    private final NaptanRepository naptanRespository;
    private final Geography geography;
    private final Set<BoundingBox> quadrants;

    private final Map<IdFor<NPTGLocality>, MixedLocationSet> locationsWithinLocality;
    private final Map<BoundingBox, Set<Station>> stationBoxes;

    private BoundingBox bounds;

    @Inject
    public StationLocations(StationRepository stationRepository, PlatformRepository platformRepository,
                            NaptanRepository naptanRespository, Geography geography) {
        this.stationRepository = stationRepository;
        this.platformRepository = platformRepository;
        this.naptanRespository = naptanRespository;
        this.geography = geography;

        quadrants = new HashSet<>();
        stationBoxes = new HashMap<>();
        locationsWithinLocality = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        bounds = new CreateBoundingBox().createBoundingBox(stationRepository.getActiveStationStream());
        logger.info("Created bounds for active stations: " + bounds);
        createQuadrants();
        if (naptanRespository.isEnabled()) {
            populateLocality();
        } else {
            logger.warn("Naptan repository is disabled, no area data will be populated");
        }
        logger.info("started");
    }

    @PreDestroy
    public void dispose() {
        logger.info("Stopping");
        stationBoxes.values().forEach(Set::clear);
        stationBoxes.clear();
        quadrants.clear();
        locationsWithinLocality.clear();
        logger.info("Stopped");
    }

    private void createQuadrants() {
        final Set<Station> allStations = stationRepository.getActiveStationStream().
                filter(station -> station.getGridPosition().isValid()).
                collect(Collectors.toSet());
        populateQuadrants(bounds, DEPTH_LIMIT, allStations);
        logger.info("Discovered " + quadrants.size() + " quadrants");
    }

    private void populateQuadrants(final BoundingBox box, final int depthLimit, Set<Station> inside) {
        final int currentLimit = depthLimit - 1;

        if (currentLimit<=0 || box.width() <= GRID_SIZE_METERS || box.height() <= GRID_SIZE_METERS) {
            logger.debug("Added " + box);
            quadrants.add(box);
            stationBoxes.put(box, inside);
            return;
        }

        // break current box into quadrants, but only if quadrant contains stations
        final Set<BoundingBox> newQuadrants = box.quadrants();
        newQuadrants.forEach(quadrant -> {
            Set<Station> insideNewQuadrant = inside.stream().
                    filter(station -> quadrant.contained(station.getGridPosition())).
                    collect(Collectors.toSet());
            if (!insideNewQuadrant.isEmpty()) {
                populateQuadrants(quadrant, currentLimit, insideNewQuadrant);
            }
        });
    }

    private void populateLocality() {
        stationRepository.getActiveStationStream().
                filter(location -> location.getLocalityId().isValid()).
                collect(Collectors.groupingBy(Location::getLocalityId)).entrySet()
                .stream().
                filter(entry -> !entry.getValue().isEmpty()).
                forEach(entry -> locationsWithinLocality.put(entry.getKey(), new MixedLocationSet(entry.getValue())));

        platformRepository.getPlaformStream().
                filter(Location::isActive).
                collect(Collectors.groupingBy(Location::getLocalityId)).entrySet()
                .stream().
                filter(entry -> !entry.getValue().isEmpty()).
                forEach(entry -> updateLocations(entry.getKey(), new LocationSet<>(entry.getValue())));

        logger.info("Added " + locationsWithinLocality.size() + " areas which have stations");
    }

    private void updateLocations(final IdFor<NPTGLocality> localityIdFor, final LocationCollection toAdd) {
        if (locationsWithinLocality.containsKey(localityIdFor)) {
            locationsWithinLocality.get(localityIdFor).addAll(toAdd);
        } else {
            locationsWithinLocality.put(localityIdFor, new MixedLocationSet(toAdd));
        }
    }

    public BoundingBox getActiveStationBounds() {
        return bounds;
    }

    @Override
    public LocationCollection getLocationsWithin(IdFor<NPTGLocality> areaId) {
        return locationsWithinLocality.get(areaId);
    }


    /***
     * Uses Latitude/Longitude and EPSG
     * @return A list of points on convex hull containing all stations
     */
    public List<LatLong> getBoundaryForStations() {
        Stream<LatLong> points = stationRepository.getAllStationStream().
                map(Station::getLatLong);

        return geography.createBoundaryFor(points);
    }

    @Override
    public boolean hasStationsOrPlatformsIn(IdFor<NPTGLocality> areaId) {
        // map only populated if an area does contain a station or platform
        return locationsWithinLocality.containsKey(areaId);
    }

    @Override
    public boolean withinBounds(Location<?> location) {
        return getActiveStationBounds().contained(location);
    }

    public Set<BoundingBox> getQuadrants() {
        return quadrants;
    }

    @Override
    public List<Station> nearestStationsSorted(Location<?> origin, int maxToFind, MarginInMeters rangeInMeters,
                                               EnumSet<TransportMode> modes) {
        return nearestStationsSorted(origin.getGridPosition(), maxToFind, rangeInMeters, modes);
    }

    // TODO Use quadrants for this search?
    // TODO Station Groups here?
    public List<Station> nearestStationsSorted(final GridPosition gridPosition, final int maxToFind, final MarginInMeters rangeInMeters,
                                               final EnumSet<TransportMode> modes) {

        final Geography.LocationsSource<Station> source;
        if (modes.isEmpty()) {
            logger.warn("No station modes provided, will not filter stations by mode");
            source = stationRepository::getActiveStationStream;
        } else {
            source = () -> getStationModeFilteredStations(modes);
        }

        return geography.getNearToSorted(source, gridPosition, rangeInMeters).
                limit(maxToFind).
                collect(Collectors.toList());
    }

    private Stream<Station> getStationModeFilteredStations(EnumSet<TransportMode> modes) {
        return stationRepository.getActiveStationStream().
                filter(station -> TransportMode.intersects(modes, station.getTransportModes()));
    }

    @Override
    public Stream<Station> nearestStationsUnsorted(Station station, MarginInMeters rangeInMeters) {
        return geography.getNearToUnsorted(stationRepository::getActiveStationStream, station.getGridPosition(), rangeInMeters);
    }

    public boolean anyStationsWithinRangeOf(Location<?> position, MarginInMeters margin) {
        return anyStationsWithinRangeOf(position.getGridPosition(), margin);
    }

    public boolean anyStationsWithinRangeOf(GridPosition gridPosition, MarginInMeters margin) {
        // find if within range of a box, if we then need to check if also within range of an actual station
        Set<BoundingBox> quadrantsWithinRange = getQuadrantsWithinRange(gridPosition, margin);

        if (quadrantsWithinRange.isEmpty()) {
            logger.debug("No quadrant within range " + margin + " of " + gridPosition);
            return false;
        }

        Stream<Station> candidateStations = quadrantsWithinRange.stream().flatMap(quadrant -> stationBoxes.get(quadrant).stream());

        return geography.getNearToUnsorted(() -> candidateStations, gridPosition, margin).findAny().isPresent();
    }

    public Stream<BoundingBoxWithStations> getStationsInGrids(final int gridSize) {
        if (gridSize <= 0) {
            throw new RuntimeException("Invalid grid size of " + gridSize);
        }

        logger.info("Getting groupded stations for grid size " + gridSize);

        return createBoundingBoxesFor(gridSize).
                map(box -> new BoundingBoxWithStations(box, getStationsWithin(box))).
                filter(BoundingBoxWithStations::hasStations);
    }

    // todo use StationBoxFactory instead
    @Deprecated
    public Stream<BoundingBox> createBoundingBoxesFor(final int gridSize) {
        // addresses performance and memory usages on very large grids
        return getEastingsStream(gridSize).
                flatMap(x -> getNorthingsStream(gridSize).
                        map(y -> new BoundingBox(x, y, x + gridSize, y + gridSize)));
    }

    private Set<BoundingBox> getQuadrantsWithinRange(GridPosition position, MarginInMeters range) {
        return this.quadrants.stream().
                filter(quadrant -> quadrant.within(range, position)).collect(Collectors.toSet());
    }

    public LocationSet<Station> getStationsWithin(final BoundingBox box) {
        final Stream<BoundingBox> overlaps = quadrants.stream().filter(box::overlapsWith);

        final Stream<Station> candidateStations = overlaps.flatMap(quadrant -> stationBoxes.get(quadrant).stream());

        return candidateStations.filter(box::contained).collect(LocationSet.stationCollector());
    }

    private Stream<Integer> getEastingsStream(int gridSize) {
        final int minEastings = bounds.getMinEastings();
        final int maxEasting = bounds.getMaxEasting();
        return IntStream.
                iterate(minEastings, current -> current <= maxEasting, current -> current + gridSize).boxed();
    }

    private Stream<Integer> getNorthingsStream(int gridSize) {
        int minNorthings = bounds.getMinNorthings();
        int maxNorthings = bounds.getMaxNorthings();
        return IntStream.iterate(minNorthings, current -> current <= maxNorthings, current -> current + gridSize).boxed();
    }

    private static class CreateBoundingBox {
        private int minEastings = Integer.MAX_VALUE;
        private int maxEasting = Integer.MIN_VALUE;
        private int minNorthings = Integer.MAX_VALUE;
        private int maxNorthings = Integer.MIN_VALUE;

        private BoundingBox createBoundingBox(Stream<Station> stations) {
            stations.map(Station::getGridPosition).
                    filter(GridPosition::isValid).
                    forEach(gridPosition -> {
                        int eastings = gridPosition.getEastings();
                        int northings = gridPosition.getNorthings();

                        if (eastings < minEastings) {
                            minEastings = eastings;
                        }
                        if (eastings > maxEasting) {
                            maxEasting = eastings;
                        }
                        if (northings < minNorthings) {
                            minNorthings = northings;
                        }
                        if (northings > maxNorthings) {
                            maxNorthings = northings;
                        }
                    });

            if (minEastings==Integer.MAX_VALUE || maxEasting==Integer.MIN_VALUE ||
                    minNorthings==Integer.MIN_VALUE || maxNorthings==Integer.MIN_VALUE) {
                String message = "Could not form bounded box for active stations, are any stations loaded?";
                logger.error(message);
                throw new RuntimeException(message);
            }

            return new BoundingBox(minEastings, minNorthings, maxEasting, maxNorthings);
        }
    }
}
