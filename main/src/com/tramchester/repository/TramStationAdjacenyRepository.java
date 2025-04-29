package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.graph.filters.GraphFilterActive;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Supports tram position inference
 */
@LazySingleton
public class TramStationAdjacenyRepository  {

    // each station and it's direct neighbours

    private static final Logger logger = LoggerFactory.getLogger(TramStationAdjacenyRepository.class);

    private final TripRepository tripRepository;
    private final GraphFilterActive graphFilter;

    @Inject
    public TramStationAdjacenyRepository(TripRepository tripRepository, GraphFilterActive graphFilter) {
        this.tripRepository = tripRepository;
        this.graphFilter = graphFilter;
    }

    //
    // Distance between two adjacent stations, or negative Duration -999 if not next to each other
    //
    public Duration getAdjacent(final StationIdPair stationPair, final TramDate date, final TimeRange timeRange) {
        final IdFor<Station> begin = stationPair.getBeginId();
        final IdFor<Station> end = stationPair.getEndId();

        final List<StopCalls.StopLeg> legs = tripRepository.getTrips().stream().
                filter(TransportMode::isTram).
                filter(trip -> trip.operatesOn(date)).
                filter(trip -> trip.callsAt(begin) && trip.callsAt(end)).
                flatMap(trip -> trip.getStopCalls().getLegs(graphFilter.isActive()).stream()).toList();

        if (legs.isEmpty()) {
            logger.warn("Failed to find legs between " + stationPair + " for " + date + " and " + timeRange);
        }

        final List<Duration> costs = legs.stream().filter(leg -> leg.getStations().equals(stationPair)).
                filter(leg -> timeRange.contains(leg.getDepartureTime())).
                map(StopCalls.StopLeg::getCost).
                toList();

        if (costs.isEmpty()) {
            logger.warn("Failed to find costs between " + stationPair + " for " + date + " and " + timeRange);
            return Duration.ofMinutes(-999);
        }

        final Set<Duration> unique = new HashSet<>(costs);

        if (unique.size()==1) {
            return costs.getFirst();
        }

        logger.warn("Ambiguous cost between " + stationPair + " costs: " + costs + " for " + date + " and " + timeRange);
        final long sum = costs.stream().mapToLong(Duration::getSeconds).sum();
        final long average = Math.floorDiv(sum, costs.size());
        return Duration.ofSeconds(average);

    }

    public Set<StationPair> getTramStationParis(final TramDate date) {

        return tripRepository.getTrips().stream().
                filter(TransportMode::isTram).
                filter(trip -> trip.operatesOn(date)).
                flatMap(trip -> trip.getStopCalls().getLegs(graphFilter.isActive()).stream()).
                map(StationPair::of).
                collect(Collectors.toSet());

    }

}
