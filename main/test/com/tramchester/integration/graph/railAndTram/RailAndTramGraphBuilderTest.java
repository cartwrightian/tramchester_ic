package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.*;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.reference.TransportRelationshipTypes.*;
import static com.tramchester.integration.graph.railAndTram.RailAndTramRouteCalculatorTest.STOCKPORT_TRIP_ID;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
class RailAndTramGraphBuilderTest {
    private static ComponentContainer componentContainer;

    private GraphTransaction txn;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {

        stationRepository = componentContainer.get(StationRepository.class);
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);

        StagedTransportGraphBuilder builder = componentContainer.get(StagedTransportGraphBuilder.class);
        builder.getReady();
        txn = graphDatabase.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForInterchange() {
        Station cornbrook = Cornbrook.from(stationRepository);
        GraphNode cornbrookNode = txn.findNode(cornbrook);
        Stream<GraphRelationship> outboundLinks = cornbrookNode.getRelationships(txn, GraphDirection.Outgoing, LINKED);

        List<GraphRelationship> list = outboundLinks.toList();
        // summer 2026
        assertEquals(3+2, list.size(), list.toString());

        Set<IdFor<Station>> destinations = list.stream().
                map(graphRelationship -> graphRelationship.getEndNode(txn)).
                map(GraphNode::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(TraffordBar.getId()), "missing from " + destinations);
        assertTrue(destinations.contains(Deansgate.getId()), "missing from " + destinations);

        // summer 2026
        assertTrue(destinations.contains(Pomona.getId()), "missing from " + destinations);

    }

    @Test
    void shouldHaveCorrectRouteStationToStationRouteCosts() {

        Set<RouteStation> routeStations = stationRepository.getRouteStationsFor(Piccadilly.getId());

        routeStations.forEach(routeStation -> {
            GraphNode node = txn.findNode(routeStation);

            GraphRelationship toStation = node.getSingleRelationship(txn, ROUTE_TO_STATION, GraphDirection.Outgoing);
            TramDuration costToStation = toStation.getCost(); // GraphProps.getCost(toStation);
            assertEquals(TramDuration.ZERO, costToStation, "wrong cost for " + routeStation);

            GraphRelationship fromStation = node.getSingleRelationship(txn, STATION_TO_ROUTE, GraphDirection.Incoming);
            TramDuration costFromStation = fromStation.getCost(); // GraphProps.getCost(fromStation);
            TramDuration expected = routeStation.getStation().getMinChangeDuration();
            assertEquals(expected, costFromStation, "wrong cost for " + routeStation);
        });
    }

    @Test
    void shouldHaveExpectedRelationshipsBetweenTramAndTrainStations() {
        Station altyTram = Altrincham.from(stationRepository);
        Station altyTrain = RailStationIds.Altrincham.from(stationRepository);

        TramDuration expectedCost = TramDuration.ofSeconds(51L);

        GraphNode altyTramNode = txn.findNode(altyTram);
        GraphNode altyTrainNode = txn.findNode(altyTrain);

        assertNotNull(altyTramNode);
        assertNotNull(altyTrainNode);

        List<GraphRelationship> fromTram = altyTramNode.getRelationships(txn, GraphDirection.Outgoing, NEIGHBOUR).toList();
        assertEquals(1, fromTram.size(), "Wrong number of neighbours " + fromTram);

        GraphRelationship tramNeighbour = fromTram.getFirst();
        assertEquals(altyTrainNode, tramNeighbour.getEndNode(txn)); // GraphNode.fromEnd(tramNeighbour));
        assertEquals(expectedCost, tramNeighbour.getCost());

        List<GraphRelationship> fromTrain = altyTrainNode.getRelationships(txn, GraphDirection.Outgoing, NEIGHBOUR).toList();
        assertEquals(1, fromTrain.size(), "Wrong number of neighbours " + fromTram);

        GraphRelationship trainNeighbour = fromTrain.getFirst();
        assertEquals(altyTramNode, trainNeighbour.getEndNode(txn)); //GraphNode.fromEnd(trainNeighbour));
        assertEquals(expectedCost, trainNeighbour.getCost());

    }

    @Test
    void shouldHaveOneNodePerRouteStation() {
        Set<RouteStation> routeStations = stationRepository.getRouteStations();

        ImmutableIdSet<RouteStation> noTramRouteStationNode = routeStations.stream().
                filter(routeStation -> routeStation.getTransportModes().contains(TransportMode.Tram)).
                filter(routeStation -> txn.findNode(routeStation) == null).
                collect(IdSet.collector());

        assertTrue(noTramRouteStationNode.isEmpty(), noTramRouteStationNode.toString());

        Set<RouteStation> trainRouteStations = routeStations.stream().
                filter(routeStation -> routeStation.getTransportModes().contains(TransportMode.Train)).
                filter(RouteStation::isActive). // rail data has 'passed' stations
                collect(Collectors.toSet());

        ImmutableIdSet<RouteStation> noTrainRouteStationNode = trainRouteStations.stream().
                filter(routeStation -> txn.findNode(routeStation) == null).
                collect(IdSet.collector());

        int numRouteStations = trainRouteStations.size();
        assertTrue(noTrainRouteStationNode.isEmpty(), "Not empty, num route stations is " + numRouteStations
                + " without nodes is " + noTrainRouteStationNode.size());
    }

    @Test
    void shouldHaveExpectedInterchangesInTheGraph() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);

        ImmutableIdSet<Station> fromConfigAndDiscovered = interchangeRepository.getAllInterchanges().stream().
                map(InterchangeStation::getStationId).collect(IdSet.idCollector());

        Stream<GraphNode> interchangeNodes = txn.findNodes(GraphLabel.INTERCHANGE);

        ImmutableIdSet<Station> fromDB = interchangeNodes.map(GraphNode::getStationId).collect(IdSet.idCollector());

        assertEquals(fromConfigAndDiscovered, fromDB, "Graph clean and rebuild needed?");
    }

    @Test
    void shouldHaveExpectedCostForSpecificLinkStockportToNavigationRoad() {

        TripRepository tripRepository = componentContainer.get(TripRepository.class);

        Trip trip = tripRepository.getTripById(STOCKPORT_TRIP_ID);
        IdFor<Service> serviceId = trip.getService().getId();

        StopCalls stopCalls = trip.getStopCalls();
        StopCall stockportStop = stopCalls.getStopFor(RailStationIds.Stockport.getId());
        TramTime stockportArrivalTime = stockportStop.getArrivalTime();

        Route route = trip.getRoute();

        RouteStation routeStation = stationRepository.getRouteStation(RailStationIds.Stockport.from(stationRepository), route);

        GraphNode routeStationNode = txn.findNode(routeStation);

        Stream<GraphRelationship> toServices = routeStationNode.getRelationships(txn, GraphDirection.Outgoing, TO_SERVICE);

        Optional<GraphRelationship> maybeToService = toServices.filter(graphRelationship -> graphRelationship.getServiceId().equals(serviceId)).findFirst();
        assertTrue(maybeToService.isPresent());

        GraphRelationship toService = maybeToService.get();

        GraphNode serviceNode = toService.getEndNode(txn);

        Stream<GraphRelationship> toHours = serviceNode.getRelationships(txn, GraphDirection.Outgoing, TO_HOUR);
        Optional<GraphRelationship> maybeToHour = toHours.filter(graphRelationship -> graphRelationship.getHour() == stockportArrivalTime.getHourOfDay()).findFirst();
        assertTrue(maybeToHour.isPresent());

        GraphRelationship toHour = maybeToHour.get();

        GraphNode hourNode = toHour.getEndNode(txn);
        Stream<GraphRelationship> toMinutes = hourNode.getRelationships(txn, GraphDirection.Outgoing, TO_MINUTE);
        Optional<GraphRelationship> toMinuteMaybe = toMinutes.filter(graphRelationship -> graphRelationship.getEndNode(txn).getTripId().equals(trip.getId())).findFirst();
        assertTrue(toMinuteMaybe.isPresent());

        GraphRelationship toMinute = toMinuteMaybe.get();

        GraphNode minuteNode = toMinute.getEndNode(txn);
        assertEquals(trip.getId(), minuteNode.getTripId());

        List<GraphRelationship> goesTos = minuteNode.getRelationships(txn, GraphDirection.Outgoing, TRAIN_GOES_TO).toList();

        assertEquals(1, goesTos.size());
        GraphRelationship goesTo = goesTos.getFirst();

        StopCall navyRoad = stopCalls.getStopFor(RailStationIds.NavigationRaod.getId());

        TramDuration expectedCost = TramTime.difference(stockportStop.getDepartureTime(), navyRoad.getArrivalTime());

        assertEquals(trip.getId(), goesTo.getTripId());
        assertEquals(navyRoad.getGetSequenceNumber(), goesTo.getStopSeqNumber(), "Wrong for stopcalls " + stopCalls);
        assertEquals(expectedCost, goesTo.getCost());

    }

}
