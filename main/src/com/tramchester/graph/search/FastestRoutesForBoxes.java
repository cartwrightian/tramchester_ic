package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.StationLocations;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.ClosedStationsRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class FastestRoutesForBoxes {
    private static final Logger logger = LoggerFactory.getLogger(FastestRoutesForBoxes.class);

    private final StationLocations stationLocations;
    private final ClosedStationsRepository closedStationsRepository;
    private final RouteCalculatorForBoxes calculator;
    private final Geography geography;

    @Inject
    public FastestRoutesForBoxes(StationLocations stationLocations, ClosedStationsRepository closedStationsRepository,
                                 RouteCalculatorForBoxes calculator, Geography geography) {
        this.stationLocations = stationLocations;
        this.closedStationsRepository = closedStationsRepository;
        this.calculator = calculator;
        this.geography = geography;
    }

    public Stream<BoundingBoxWithCost> findForGrid(Location<?> destination, int gridSize, JourneyRequest journeyRequest)  {

        logger.info("Creating station groups for gridsize " + gridSize + " and destination " + destination.getId());
        final GridPosition gridPosition = destination.getGridPosition();

        return findForGrid(gridPosition, gridSize, journeyRequest);
    }

    @NotNull
    private Stream<BoundingBoxWithCost> findForGrid(GridPosition destinationGrid, int gridSize, final JourneyRequest journeyRequest) {
        logger.info("Creating station groups for gridsize " + gridSize + " and destination " + destinationGrid);

        final Set<BoundingBoxWithStations> searchGrid = stationLocations.getStationsInGrids(gridSize).
                filter(boxWithStations -> anyOpen(boxWithStations.getStations(), journeyRequest.getDate())).
                collect(Collectors.toSet());

        final BoundingBoxWithStations searchBoxWithDest = searchGrid.stream().
                filter(box -> box.contained(destinationGrid)).
                findFirst().
                orElseThrow(() -> new RuntimeException("Unable to find destination in any boxes " + destinationGrid));

        final LocationSet destinations = searchBoxWithDest.getStations();

        logger.info(format("Using %s groups and %s destinations", searchGrid.size(), destinations.size()));

        final List<BoundingBoxWithStations> sortedSearchGrid = sortGridNearestFirst(searchGrid, destinationGrid);

        return calculator.calculateRoutes(destinations, journeyRequest, sortedSearchGrid).
                map(box -> cheapest(box, destinationGrid));
    }

    private boolean anyOpen(final LocationSet stations, final TramDate date) {
        // any not closed
        return stations.stream().anyMatch(station -> !closedStationsRepository.isClosed(station, date));
    }

    private List<BoundingBoxWithStations> sortGridNearestFirst(Set<BoundingBoxWithStations> searchGrid, GridPosition destinationGrid) {
        return searchGrid.stream().
                sorted((a,b) -> geography.chooseNearestToGrid(destinationGrid, a.getMidPoint(), b.getMidPoint())).
                collect(Collectors.toList());
    }

    private BoundingBoxWithCost cheapest(JourneysForBox results, GridPosition destination) {

        if (results.contains(destination)) {
            return new BoundingBoxWithCost(results.getBox(), Duration.ZERO, null);
        }

        if (results.isEmpty()) {
            return new BoundingBoxWithCost(results.getBox(), Duration.ofMinutes(-1), null);
        }

        Journey result = results.getLowestCost();

        Duration cost = TramTime.difference(result.getDepartTime(), result.getArrivalTime());
        return new BoundingBoxWithCost(results.getBox(), cost, result);
    }
}
