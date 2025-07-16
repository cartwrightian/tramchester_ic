package com.tramchester.integration.testSupport;

import com.tramchester.ComponentContainer;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationIdPair;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.collections.LocationIdPairSet;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphDatabaseNeo4J;
import com.tramchester.graph.facade.neo4j.ImmutableGraphTransactionNeo4J;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.*;
import com.tramchester.testSupport.UpcomingDates;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

public class RouteCalculationCombinations<T extends Location<T>> {

    private final GraphDatabase database;
    private final RouteCalculator calculator;
    private final StationRepository stationRepository;
    private final InterchangeRepository interchangeRepository;
    private final TripEndsRepository routeEndRepository;
    private final ChecksOpen<T> checksOpen;
    private final LocationRepository locationRepository;

    public RouteCalculationCombinations(ComponentContainer componentContainer, ChecksOpen<T> checksOpen) {
        this.database = componentContainer.get(GraphDatabase.class);
        this.calculator = componentContainer.get(RouteCalculator.class);
        this.stationRepository = componentContainer.get(StationRepository.class);
        this.locationRepository = componentContainer.get(LocationRepository.class);
        this.interchangeRepository = componentContainer.get(InterchangeRepository.class);
        this.routeEndRepository = componentContainer.get(TripEndsRepository.class);
        this.checksOpen = checksOpen;
    }

    public CreatePairs getCreatePairs(TramDate date) {
        return new CreatePairs(stationRepository, routeEndRepository, interchangeRepository, date);
    }

    public static ChecksOpen<Station> checkStationOpen(final ComponentContainer componentContainer) {
        final ClosedStationsRepository closedStationRepository = componentContainer.get(ClosedStationsRepository.class);
        return (stationId, date) -> {
            return !( UpcomingDates.hasClosure(stationId, date) || closedStationRepository.isStationClosed(stationId, date) );
        };
    }

    public static ChecksOpen<StationLocalityGroup> checkGroupOpen(final ComponentContainer componentContainer) {
        final ClosedStationsRepository closedStationRepository = componentContainer.get(ClosedStationsRepository.class);
        final StationGroupsRepository stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        return (stationGroupId, date) ->
                !closedStationRepository.isGroupClosed(stationGroupsRepository.getStationGroup(stationGroupId), date);
    }

    public Optional<Journey> findJourneys(final ImmutableGraphTransactionNeo4J txn, final IdFor<T> start, final IdFor<T> dest,
                                          final JourneyRequest journeyRequest, final Running running) {
        return calculator.calculateRoute(txn, locationRepository.getLocation(start), locationRepository.getLocation(dest), journeyRequest, running)
                .limit(1).
                findAny();
    }

    public CombinationResults<T> validateAllHaveAtLeastOneJourney(final LocationIdPairSet<T> stationIdPairs,
                                                                             final JourneyRequest journeyRequest, final boolean check) {
        return validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, check, GraphDatabaseNeo4J.DEFAULT_TXN_TIMEOUT);
    }

    public CombinationResults<T> validateAllHaveAtLeastOneJourney(final LocationIdPairSet<T> stationIdPairs,
                                                                  final JourneyRequest journeyRequest, final boolean check,
                                                                  final Duration timeout) {

        final Running running = () -> true;
        return validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, check, timeout, running);
    }

    public CombinationResults<T> validateAllHaveAtLeastOneJourney(final LocationIdPairSet<T> stationIdPairs,
                                                                             final JourneyRequest journeyRequest, final boolean check,
                                                                  final Duration timeout, final Running running) {

        if (stationIdPairs.isEmpty()) {
            fail("no station pairs");
        }
        final long openPairs = stationIdPairs.stream().
                filter(stationIdPair -> bothOpen(stationIdPair, journeyRequest.getDate())).
                count();
        assertNotEquals(0, openPairs);


        final CombinationResults<T> results = computeJourneys(stationIdPairs, journeyRequest, running, timeout);
        assertEquals(openPairs, results.size(), "Not enough results");

        // check all results present, collect failures into a list
        final List<RouteCalculationCombinations.JourneyOrNot<T>> failed = results.getFailed();

        // TODO This should be in the tests, not here
        if (check) {
            assertEquals(0L, failed.size(), format("For %s Failed some of %s (finished %s) combinations %s",
                    journeyRequest, results.size(), stationIdPairs.size(), displayFailed(failed)));
        }

        return results;
    }

    private boolean bothOpen(final LocationIdPair<T> locationIdPair, final TramDate date) {
        return checksOpen.isOpen(locationIdPair.getBeginId(), date) &&
                checksOpen.isOpen(locationIdPair.getEndId(), date);
    }

    public CombinationResults<T> getJourneysFor(final LocationIdPairSet<T> stationIdPairs, final JourneyRequest journeyRequest) {
        return validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, false, GraphDatabaseNeo4J.DEFAULT_TXN_TIMEOUT);
    }

    public CombinationResults<T> getJourneysFor(final LocationIdPairSet<T> stationIdPairs, JourneyRequest journeyRequest,
                                                Duration timeout, Running running) {
        return validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, false, timeout, running);
    }

    @Deprecated
    public CombinationResults<T> validateAllHaveAtLeastOneJourney(LocationIdPairSet<T> stationIdPairs, JourneyRequest journeyRequest) {
        return validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, true, GraphDatabaseNeo4J.DEFAULT_TXN_TIMEOUT);
    }

    @NotNull
    private CombinationResults<T> computeJourneys(final LocationIdPairSet<T> combinations, final JourneyRequest request,
                                                  final Running running, final Duration timeout) {
        final TramDate queryDate = request.getDate();
        final TramTime queryTime = request.getOriginalTime();

        final Function<IdFor<T>, String> resolver = id -> locationRepository.getLocation(id).getName();

        Stream<JourneyOrNot<T>> resultsStream = combinations.
                parallelStream().
                filter(stationIdPair -> bothOpen(stationIdPair, queryDate)).
                map(stationIdPair -> new LocationIdAndNamePair<>(stationIdPair, resolver)).
                map(stationIdPair -> {
                    try (final ImmutableGraphTransactionNeo4J txn = database.beginTx(timeout)) {
                        final Optional<Journey> optionalJourney = findJourneys(txn, stationIdPair.getBeginId(), stationIdPair.getEndId(), request, running);
                        return new JourneyOrNot<>(stationIdPair, queryDate, queryTime, optionalJourney);
                    }
                });
        return new CombinationResults<>(resultsStream);
    }

    public String displayFailed(final List<JourneyOrNot<T>> pairs) {
        final StringBuilder stringBuilder = new StringBuilder();
        pairs.forEach(pair -> stringBuilder.append("[").append(pair).append("] "));
        return stringBuilder.toString();
    }

    public boolean betweenInterchanges(final Station start, final Station dest) {
        return interchangeRepository.isInterchange(start) && interchangeRepository.isInterchange(dest);
    }

    public JourneyOrNot<T> createResult(LocationIdPair<T> locationIdPair, TramDate queryDate, TramTime queryTime,
                                        Optional<Journey> optionalJourney) {
        final Function<IdFor<T>, String> resolver = id -> locationRepository.getLocation(id).getName();
        final LocationIdAndNamePair<T> requested = new LocationIdAndNamePair<>(locationIdPair, resolver);
        return new JourneyOrNot<>(requested, queryDate, queryTime, optionalJourney);
    }

    public static class CreatePairs {
        private final StationRepository stationRepository;
        private final TripEndsRepository routeEndRepository;
        private final InterchangeRepository interchangeRepository;
        private final TramDate date;

        public CreatePairs(StationRepository stationRepository, TripEndsRepository routeEndRepository,
                           InterchangeRepository interchangeRepository, TramDate date) {
            this.stationRepository = stationRepository;
            this.routeEndRepository = routeEndRepository;
            this.interchangeRepository = interchangeRepository;
            this.date = date;
        }

        public LocationIdPairSet<Station> createStationPairsForAll(final EnumSet<TransportMode> modes) {

            final Set<Station> allStations = stationRepository.getStations(modes);

            // pairs of stations to check
            return allStations.stream().
                    flatMap(start -> allStations.stream().
                            map(dest -> LocationIdPair.of(start, dest))).
                    filter(pair -> !UpcomingDates.hasClosure(pair, date)).
                    filter(pair -> !pair.same()).
                    collect(LocationIdPairSet.collector());
        }

        public LocationIdPairSet<Station> endOfRoutesToEndOfRoutes(final TransportMode mode) {
            return endOfRoutesToEndOfRoutes(EnumSet.of(mode));
        }

        public LocationIdPairSet<Station> endOfRoutesToEndOfRoutes(final EnumSet<TransportMode> modes) {
            final IdSet<Station> endRoutes = routeEndRepository.getStations(modes);
            // sanity check, primarily for rail
            IdSet<Station> missingStations = endRoutes.stream().
                    filter(stationIdFor -> !stationRepository.hasStationId(stationIdFor)).
                    collect(IdSet.idCollector());
            if (missingStations.isEmpty()) {
                return createJourneyPairs(endRoutes, endRoutes, date);
            } else {
                throw new RuntimeException("Got missing stations from end routes " + missingStations);
            }
        }

        public LocationIdPairSet<Station> endOfRoutesToInterchanges(final TransportMode mode) {
            return endOfRoutesToInterchanges(EnumSet.of(mode));
        }

            public LocationIdPairSet<Station> endOfRoutesToInterchanges(final EnumSet<TransportMode> modes) {
            final IdSet<Station> interchanges = getInterchangesFor(modes);
            final IdSet<Station> endRoutes = routeEndRepository.getStations(modes);
            return createJourneyPairs(interchanges, endRoutes, date);
        }

        public LocationIdPairSet<Station> interchangeToInterchange(TransportMode mode) {
            return interchangeToInterchange(EnumSet.of(mode));
        }

        public LocationIdPairSet<Station> interchangeToInterchange(final EnumSet<TransportMode> modes) {
            final IdSet<Station> interchanges = getInterchangesFor(modes);
            return createJourneyPairs(interchanges, interchanges, date);
        }

        public LocationIdPairSet<Station> interchangeToEndRoutes(final TransportMode mode) {
            return interchangeToEndRoutes(EnumSet.of(mode));
        }

            public LocationIdPairSet<Station> interchangeToEndRoutes(final EnumSet<TransportMode> modes) {
            final IdSet<Station> interchanges = getInterchangesFor(modes);
            final IdSet<Station> endRoutes = routeEndRepository.getStations(modes);
            return createJourneyPairs(endRoutes, interchanges, date);
        }

        private IdSet<Station> getInterchangesFor(final EnumSet<TransportMode> modes) {
            return interchangeRepository.getAllInterchanges().stream().
                    map(InterchangeStation::getStationId).
                    filter(stationId -> stationRepository.getStationById(stationId).anyOverlapWith(modes)).
                    collect(IdSet.idCollector());
        }

        private LocationIdPairSet<Station> createJourneyPairs(IdSet<Station> starts, IdSet<Station> ends, TramDate date) {
            LocationIdPairSet<Station> combinations = new LocationIdPairSet<>();
            for (IdFor<Station> start : starts) {
                for (IdFor<Station> dest : ends) {
                    if (!dest.equals(start)) {
                        StationIdPair locationIdPair = new StationIdPair(start, dest);
                        if (!UpcomingDates.hasClosure(locationIdPair, date)) {
                            combinations.add(locationIdPair);
                        }
                    }
                }
            }
            return combinations;
        }
    }

    public static class JourneyOrNot<T extends Location<T>> {
        private final LocationIdAndNamePair<T> requested;
        private final TramDate queryDate;
        private final TramTime queryTime;
        private final Journey journey;

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        public JourneyOrNot(LocationIdAndNamePair<T> requested, TramDate queryDate, TramTime queryTime, Optional<Journey> optionalJourney) {
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
                    ", requested=" + requested +
                    '}';
        }

        public Journey getJourney() {
            if (journey==null) {
                throw new RuntimeException("no journey");
            }
            return journey;
        }

        public LocationIdAndNamePair<T> getPair() {
            return requested;
        }
    }

    public static class CombinationResults<T extends Location<T>> {
        private final List<JourneyOrNot<T>> theResults;

        public CombinationResults(final Stream<JourneyOrNot<T>> resultsStream) {
            theResults = resultsStream.toList();
        }

        public int size() {
            return theResults.size();
        }

        public List<JourneyOrNot<T>> getFailed() {
            return theResults.stream().
                    filter(RouteCalculationCombinations.JourneyOrNot::missing).
                    sorted(Comparator.comparing(a -> a.getPair().getBeginId())).
                    toList();
        }

        public LocationIdsAndNames<T> getMissing() {
            return theResults.stream().
                    filter(JourneyOrNot::missing).
                    map(JourneyOrNot::getPair).
                    collect(LocationIdsAndNames.collect());
        }

        public List<Journey> getValidJourneys() {
            return theResults.stream().
                    filter(journeyOrNot -> !journeyOrNot.missing()).
                    map(JourneyOrNot::getJourney).
                    toList();
        }

    }

    public interface ChecksOpen<T extends Location<T>> {
        boolean isOpen(final IdFor<T> beginId, final TramDate date);
    }
}
