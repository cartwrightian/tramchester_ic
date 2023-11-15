package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.ImmuableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import org.assertj.core.util.Streams;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class TramGraphBuilderTest {
    private static ComponentContainer componentContainer;

    private TransportData transportData;
    private MutableGraphTransaction txn;
    private StationRepository stationRepository;


    private Route tramRouteEcclesAshton;
    private TramRouteHelper tramRouteHelper;
    private TramDate when;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = componentContainer.get(TransportData.class);
        tramRouteHelper = new TramRouteHelper(transportData);

        when = TestEnv.testDay();

        tramRouteEcclesAshton = tramRouteHelper.getOneRoute(EcclesManchesterAshtonUnderLyne, when);

        stationRepository = componentContainer.get(StationRepository.class);
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);

        StagedTransportGraphBuilder builder = componentContainer.get(StagedTransportGraphBuilder.class);
        builder.getReady();
        txn = graphDatabase.beginTxMutable();
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
        Stream<GraphRelationship> outboundLinks = cornbrookNode.getRelationships(txn, Direction.OUTGOING, LINKED);

        List<GraphRelationship> list = outboundLinks.toList(); //Lists.newArrayList(outboundLinks);
        assertEquals(3, list.size());

        Set<IdFor<Station>> destinations = list.stream().
                map(graphRelationship -> graphRelationship.getEndNode(txn)).
                map(GraphNode::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(TraffordBar.getId()));
        assertTrue(destinations.contains(Pomona.getId()));
        assertTrue(destinations.contains(Deansgate.getId()));
    }

    @Test
    void shouldHaveCorrectPlatformCosts() {
        Station piccadilly = Piccadilly.from(stationRepository);
        Set<Platform> platforms = piccadilly.getPlatforms();

        Duration expectedCost = piccadilly.getMinChangeDuration();

        platforms.forEach(platform -> {
            GraphNode node = txn.findNode(platform);
            GraphRelationship leave = node.getSingleRelationship(txn, TransportRelationshipTypes.LEAVE_PLATFORM, Direction.OUTGOING);
            Duration leaveCost =  leave.getCost(); //GraphProps.getCost(leave);
            assertEquals(Duration.ZERO, leaveCost, "leave cost wrong for " + platform);

            GraphRelationship enter = node.getSingleRelationship(txn, TransportRelationshipTypes.ENTER_PLATFORM, Direction.INCOMING);
            Duration enterCost = enter.getCost(); //GraphProps.getCost(enter);
            assertEquals(expectedCost, enterCost, "wrong cost for " + platform.getId());
        });

        platforms.forEach(platform -> {
            GraphNode node = txn.findNode(platform);
            Stream<GraphRelationship> boards = node.getRelationships(txn, Direction.OUTGOING, INTERCHANGE_BOARD);
            boards.forEach(board -> {
                Duration boardCost = board.getCost();
                assertEquals(Duration.ZERO, boardCost, "board cost wrong for " + platform);
            });

            Stream<GraphRelationship> departs = node.getRelationships(txn, Direction.OUTGOING, INTERCHANGE_DEPART);
            departs.forEach(depart -> {
                Duration enterCost = depart.getCost();
                assertEquals(Duration.ZERO, enterCost, "depart wrong cost for " + platform.getId());
            });

        });
    }

    @Test
    void shouldHaveCorrectRouteStationToStationRouteCosts() {

        Set<RouteStation> routeStations = stationRepository.getRouteStationsFor(Piccadilly.getId());

        routeStations.forEach(routeStation -> {
            GraphNode node = txn.findNode(routeStation);

            GraphRelationship toStation = node.getSingleRelationship(txn, ROUTE_TO_STATION, Direction.OUTGOING);
            Duration costToStation = toStation.getCost(); //GraphProps.getCost(toStation);
            assertEquals(Duration.ZERO, costToStation, "wrong cost for " + routeStation);

            GraphRelationship fromStation = node.getSingleRelationship(txn, STATION_TO_ROUTE, Direction.INCOMING);
            Duration costFromStation = fromStation.getCost(); //GraphProps.getCost(fromStation);
            Duration expected = routeStation.getStation().getMinChangeDuration();
            assertEquals(expected, costFromStation, "wrong cost for " + routeStation);
        });
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForEndOfLine() {
        Station alty = Altrincham.from(stationRepository);
        GraphNode altyNode = txn.findNode(alty);
        Stream<GraphRelationship> outboundLinks = altyNode.getRelationships(txn, Direction.OUTGOING, LINKED);

        List<GraphRelationship> list = outboundLinks.toList();
        assertEquals(1, list.size());

        Set<IdFor<Station>> destinations = list.stream().
                map(graphRelationship -> graphRelationship.getEndNode(txn)).
                map(GraphNode::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(NavigationRoad.getId()));
    }

    @Test
    void shouldHaveOneNodePerRouteStation() {
        stationRepository.getRouteStations().forEach(routeStation -> {
            GraphNode found = txn.findNode(routeStation);
            assertNotNull(found, routeStation.getId().toString());
        });
    }

    @Test
    void shouldHaveExpectedInterchangesInTheGraph() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);

        IdSet<Station> fromConfigAndDiscovered = interchangeRepository.getAllInterchanges().stream().
                map(InterchangeStation::getStationId).collect(IdSet.idCollector());

        Stream<ImmuableGraphNode> interchangeNodes = txn.findNodes(GraphLabel.INTERCHANGE);

        IdSet<Station> fromDB = interchangeNodes.map(GraphNode::getStationId).collect(IdSet.idCollector());

        IdSet<Station> diffs = IdSet.disjunction(fromConfigAndDiscovered, fromDB);

        assertTrue(diffs.isEmpty(), "Diff was " + diffs + " between expected "
                + fromConfigAndDiscovered + " and DB " + fromDB);
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForNonInterchange() {
        Station exchangeSq = ExchangeSquare.from(stationRepository);
        GraphNode exchangeSqNode = txn.findNode(exchangeSq);
        Stream<GraphRelationship> outboundLinks = exchangeSqNode.getRelationships(txn, Direction.OUTGOING, LINKED);

        List<GraphRelationship> list = outboundLinks.toList();
        assertEquals(2, list.size());

        Set<IdFor<Station>> destinations = list.stream().
                map(graphRelationship -> graphRelationship.getEndNode(txn)).
                map(GraphNode::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(Victoria.getId()));
        assertTrue(destinations.contains(StPetersSquare.getId()));
    }

    @Test
    void shouldHaveCorrectOutboundsAtMediaCity() {

        Station mediaCityUK = MediaCityUK.from(stationRepository);

        RouteStation routeStationMediaCityA = stationRepository.getRouteStation(mediaCityUK, tramRouteEcclesAshton);
        List<GraphRelationship> outboundsFromRouteStation = txn.getRouteStationRelationships(routeStationMediaCityA, Direction.OUTGOING);

        IdSet<Service> graphSvcsFromRouteStations = outboundsFromRouteStation.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.TO_SERVICE)).
                map(GraphRelationship::getServiceId).
                collect(IdSet.idCollector());

        // check number of outbound services matches services in transport data files
        IdSet<Service> fileSvcIds = getTripsFor(transportData.getTrips(), mediaCityUK).stream().
                filter(trip -> trip.getRoute().equals(tramRouteEcclesAshton)).
                map(trip -> trip.getService().getId()).
                collect(IdSet.idCollector());

        assertEquals(graphSvcsFromRouteStations, fileSvcIds);
    }

    @Test
    void shouldHaveCorrectRelationshipsAtCornbrook() {

        final Station cornbrook = Cornbrook.from(stationRepository);

        Route tramRouteAltBury = tramRouteHelper.getOneRoute(KnownTramRoute.BuryManchesterAltrincham, when);

        RouteStation routeStationCornbrookAltyPiccRoute = stationRepository.getRouteStation(cornbrook, tramRouteAltBury);
        List<GraphRelationship> outboundsA = txn.getRouteStationRelationships(routeStationCornbrookAltyPiccRoute, Direction.OUTGOING);

        assertTrue(outboundsA.size()>1, "have at least one outbound");

    }

    @Test
    void shouldHaveCorrectInboundsAtMediaCity() {

        checkInboundConsistency(MediaCityUK, EcclesManchesterAshtonUnderLyne);

        checkInboundConsistency(HarbourCity, EcclesManchesterAshtonUnderLyne);

        checkInboundConsistency(Broadway, EcclesManchesterAshtonUnderLyne);

    }

    @Test
    void shouldCheckOutboundSvcRelationships() {

        checkOutboundConsistency(StPetersSquare, BuryManchesterAltrincham);

        checkOutboundConsistency(Cornbrook, BuryManchesterAltrincham);

        checkOutboundConsistency(MediaCityUK, EcclesManchesterAshtonUnderLyne);

        checkOutboundConsistency(HarbourCity, EcclesManchesterAshtonUnderLyne);

        // these two are not consistent because same svc can go different ways while still having same route code
        // i.e. service from harbour city can go to media city or to Broadway with same svc and route id
        // => end up with two outbound services instead of one, hence numbers looks different
        // graphAndFileConsistencyCheckOutbounds(Stations.Broadway.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        // graphAndFileConsistencyCheckOutbounds(Stations.HarbourCity.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
    }

    private void checkOutboundConsistency(TramStations tramStation, KnownTramRoute knownRoute) {
        Station station = tramStation.from(stationRepository);
        Route route = tramRouteHelper.getOneRoute(knownRoute, when);

        assertNotNull(route, String.format("Could not find route %s for %s", knownRoute, when));

        checkOutboundConsistency(station, route);
    }

    private void checkOutboundConsistency(Station station, Route route) {
        RouteStation routeStation = stationRepository.getRouteStation(station, route);

        assertNotNull(routeStation, "Could not find route stations for " + station.getId() + " " + route.getId());

        List<GraphRelationship> routeStationOutbounds = txn.getRouteStationRelationships(routeStation, Direction.OUTGOING);

        assertFalse(routeStationOutbounds.isEmpty());

        // since can have 'multiple' route stations due to dup routes use set here
       IdSet<Service> serviceRelatIds = routeStationOutbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.TO_SERVICE)).
                map(GraphRelationship::getServiceId).
                collect(IdSet.idCollector());

        Set<Trip> fileCallingTrips =
                transportData.getRouteById(route.getId()).getTrips().stream().

                filter(trip -> trip.getStopCalls().callsAt(station)).
                collect(Collectors.toSet());

        IdSet<Service> fileSvcIdFromTrips = fileCallingTrips.stream().
                map(trip -> trip.getService().getId()).
                collect(IdSet.idCollector());

        // NOTE: Check clean target that and graph has been rebuilt if see failure here
        assertEquals(fileSvcIdFromTrips.size(), serviceRelatIds.size(),
                "Did not match " + fileSvcIdFromTrips + " and " + serviceRelatIds);
        assertTrue(fileSvcIdFromTrips.containsAll(serviceRelatIds));

        long connectedToRouteStation = routeStationOutbounds.stream().filter(relationship -> relationship.isType(ROUTE_TO_STATION)).count();
        assertNotEquals(0, connectedToRouteStation);

        List<GraphRelationship> incomingToRouteStation = txn.getRouteStationRelationships(routeStation, Direction.INCOMING);
        long fromStation = Streams.stream(incomingToRouteStation).filter(relationship -> relationship.isType(STATION_TO_ROUTE)).count();
        assertNotEquals(0, fromStation);
    }

    @SuppressWarnings("SameParameterValue")
    private void checkInboundConsistency(TramStations tramStation, KnownTramRoute knownRoute) {
        Route route = tramRouteHelper.getOneRoute(knownRoute, when);
        Station station = tramStation.from(stationRepository);

        checkInboundConsistency(station, route);
    }

    private void checkInboundConsistency(Station station, Route route) {
        RouteStation routeStation = stationRepository.getRouteStation(station, route);
        assertNotNull(routeStation, "Could not find a route for " + station.getId() + " and  " + route.getId());
        List<GraphRelationship> inbounds = txn.getRouteStationRelationships(routeStation, Direction.INCOMING);

        List<GraphRelationship> graphTramsIntoStation = inbounds.stream().
                filter(inbound -> inbound.isType(TransportRelationshipTypes.TRAM_GOES_TO)).toList();

        long boardingCount = inbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.BOARD)
                        || relationship.isType(TransportRelationshipTypes.INTERCHANGE_BOARD)).count();
        // now 2 due to one route station and N platforms
        assertEquals(2, boardingCount);

        SortedSet<IdFor<Service>> graphInboundSvcIds = graphTramsIntoStation.stream().
                map(GraphRelationship::getServiceId).collect(Collectors.toCollection(TreeSet::new));

        Set<Trip> callingTrips =
                transportData.getRouteById(route.getId()).getTrips().stream().
                filter(trip -> trip.callsAt(station)). // calls at , but not starts at because no inbound for these
                //filter(trip -> !trip.getStopCalls().getStopBySequenceNumber(trip.getSeqNumOfFirstStop()).getStation().equals(station)).
                filter(trip -> !trip.getStopCalls().getFirstStop().getStation().equals(station)).
                collect(Collectors.toSet());

        SortedSet<IdFor<Service>> svcIdsFromCallingTrips = callingTrips.stream().
                map(trip -> trip.getService().getId()).collect(Collectors.toCollection(TreeSet::new));

        assertEquals(svcIdsFromCallingTrips, graphInboundSvcIds);

        Set<IdFor<Trip>> graphInboundTripIds = graphTramsIntoStation.stream().
                map(GraphRelationship::getTripId).
                collect(Collectors.toSet());

        assertEquals(graphTramsIntoStation.size(), graphInboundTripIds.size()); // should have an inbound link per trip

        Set<IdFor<Trip>> tripIdsFromFile = callingTrips.stream().
                map(Trip::getId).
                collect(Collectors.toSet());

        tripIdsFromFile.removeAll(graphInboundTripIds);
        assertEquals(0, tripIdsFromFile.size());
    }
}
