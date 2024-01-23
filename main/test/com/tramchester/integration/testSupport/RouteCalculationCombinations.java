package com.tramchester.integration.testSupport;

import com.tramchester.ComponentContainer;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteEndRepository;
import com.tramchester.repository.StationRepository;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

public class RouteCalculationCombinations {

    private final GraphDatabase database;
    private final RouteCalculator calculator;
    private final StationRepository stationRepository;
    private final InterchangeRepository interchangeRepository;
    private final RouteEndRepository routeEndRepository;
    private final ClosedStationsRepository closedStationsRepository;

    public RouteCalculationCombinations(ComponentContainer componentContainer) {
        this.database = componentContainer.get(GraphDatabase.class);
        this.calculator = componentContainer.get(RouteCalculator.class);
        this.stationRepository = componentContainer.get(StationRepository.class);
        this.interchangeRepository = componentContainer.get(InterchangeRepository.class);
        routeEndRepository = componentContainer.get(RouteEndRepository.class);
        closedStationsRepository = componentContainer.get(ClosedStationsRepository.class);
    }

    public Optional<Journey> findJourneys(MutableGraphTransaction txn, IdFor<Station> start, IdFor<Station> dest, JourneyRequest journeyRequest) {
        return calculator.calculateRoute(txn, stationRepository.getStationById(start),
                stationRepository.getStationById(dest), journeyRequest)
                .limit(1).findAny();
    }

    public CombinationResults validateAllHaveAtLeastOneJourney(final Set<StationIdPair> stationIdPairs,
                                                                             final JourneyRequest journeyRequest, final boolean check) {

        if (stationIdPairs.isEmpty()) {
            fail("no station pairs");
        }
        long openPairs = stationIdPairs.stream().filter(stationIdPair -> bothOpen(stationIdPair, journeyRequest)).count();
        assertNotEquals(0, openPairs);

        CombinationResults results = computeJourneys(stationIdPairs, journeyRequest);
        assertEquals(openPairs, results.size(), "Not enough results");

        // check all results present, collect failures into a list
        List<RouteCalculationCombinations.JourneyOrNot> failed = results.getFailed();

        // TODO This should be in the tests, not here
        if (check) {
            assertEquals(0L, failed.size(), format("For %s Failed some of %s (finished %s) combinations %s",
                    journeyRequest, results.size(), stationIdPairs.size(), displayFailed(failed)));
        }

        return results;
    }

    public CombinationResults getJourneysFor(Set<StationIdPair> stationIdPairs, JourneyRequest journeyRequest) {
        return validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, false);
    }

    @Deprecated
    public CombinationResults validateAllHaveAtLeastOneJourney(Set<StationIdPair> stationIdPairs, JourneyRequest journeyRequest) {
        return validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, true);
    }

    @NotNull
    private CombinationResults computeJourneys(final Set<StationIdPair> combinations, final JourneyRequest request) {
        final TramDate queryDate = request.getDate();
        final TramTime queryTime = request.getOriginalTime();

        Stream<JourneyOrNot> resultsStream = combinations.
                parallelStream().
                filter(stationIdPair -> bothOpen(stationIdPair, request)).
                map(stationIdPair -> {
                    try (final MutableGraphTransaction txn = database.beginTxMutable()) {
                        final Optional<Journey> optionalJourney = findJourneys(txn, stationIdPair.getBeginId(), stationIdPair.getEndId(), request);
                        return new JourneyOrNot(stationIdPair, queryDate, queryTime, optionalJourney);
                    }
                });
        return new CombinationResults(resultsStream);
    }

    private boolean bothOpen(final StationIdPair stationIdPair, final JourneyRequest request) {
        final TramDate date = request.getDate();
        if (closedStationsRepository.hasClosuresOn(date)) {
            return ! (closedStationsRepository.isClosed(stationIdPair.getBeginId(), date) ||
                    closedStationsRepository.isClosed(stationIdPair.getEndId(), date));
        }
        return true;

    }

    public String displayFailed(List<JourneyOrNot> pairs) {
        StringBuilder stringBuilder = new StringBuilder();
        pairs.forEach(pair -> stringBuilder.append("[").
                append(pair.requested.getBeginId()).
                append(" to ").append(pair.requested.getEndId()).
                append("] "));
        return stringBuilder.toString();
    }

    public Set<StationIdPair> EndOfRoutesToInterchanges(TransportMode mode) {
        IdSet<Station> interchanges = getInterchangesFor(mode);
        IdSet<Station> endRoutes = routeEndRepository.getStations(mode);
        return createJourneyPairs(interchanges, endRoutes);
    }

    public Set<StationIdPair> InterchangeToInterchange(TransportMode mode) {
        IdSet<Station> interchanges = getInterchangesFor(mode);
        return createJourneyPairs(interchanges, interchanges);
    }

    public Set<StationIdPair> InterchangeToEndRoutes(TransportMode mode) {
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

    public Set<StationIdPair> EndOfRoutesToEndOfRoutes(TransportMode mode) {
        IdSet<Station> endRoutes = routeEndRepository.getStations(mode);
        return createJourneyPairs(endRoutes, endRoutes);
    }

    private Set<StationIdPair> createJourneyPairs(IdSet<Station> starts, IdSet<Station> ends) {
        Set<StationIdPair> combinations = new HashSet<>();
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

    public static class JourneyOrNot {
        private final StationIdPair requested;
        private final LocalDate queryDate;
        private final TramTime queryTime;
        private final Journey journey;

        public JourneyOrNot(StationIdPair requested, TramDate queryDate, TramTime queryTime, Optional<Journey> optionalJourney) {
            this(requested, queryDate.toLocalDate(), queryTime, optionalJourney);
        }

        public JourneyOrNot(StationIdPair requested, LocalDate queryDate, TramTime queryTime, Optional<Journey> optionalJourney) {
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

        public StationIdPair getPair() {
            return requested;
        }
    }

    public static class CombinationResults {
        private final List<JourneyOrNot> theResults;

//        public CombinationResults(Stream<Pair<StationIdPair, JourneyOrNot>> resultsStream) {
//            theResults = resultsStream.map(Pair::getRight).collect(Collectors.toList());
//        }

        public CombinationResults(Stream<JourneyOrNot> resultsStream) {
            theResults = resultsStream.collect(Collectors.toList());
        }

        public int size() {
            return theResults.size();
        }

        public List<JourneyOrNot> getFailed() {
            return theResults.stream().
                    filter(RouteCalculationCombinations.JourneyOrNot::missing).
                    toList();
        }

        public Set<StationIdPair> getMissing() {
            return theResults.stream().
                    filter(JourneyOrNot::missing).
                    map(JourneyOrNot::getPair).
                    collect(Collectors.toSet());
        }

        public List<Journey> getValidJourneys() {
            return theResults.stream().
                    filter(journeyOrNot -> !journeyOrNot.missing()).
                    map(JourneyOrNot::getJourney).
                    collect(Collectors.toList());
        }

    }
}
