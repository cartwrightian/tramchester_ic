package com.tramchester.geo;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.BoxWithServiceFrequency;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.StopCallRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class StopCallsForGrid {
    private static final Logger logger = LoggerFactory.getLogger(StopCallsForGrid.class);

    private final StationLocations stationLocations;
    private final StopCallRepository stopCallRepository;

    @Inject
    public StopCallsForGrid(StationLocations stationLocations, StopCallRepository stopCallRepository) {
        this.stationLocations = stationLocations;
        this.stopCallRepository = stopCallRepository;
    }

    public Set<BoxWithServiceFrequency> getServiceFreqencies(long gridSize, LocalDate date, TramTime begin, TramTime end) {
        logger.info(format("Get stopcalls for grid size %s on %s between %s and %s", gridSize, date, begin, end));
        Stream<BoundingBoxWithStations> boxes = stationLocations.getGroupedStations(gridSize);

        return boxes.filter(BoundingBoxWithStations::hasStations).
                map(box -> createFrequencyBox(date, begin, end, box)).
                collect(Collectors.toSet());

    }

    @NotNull
    private BoxWithServiceFrequency createFrequencyBox(LocalDate date, TramTime begin, TramTime end, BoundingBoxWithStations box) {
        Map<Station, Integer> stationToNumberStopCalls = new HashMap<>();
        box.getStaions().forEach(station -> {
            int number = stopCallRepository.getStopCallsFor(station, date, begin, end).size();
            if (number>0) {
                stationToNumberStopCalls.put(station, number);
            }
        });
        int total = stationToNumberStopCalls.values().stream().mapToInt(num->num).sum();
        return new BoxWithServiceFrequency(box, stationToNumberStopCalls.keySet(), total);
    }

}