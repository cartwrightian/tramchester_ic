package com.tramchester.geo;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.places.Station;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@LazySingleton
public class StationBoxFactory {
    private static final Logger logger = LoggerFactory.getLogger(StationBoxFactory.class);

    private final StationLocations stationLocations;

    @Inject
    public StationBoxFactory(StationLocations stationLocations) {
        this.stationLocations = stationLocations;
    }

    @PostConstruct
    public void start() {
    }

    public List<StationsBoxSimpleGrid> getStationBoxes(final int gridSizeInMeters) {
        final ArrayList<StationsBoxSimpleGrid> results = new ArrayList<>();

        final BoundingBox bounds = stationLocations.getActiveStationBounds();

        final double xSize = Math.ceil(bounds.width() / (double) gridSizeInMeters);
        final double ySzie = Math.ceil(bounds.height() / (double) gridSizeInMeters);

        final long maxX = Math.round(xSize);
        final long maxY = Math.round(ySzie);

        logger.info(String.format("Creating grid of %s by %s for grid size %s meters", maxX, maxY, gridSizeInMeters));

        int eastings = bounds.getMinEastings();
        for (int x = 0; x < maxX; x++) {
            int northing = bounds.getMinNorthings();
            for (int y = 0; y < maxY; y++) {
                final BoundingBox box = new BoundingBox(eastings, northing, eastings+gridSizeInMeters, northing+gridSizeInMeters);
                final LocationSet<Station> openStations = stationLocations.getStationsWithin(box).
                        stream().
                        collect(LocationSet.stationCollector());
                // excluding causes issues where diversions are in place for closed stations
//                        filter(station -> !closedStationsRepository.isClosed(station, date)).
                if (!openStations.isEmpty()) {
                    final StationsBoxSimpleGrid stationBox = new StationsBoxSimpleGrid(x, y, box, openStations);
                    results.add(stationBox);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Excluding " + box + " since no contained stations");
                    }
                }

                northing = northing + gridSizeInMeters;
            }
            eastings = eastings + gridSizeInMeters;
        }

        logger.info("Created " + results.size() + " boxes");
        return results;
    }

}
