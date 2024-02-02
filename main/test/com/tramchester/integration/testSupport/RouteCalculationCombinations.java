package com.tramchester.integration.testSupport;

import com.tramchester.ComponentContainer;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationIdPair;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.LocationIdPairSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.*;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

public class RouteCalculationCombinations<T extends Location<T>> {

    private final GraphDatabase database;
    private final RouteCalculator calculator;
    private final StationRepository stationRepository;
    private final InterchangeRepository interchangeRepository;
    private final RouteEndRepository routeEndRepository;
    private final ChecksOpen<T> checksOpen;
    private final LocationRepository locationRepository;

    public RouteCalculationCombinations(ComponentContainer componentContainer, ChecksOpen<T> checksOpen) {
        this.database = componentContainer.get(GraphDatabase.class);
        this.calculator = componentContainer.get(RouteCalculator.class);
        this.stationRepository = componentContainer.get(StationRepository.class);
        this.locationRepository = componentContainer.get(LocationRepository.class);
        this.interchangeRepository = componentContainer.get(InterchangeRepository.class);
        routeEndRepository = componentContainer.get(RouteEndRepository.class);
        this.checksOpen = checksOpen;
    }

    public static ChecksOpen<Station> checkStationOpen(ComponentContainer componentContainer) {
        final ClosedStationsRepository closedStationRepository = componentContainer.get(ClosedStationsRepository.class);
        return (stationId, date) -> !closedStationRepository.isClosed(stationId, date);
    }

    public static ChecksOpen<StationGroup> checkGroupOpen(ComponentContainer componentContainer) {
        final ClosedStationsRepository closedStationRepository = componentContainer.get(ClosedStationsRepository.class);
        final StationGroupsRepository stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        return (stationGroupId, date) -> !closedStationRepository.isGroupClosed(stationGroupsRepository.getStationGroup(stationGroupId), date);
    }

    public Optional<Journey> findJourneys(MutableGraphTransaction txn, IdFor<T> start, IdFor<T> dest, JourneyRequest journeyRequest) {
        return calculator.calculateRoute(txn, locationRepository.getLocation(start),
                        locationRepository.getLocation(dest), journeyRequest)
                .limit(1).findAny();
    }

    public CombinationResults<T> validateAllHaveAtLeastOneJourney(final LocationIdPairSet<T> stationIdPairs,
                                                                             final JourneyRequest journeyRequest, final boolean check) {

        if (stationIdPairs.isEmpty()) {
            fail("no station pairs");
        }
        long openPairs = stationIdPairs.stream().filter(stationIdPair -> bothOpen(stationIdPair, journeyRequest)).count();
        assertNotEquals(0, openPairs);

        CombinationResults<T> results = computeJourneys(stationIdPairs, journeyRequest);
        assertEquals(openPairs, results.size(), "Not enough results");

        // check all results present, collect failures into a list
        List<RouteCalculationCombinations.JourneyOrNot<T>> failed = results.getFailed();

        // TODO This should be in the tests, not here
        if (check) {
            assertEquals(0L, failed.size(), format("For %s Failed some of %s (finished %s) combinations %s",
                    journeyRequest, results.size(), stationIdPairs.size(), displayFailed(failed)));
        }

        return results;
    }

    private boolean bothOpen(LocationIdPair<T> locationIdPair, JourneyRequest journeyRequest) {
        return checksOpen.isOpen(locationIdPair.getBeginId(), journeyRequest.getDate()) &&
                checksOpen.isOpen(locationIdPair.getEndId(), journeyRequest.getDate());
    }

    public CombinationResults<T> getJourneysFor(final LocationIdPairSet<T> stationIdPairs, JourneyRequest journeyRequest) {
        return validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, false);
    }

    @Deprecated
    public CombinationResults<T> validateAllHaveAtLeastOneJourney(LocationIdPairSet<T> stationIdPairs, JourneyRequest journeyRequest) {
        return validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, true);
    }

    @NotNull
    private CombinationResults<T> computeJourneys(final LocationIdPairSet<T> combinations, final JourneyRequest request) {
        final TramDate queryDate = request.getDate();
        final TramTime queryTime = request.getOriginalTime();

        Stream<JourneyOrNot<T>> resultsStream = combinations.
                parallelStream().
                filter(stationIdPair -> bothOpen(stationIdPair, request)).
                map(stationIdPair -> {
                    try (final MutableGraphTransaction txn = database.beginTxMutable()) {
                        final Optional<Journey> optionalJourney = findJourneys(txn, stationIdPair.getBeginId(), stationIdPair.getEndId(), request);
                        return new JourneyOrNot<T>(stationIdPair, queryDate, queryTime, optionalJourney);
                    }
                });
        return new CombinationResults<>(resultsStream);
    }

    public String displayFailed(List<JourneyOrNot<T>> pairs) {
        StringBuilder stringBuilder = new StringBuilder();
        pairs.forEach(pair -> stringBuilder.append("[").
                append(pair.requested.getBeginId()).
                append(" to ").append(pair.requested.getEndId()).
                append("] "));
        return stringBuilder.toString();
    }

    public LocationIdPairSet<Station> EndOfRoutesToInterchanges(TransportMode mode) {
        IdSet<Station> interchanges = getInterchangesFor(mode);
        IdSet<Station> endRoutes = routeEndRepository.getStations(mode);
        return createJourneyPairs(interchanges, endRoutes);
    }

    public LocationIdPairSet<Station> InterchangeToInterchange(TransportMode mode) {
        IdSet<Station> interchanges = getInterchangesFor(mode);
        return createJourneyPairs(interchanges, interchanges);
    }

    public LocationIdPairSet<Station> InterchangeToEndRoutes(TransportMode mode) {
        IdSet<Station> interchanges = getInterchangesFor(mode);
        IdSet<Station> endRoutes = routeEndRepository.getStations(mode);
        return createJourneyPairs(endRoutes, interchanges);
    }

    private IdSet<Station> getInterchangesFor(TransportMode mode) {
        return interchangeRepository.getAllInterchanges().stream().
                map(InterchangeStation::getStationId).
                filter(stationId -> stationRepository.getStationById(stationId).servesMode(mode)).
                collect(IdSet.idCollector());
    }

    public LocationIdPairSet<Station> EndOfRoutesToEndOfRoutes(TransportMode mode) {
        IdSet<Station> endRoutes = routeEndRepository.getStations(mode);
        return createJourneyPairs(endRoutes, endRoutes);
    }

    private LocationIdPairSet<Station> createJourneyPairs(IdSet<Station> starts, IdSet<Station> ends) {
        LocationIdPairSet<Station> combinations = new LocationIdPairSet<>();
        for (IdFor<Station> start : starts) {
            for (IdFor<Station> dest : ends) {
                if (!dest.equals(start)) {
                    combinations.add(new StationIdPair(start, dest));
                }
            }
        }
        return combinations;
    }

    public boolean betweenInterchanges(Station start, Station dest) {
        return interchangeRepository.isInterchange(start) && interchangeRepository.isInterchange(dest);
    }

    public static class JourneyOrNot<T extends Location<T>> {
        private final LocationIdPair<T> requested;
        private final LocalDate queryDate;
        private final TramTime queryTime;
        private final Journey journey;

        public JourneyOrNot(LocationIdPair<T> requested, TramDate queryDate, TramTime queryTime, Optional<Journey> optionalJourney) {
            this(requested, queryDate.toLocalDate(), queryTime, optionalJourney);
        }

        public JourneyOrNot(LocationIdPair<T> requested, LocalDate queryDate, TramTime queryTime, Optional<Journey> optionalJourney) {
            this.requested = requested;
            this.queryDate = queryDate;
            this.queryTime = queryTime;
            this.journey = optionalJourney.orElse(null);
        }


        public boolean missing() {
            return journey==null;
        }

        @Override
        public String toString() {
            return "JourneyOrNot{" +
                    " queryDate=" + queryDate +
                    ", queryTime=" + queryTime +
                    ", from=" + requested.getBeginId() +
                    ", to=" + requested.getEndId() +
                    '}';
        }

        public Journey getJourney() {
            if (journey==null) {
                throw new RuntimeException("no journey");
            }
            return journey;
        }

        public LocationIdPair<T> getPair() {
            return requested;
        }
    }

    public static class CombinationResults<T extends Location<T>> {
        private final List<JourneyOrNot<T>> theResults;

        public CombinationResults(Stream<JourneyOrNot<T>> resultsStream) {
            theResults = resultsStream.collect(Collectors.toList());
        }

        public int size() {
            return theResults.size();
        }

        public List<JourneyOrNot<T>> getFailed() {
            return theResults.stream().
                    filter(RouteCalculationCombinations.JourneyOrNot::missing).
                    toList();
        }

        public LocationIdPairSet<T> getMissing() {
            return theResults.stream().
                    filter(JourneyOrNot::missing).
                    map(JourneyOrNot::getPair).
                    collect(LocationIdPairSet.collector());
        }

        public List<Journey> getValidJourneys() {
            return theResults.stream().
                    filter(journeyOrNot -> !journeyOrNot.missing()).
                    map(JourneyOrNot::getJourney).
                    collect(Collectors.toList());
        }

    }

    public static interface ChecksOpen<T extends Location<T>> {
        boolean isOpen(IdFor<T> beginId, TramDate date);
    }
}
