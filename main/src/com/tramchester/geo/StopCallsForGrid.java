package com.tramchester.geo;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.BoxWithServiceFrequency;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.StopCallRepository;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
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

    public Stream<BoxWithServiceFrequency> getServiceFrequencies(int gridSize, TramDate date, TramTime begin, TramTime end) {
        logger.info(format("Get stopcalls for grid size %s on %s between %s and %s", gridSize, date, begin, end));

        return stationLocations.getStationsInGrids(gridSize).
                filter(BoundingBoxWithStations::hasStations).
                map(box -> createFrequencyBox(date, begin, end, box));
    }

    @NotNull
    private BoxWithServiceFrequency createFrequencyBox(final TramDate date, final TramTime begin, final TramTime end, final BoundingBoxWithStations box) {
        final Map<Station, Integer> stationToNumberStopCalls = new HashMap<>();
        final EnumSet<TransportMode> modes = EnumSet.noneOf(TransportMode.class);
        box.getStations().stream().
                filter(location -> location.getLocationType()== LocationType.Station).
                map(location -> (Station)location).
                forEach(station -> {
                    final Set<StopCall> calls = stopCallRepository.getStopCallsFor(station, date, begin, end);
                    if (!calls.isEmpty()) {
                        stationToNumberStopCalls.put(station, calls.size());
                        modes.addAll(calls.stream().map(StopCall::getTransportMode).collect(Collectors.toCollection(() ->EnumSet.noneOf(TransportMode.class))));
                    }
        });

        final int total = stationToNumberStopCalls.values().stream().mapToInt(num->num).sum();
        return new BoxWithServiceFrequency(box, stationToNumberStopCalls.keySet(), total, modes);
    }

}
