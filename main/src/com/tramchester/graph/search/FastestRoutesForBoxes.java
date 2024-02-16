package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.JourneysForBox;
import com.tramchester.domain.collections.RequestStopStream;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.StationBoxFactory;
import com.tramchester.geo.StationsBoxSimpleGrid;
import com.tramchester.mappers.Geography;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class FastestRoutesForBoxes {
    private static final Logger logger = LoggerFactory.getLogger(FastestRoutesForBoxes.class);

    private final StationBoxFactory stationBoxFactory;
    private final RouteCalculatorForBoxes calculator;
    private final Geography geography;

    @Inject
    public FastestRoutesForBoxes(StationBoxFactory stationBoxFactory, RouteCalculatorForBoxes calculator, Geography geography) {
        this.stationBoxFactory = stationBoxFactory;
        this.calculator = calculator;
        this.geography = geography;
    }

    public RequestStopStream<BoundingBoxWithCost> findForGrid(Location<?> destination, int gridSize, JourneyRequest journeyRequest)  {

        logger.info("Creating station groups for gridsize " + gridSize + " and destination " + destination.getId());
        final GridPosition gridPosition = destination.getGridPosition();

        return findForGrid(gridPosition, gridSize, journeyRequest);
    }

    @NotNull
    private RequestStopStream<BoundingBoxWithCost> findForGrid(GridPosition destinationGrid, int gridSize, final JourneyRequest journeyRequest) {
        logger.info("Creating station groups for gridsize " + gridSize + " and destination " + destinationGrid);

//        final Set<BoundingBoxWithStations> searchGrid = stationLocations.getStationsInGrids(gridSize).
//                filter(boxWithStations -> anyOpen(boxWithStations.getStations(), journeyRequest.getDate())).
//                collect(Collectors.toSet());

        final List<StationsBoxSimpleGrid> searchGrid = stationBoxFactory.getStationBoxes(gridSize, journeyRequest.getDate());

        final StationsBoxSimpleGrid searchBoxWithDest = searchGrid.stream().
                filter(box -> box.contained(destinationGrid)).
                findFirst().
                orElseThrow(() -> new RuntimeException("Unable to find destination in any boxes " + destinationGrid));

        logger.info(format("Using %s groups and %s destinations", searchGrid.size(), searchBoxWithDest.getStations().size()));

        final List<StationsBoxSimpleGrid> sortedSearchGrid = sortGridNearestFirst(searchGrid, destinationGrid);

        return calculator.calculateRoutes(searchBoxWithDest, journeyRequest, sortedSearchGrid).
                map(box -> cheapest(box, destinationGrid));
    }

    private List<StationsBoxSimpleGrid> sortGridNearestFirst(List<StationsBoxSimpleGrid> searchGrid, GridPosition destinationGrid) {
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
