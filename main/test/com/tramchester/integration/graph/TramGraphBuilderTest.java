package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TFGMRouteNames;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@DataUpdateTest
class TramGraphBuilderTest {
    private static ComponentContainer componentContainer;

    private TransportData transportData;
    private ImmutableGraphTransaction txn;
    private StationRepository stationRepository;
    private ServiceRepository serviceRepository;

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

        tramRouteEcclesAshton = tramRouteHelper.getBlue(when);

        stationRepository = componentContainer.get(StationRepository.class);
        serviceRepository = componentContainer.get(ServiceRepository.class);
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
        Stream<ImmutableGraphRelationship> outboundLinks = cornbrookNode.getRelationships(txn, Direction.OUTGOING, LINKED);

        List<ImmutableGraphRelationship> list = outboundLinks.toList(); //Lists.newArrayList(outboundLinks);

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
            Stream<ImmutableGraphRelationship> boards = node.getRelationships(txn, Direction.OUTGOING, INTERCHANGE_BOARD);
            boards.forEach(board -> {
                Duration boardCost = board.getCost();
                assertEquals(Duration.ZERO, boardCost, "board cost wrong for " + platform);
            });

            Stream<ImmutableGraphRelationship> departs = node.getRelationships(txn, Direction.OUTGOING, INTERCHANGE_DEPART);
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
        Stream<ImmutableGraphRelationship> outboundLinks = altyNode.getRelationships(txn, Direction.OUTGOING, LINKED);

        List<ImmutableGraphRelationship> list = outboundLinks.toList();
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

        Stream<ImmutableGraphNode> interchangeNodes = txn.findNodes(GraphLabel.INTERCHANGE);

        IdSet<Station> fromDB = interchangeNodes.map(GraphNode::getStationId).collect(IdSet.idCollector());

        IdSet<Station> diffs = IdSet.disjunction(fromConfigAndDiscovered, fromDB);

        assertTrue(diffs.isEmpty(), "Diff was " + diffs + " between expected "
                + fromConfigAndDiscovered + " and DB " + fromDB);
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForNonInterchange() {
        Station navigationRoad = NavigationRoad.from(stationRepository);
        GraphNode node = txn.findNode(navigationRoad);
        Stream<ImmutableGraphRelationship> outboundLinks = node.getRelationships(txn, Direction.OUTGOING, LINKED);

        List<ImmutableGraphRelationship> list = outboundLinks.toList();
        assertEquals(2, list.size());

        Set<IdFor<Station>> destinations = list.stream().
                map(graphRelationship -> graphRelationship.getEndNode(txn)).
                map(GraphNode::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(Altrincham.getId()));
        assertTrue(destinations.contains(Timperley.getId()));
    }

    @Test
    void shouldHaveCorrectOutboundsAtMediaCity() {

        Station mediaCityUK = MediaCityUK.from(stationRepository);

        RouteStation routeStationMediaCityA = stationRepository.getRouteStation(mediaCityUK, tramRouteEcclesAshton);

        assertNotNull(routeStationMediaCityA);
        List<ImmutableGraphRelationship> outboundsFromRouteStation = txn.getRouteStationRelationships(routeStationMediaCityA, Direction.OUTGOING);

        IdSet<Service> graphSvcsFromRouteStations = outboundsFromRouteStation.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.TO_SERVICE)).
                map(GraphRelationship::getServiceId).
                collect(IdSet.idCollector());

        assertFalse(graphSvcsFromRouteStations.isEmpty());

        // check number of outbound services matches services in transport data files
        IdSet<Service> fileSvcIds = getTripsFor(transportData.getTrips(), mediaCityUK).stream().
                filter(trip -> trip.getRoute().equals(tramRouteEcclesAshton)).
                map(trip -> trip.getService().getId()).
                collect(IdSet.idCollector());

        assertEquals(graphSvcsFromRouteStations, fileSvcIds);

    }

    @Test
    void shouldHaveCorrectOutboundsServiceAndTripAtCornbrook() {

        Station cornbrook = Cornbrook.from(stationRepository);

        Route buryToAlty = tramRouteHelper.getGreen(when);

        List<ImmutableGraphRelationship> svcOutbounds = getOutboundsServicesForRouteStation(cornbrook, buryToAlty);
        assertFalse(svcOutbounds.isEmpty());

//        IdSet<Service> unique = svcOutbounds.stream().map(ImmutableGraphRelationship::getServiceId).
//                collect(IdSet.idCollector());

        // should have two outbound relationship for each svc, one towards each of the 2 neighbours
        //assertEquals(2 * unique.size(), svcOutbounds.size(), displayAllProps(svcOutbounds));

        // iff service relationship exists check that all expected trip ids are present

        svcOutbounds.
                forEach(svcRelationship -> {
                    IdFor<Service> svcId = svcRelationship.getServiceId();
                    IdFor<Station> towardsStationId = svcRelationship.getEndNode(txn).getTowardsStationId();
                    IdSet<Trip> tripIds = svcRelationship.getTripIds();
                    IdSet<Trip> expectedTrips = relevantTripsFor(svcId, buryToAlty, cornbrook, towardsStationId);
                    assertEquals(expectedTrips, tripIds, "trip mismatch for " + svcId + " towards " + towardsStationId);
                });

    }

    @Test
    void shouldHaveCorrectRelationshipsAtRouteStationsAlongTrip() {
        Station start = Bury.from(stationRepository);
        Station end = Altrincham.from(stationRepository);

        List<Trip> callingTrips = transportData.getTripsCallingAt(start, when).stream().
                filter(trip -> trip.callsAt(end.getId())).
                filter(trip -> trip.isAfter(start.getId(), end.getId()))
                .toList();

        assertFalse(callingTrips.isEmpty());

        callingTrips.forEach(trip -> {
            final Route route = trip.getRoute();
            final StopCalls stopCalls = trip.getStopCalls();
            final IdFor<Trip> tripId = trip.getId();

            int startSeqNum = stopCalls.getStopFor(start.getId()).getGetSequenceNumber();
            int endSeqNum = stopCalls.getStopFor(end.getId()).getGetSequenceNumber();
            assertTrue(endSeqNum>startSeqNum, "sanity check");

            for (int seqNum = startSeqNum; seqNum <= endSeqNum; seqNum++) {
                final StopCall stopCall = stopCalls.getStopBySequenceNumber(seqNum);

                final RouteStation routeStation = new RouteStation(stopCall.getStation(), route);
                final ImmutableGraphNode routeStationNode = txn.findNode(routeStation);
                assertNotNull(routeStationNode,"route station node");

                final List<ImmutableGraphRelationship> inboundLinks = routeStationNode.getRelationships(txn, Direction.INCOMING, TRAM_GOES_TO).toList();
                assertFalse(inboundLinks.isEmpty(), "inbound links");

                List<ImmutableGraphRelationship> matchingRelationships = inboundLinks.stream().
                        filter(relationship -> relationship.hasProperty(GraphPropertyKey.TRIP_ID)).
                        filter(relationship -> relationship.getTripId().equals(tripId)).
                        toList();

                if (seqNum == 1) {
                    assertTrue(matchingRelationships.isEmpty(),"trip inbound at start of trip");
                } else {
                    assertEquals(1, matchingRelationships.size(), "missing inbound for " + tripId + " at "
                            + routeStation.getId() + " seq " + seqNum);
                    ImmutableGraphRelationship relationship = matchingRelationships.getFirst();

                    final GraphNode minuteNode = relationship.getStartNode(txn);
                    assertEquals(tripId, minuteNode.getTripId(), "missing on minute node");
                }
            }
        });
    }

    @Test
    void shouldHaveExpectedRouteIdsForInboundAndOutboundAtRouteStation() {

        Station station = Timperley.from(stationRepository);

        final Set<Trip> callingTrips = transportData.getTripsCallingAt(station, when);

        final Set<Route> callingRoutes = callingTrips.stream().map(Trip::getRoute).collect(Collectors.toSet());

        // for end of line/route callingTrips.size() == terminateHere.size()

        callingRoutes.forEach(route -> {
            final RouteStation routeStation = new RouteStation(station, route);
            ImmutableGraphNode routeStationNode = txn.findNode(routeStation);
            assertNotNull(routeStationNode);

            IdSet<Route> incomingRoutes = routeStationNode.getRelationships(txn, Direction.INCOMING, TRAM_GOES_TO).
                    map(ImmutableGraphRelationship::getRouteId).
                    collect(IdSet.idCollector());

            assertEquals(1,incomingRoutes.size(), "Expected only " + route.getId() + " got " + incomingRoutes);
            assertTrue(incomingRoutes.contains(route.getId()), incomingRoutes + " is missing " + route.getId());

            IdSet<Route> outgoingRoutes = routeStationNode.getRelationships(txn, Direction.OUTGOING, TO_SERVICE).
                    map(ImmutableGraphRelationship::getRouteId).
                    collect(IdSet.idCollector());

            assertEquals(1,outgoingRoutes.size(), "Expected only " + route.getId() + " got " + outgoingRoutes);
            assertTrue(outgoingRoutes.contains(route.getId()));
        });
    }

    @Test
    void shouldHaveOutboundServiceForEveryInboundTrip() {

        transportData.getStations().forEach(station -> {
            // NOTE: No date filtering
            final Set<Trip> allCallingTrips = transportData.getTrips().stream().
                    filter(trip -> trip.callsAt(station.getId())).
                    collect(Collectors.toSet());

            final Set<Route> callingRoutes = allCallingTrips.stream().map(Trip::getRoute).collect(Collectors.toSet());

            // for end of line/route callingTrips.size() == terminateHere.size()

            callingRoutes.forEach(route -> {
                final RouteStation routeStation = new RouteStation(station, route);
                ImmutableGraphNode routeStationNode = txn.findNode(routeStation);
                assertNotNull(routeStationNode);

                final Set<Trip> callingTrips = allCallingTrips.stream().
                        filter(trip -> trip.getRoute().equals(route)).
                        collect(Collectors.toSet());

                final IdSet<Trip> terminateHere = callingTrips.stream().
                        filter(trip -> trip.getRoute().equals(route)).
                        filter(trip -> trip.lastStation().equals(station.getId())).
                        collect(IdSet.collector());

                final IdSet<Trip> startHere = callingTrips.stream().
                        filter(trip -> trip.getRoute().equals(route)).
                        filter(trip -> trip.firstStation().equals(station.getId())).
                        collect(IdSet.collector());

                IdSet<Trip> allNonTerminatedInboundTrips = routeStationNode.getRelationships(txn, Direction.INCOMING, TRAM_GOES_TO).
                        map(ImmutableGraphRelationship::getTripId).
                        filter(tripId -> !terminateHere.contains(tripId)).
                        collect(IdSet.idCollector());

                // will be empty at end of line
                //assertFalse(allNonTerminatedInboundTrips.isEmpty());

                boolean sanityCheck = allNonTerminatedInboundTrips.stream().
                        map(tripId -> transportData.getTripById(tripId)).
                        noneMatch(trip -> trip.lastStation().equals(station.getId()));

                assertTrue(sanityCheck);

                IdSet<Trip> allOutgoingTripsNotStartingHere = routeStationNode.getRelationships(txn, Direction.OUTGOING, TO_SERVICE).
                        flatMap(relationship -> relationship.getTripIds().stream()).
                        filter(tripId -> !startHere.contains(tripId)).
                        collect(IdSet.idCollector());

                // will be empty at end of line
                //assertFalse(allOutgoingTripsNotStartingHere.isEmpty());

                IdSet<Trip> disjunction = IdSet.disjunction(allNonTerminatedInboundTrips, allOutgoingTripsNotStartingHere);
                assertTrue(disjunction.isEmpty(), disjunction.toString());
            });
        });

    }


    @Test
    void shouldHaveExpectedServiceAndHourRelationshipsAtStation() {

        // address issue created when moved to bi-direction routes, instead of single direction
        // which led to service nodes having incorrect relationships with hour nodes

        Station station = Brooklands.from(transportData);
        int hour = 9;

        IdFor<Station> stationId = station.getId();

        Set<StopCall> towardsAlty = transportData.getTrips().stream().
                filter(trip -> trip.callsAt(stationId)).
                filter(trip -> trip.callsAt(Altrincham.getId())).
                filter(trip -> trip.isAfter(stationId, Altrincham.getId())).
                map(trip -> trip.getStopCalls().getStopFor(stationId)).collect(Collectors.toSet());

        TimeRange range = TimeRangePartial.of(TramTime.of(hour,0), TramTime.of(hour,59));
        Set<StopCall> duringHour = towardsAlty.stream().filter(stopCall -> range.contains(stopCall.getDepartureTime())).collect(Collectors.toSet());

        assertFalse(duringHour.isEmpty());

        duringHour.forEach(stopCall -> {
            Trip trip = stopCall.getTrip();
            Route route = trip.getRoute();

            RouteStation routeStation = new RouteStation(station, route);
            ImmutableGraphNode routeStationNode = txn.findNode(routeStation);

            assertNotNull(routeStationNode);

//            List<ImmutableGraphRelationship> toService = routeStationNode.getRelationships(txn, Direction.OUTGOING, TO_SERVICE).
//                    filter(relationship -> relationship.hasTripIdInList(trip.getId())).
//                    toList();
            List<ImmutableGraphRelationship> toService = routeStationNode.getOutgoingServiceMatching(txn, trip.getId()).toList();
            assertEquals(1, toService.size());

            GraphNode serviceNode = toService.getFirst().getEndNode(txn);

            assertEquals(trip.getService().getId(), serviceNode.getServiceId());

            List<ImmutableGraphRelationship> atHour = serviceNode.getRelationships(txn, Direction.OUTGOING, TO_HOUR).
                    filter(relationship -> relationship.getHour() == hour).toList();

            assertEquals(1, atHour.size(), "Did not find relationship for " + stopCall +
                    " at service " + trip.getService().getId() + " for route " + trip.getRoute().getId());
        });
    }

    @Test
    void shouldHaveInboundAndOutboundTripsForNonTerminatingRouteStations() {
        Station station = Brooklands.from(stationRepository);

        Set<Trip> callingTrips = transportData.getTripsCallingAt(station, when);

        Set<Route> callingRoutes = callingTrips.stream().map(Trip::getRoute).collect(Collectors.toSet());

        callingRoutes.forEach(route -> {
            final RouteStation routeStation = new RouteStation(station, route);
            ImmutableGraphNode routeStationNode = txn.findNode(routeStation);
            assertNotNull(routeStationNode);


            final List<ImmutableGraphRelationship> toServices = routeStationNode.getRelationships(txn, Direction.OUTGOING, TO_SERVICE).
                    filter(relationship -> relationship.getRouteId().equals(route.getId())).toList();
            assertFalse(toServices.isEmpty());

            IdSet<Service> uniqueServices = toServices.stream().map(ImmutableGraphRelationship::getServiceId).collect(IdSet.idCollector());

            // Not useful, i.e. [3, 3, 4, 5, 5, 5] = {3,4,5}
            // assertEquals(uniqueServices.size()*2, toServices.size());

            uniqueServices.forEach(svcId -> {
                final List<ImmutableGraphRelationship> relationshipsForSvc = toServices.stream().
                        filter(relationship -> relationship.getServiceId().equals(svcId)).toList();

                assertTrue(relationshipsForSvc.size()<3, "unexpected number of relationships for " + svcId);

                // if 2 links to service then trips to each on must be unique
                if (relationshipsForSvc.size()==2) {

                    ImmutableGraphRelationship relationshipA = relationshipsForSvc.get(0);
                    ImmutableGraphRelationship relationshipB = relationshipsForSvc.get(1);

                    IdSet<Trip> tripsA = relationshipA.getTripIds();
                    IdSet<Trip> tripsB = relationshipB.getTripIds();

                    assertTrue(IdSet.intersection(tripsA, tripsB).isEmpty());
                }
                // if 1 link to a service then it only runs in one direction i.e. a late night service

                final IdSet<Trip> fromMinuteTrips = routeStationNode.getRelationships(txn, Direction.INCOMING, TRAM_GOES_TO).
                        filter(relationship -> relationship.getServiceId().equals(svcId)).
                        map(ImmutableGraphRelationship::getTripId).
                        collect(IdSet.idCollector());

                assertFalse(fromMinuteTrips.isEmpty());

                // should have a corresponding outgoing service (i.e. trip matches) for each inbound from a minute node
                // in the case this is not the end of a line (TBC)
                relationshipsForSvc.forEach(relationship -> {
                    assertTrue(fromMinuteTrips.containsAll(relationship.getTripIds()),
                            "service did not have a corresponding inbound " + relationship.getServiceId());

                });

                IdSet<Trip> outboundTripsForService = relationshipsForSvc.stream().
                        flatMap(relationship -> relationship.getTripIds().stream()).
                        collect(IdSet.idCollector());

                assertFalse(outboundTripsForService.isEmpty());

                assertEquals(fromMinuteTrips.size(), outboundTripsForService.size(), "mismatch on in/out trips for " + svcId);

            });
        });
    }

    @Test
    void shouldHaveCorrectServiceRelationshipsAtRouteStationsAlongTrip() {
        Station start = Bury.from(stationRepository);
        Station end = Altrincham.from(stationRepository);

        List<Trip> callingTrips = transportData.getTripsCallingAt(start, when).stream().
                filter(trip -> trip.callsAt(end.getId())).
                filter(trip -> trip.isAfter(start.getId(), end.getId()))
                .toList();

        assertFalse(callingTrips.isEmpty());

        callingTrips.forEach(trip -> {
            final Route route = trip.getRoute();
            final StopCalls stopCalls = trip.getStopCalls();
            final IdFor<Trip> tripId = trip.getId();

            int startSeqNum = stopCalls.getStopFor(start.getId()).getGetSequenceNumber();
            int endSeqNum = stopCalls.getStopFor(end.getId()).getGetSequenceNumber();
            assertTrue(endSeqNum>startSeqNum, "sanity check");

            for (int seqNum = startSeqNum; seqNum <= endSeqNum; seqNum++) {
                StopCall stopCall = stopCalls.getStopBySequenceNumber(seqNum);

                RouteStation routeStation = new RouteStation(stopCall.getStation(), route);
                ImmutableGraphNode routeStationNode = txn.findNode(routeStation);
                assertNotNull(routeStationNode,"route station node");

                List<ImmutableGraphRelationship> toServices = routeStationNode.getRelationships(txn, Direction.OUTGOING, TO_SERVICE).toList();
                assertFalse(toServices.isEmpty(), "to service links");

//                List<ImmutableGraphRelationship> toServicesForTrip = toServices.stream().
//                        filter(toService -> toService.hasTripIdInList(tripId)).toList();

                List<ImmutableGraphRelationship> toServicesForTrip = routeStationNode.getOutgoingServiceMatching(txn, tripId).toList();

                if (seqNum!=endSeqNum) {

                    assertEquals(1, toServicesForTrip.size(), "wrong number TO_SERVICE for " + tripId
                            + " at " + routeStation + " seq " + seqNum);

                    ImmutableGraphRelationship toService = toServicesForTrip.getFirst();

                    assertEquals(route.getId(), toService.getRouteId(), "route id");
                    assertEquals(trip.getService().getId(), toService.getServiceId(), "service id");

                    GraphNode serviceNode = toService.getEndNode(txn);
                    assertEquals(route.getId(), serviceNode.getRouteId(), "route id");
                    assertEquals(trip.getService().getId(), serviceNode.getServiceId(), "service id");
                } else {
                    assertTrue(toServicesForTrip.isEmpty());
                }

            }
        });
    }

    @Test
    void shouldHaveEndOfTripAtEndOfLineStation() {
        Station bury = Bury.from(stationRepository);
        Route buryToAlty = tramRouteHelper.getGreen(when);

        ImmutableGraphNode node = txn.findNode(new RouteStation(bury, buryToAlty));

        List<ImmutableGraphRelationship> inboundRelationships = node.getRelationships(txn, Direction.INCOMING, TRAM_GOES_TO).toList();

        assertFalse(inboundRelationships.isEmpty());

        IdSet<Trip> uniqueTripIds = inboundRelationships.stream().
                map(ImmutableGraphRelationship::getTripId).
                collect(IdSet.idCollector());

        assertEquals(uniqueTripIds.size(), inboundRelationships.size());

        IdSet<Trip> terminateAtBury = uniqueTripIds.stream().
                filter(tripId -> transportData.getTripById(tripId).lastStation().equals(bury.getId())).
                collect(IdSet.idCollector());

        assertEquals(uniqueTripIds.size(), terminateAtBury.size(), "duplication of inbound trip present");

        // check minute nodes
//        Set<GraphNodeId> minuteNodeIds = inboundRelationships.stream().
//                map(relationship -> relationship.getStartNodeId(txn)).collect(Collectors.toSet());
//
//        assertEquals(-1, minuteNodeIds.size());

        // check no "terminating" trips present on outbound service links
        List<ImmutableGraphRelationship> outboundToSvc = node.getRelationships(txn, Direction.OUTGOING, TO_SERVICE).toList();

        IdSet<Trip> outboundTripIds = outboundToSvc.stream().
                flatMap(svcRelationship -> svcRelationship.getTripIds().stream()).
                collect(IdSet.idCollector());

        assertFalse(outboundTripIds.isEmpty());

        outboundTripIds.forEach(outboundTripId -> {
            assertFalse(uniqueTripIds.contains(outboundTripId), "unexpected outbound " + outboundTripIds);
        });
    }

    @Test
    void shouldHaveCorrectRelationshipsForServicesAtCornbrook() {

        Route route = tramRouteHelper.getGreen(when);

        List<ImmutableGraphRelationship> svcOutbounds = getOutboundsServicesForRouteStation(Cornbrook.from(stationRepository), route);
        assertFalse(svcOutbounds.isEmpty());

        svcOutbounds.forEach(svcRelationship -> {
            GraphNode serviceNode = svcRelationship.getEndNode(txn);

            List<ImmutableGraphRelationship> incoming = serviceNode.getRelationships(txn, Direction.INCOMING, TO_SERVICE).toList();

            assertTrue(incoming.contains(svcRelationship));
            assertEquals(1, incoming.size(), "Got more than one inbound for " + serviceNode + " from services " + incoming);

        });
    }

    @Test
    void shouldNotHaveHourNodesWithoutServiceRelationship() {

        List<ImmutableGraphNode> hourNodes = txn.findNodes(GraphLabel.HOUR).toList();

        Set<ImmutableGraphNode> haveLinks = hourNodes.stream().
                filter(hourNode -> hourNode.getRelationships(txn, Direction.INCOMING, TO_HOUR).findAny().isPresent()).
                collect(Collectors.toSet());

        assertEquals(hourNodes.size(), haveLinks.size());

    }

    @Test
    void shouldOnlyHaveOneServiceRelationshipInboundForEveryHourNode() {

        Stream<ImmutableGraphNode> hourNodes = txn.findNodes(GraphLabel.HOUR);

        Set<ImmutableGraphNode> tooMany = hourNodes.
                filter(hourNode -> hourNode.getRelationships(txn, Direction.INCOMING, TO_HOUR).count()>1).
                collect(Collectors.toSet());

        assertTrue(tooMany.isEmpty(), tooMany.toString());

    }

    @Test
    void shouldHaveAllTimeNodesWithLinkToRouteStation() {
        Stream<ImmutableGraphNode> timeNodes = txn.findNodes(GraphLabel.MINUTE);

        long missing = timeNodes.filter(timeNode -> !timeNode.hasRelationship(Direction.OUTGOING, TRAM_GOES_TO)).count();

        assertEquals(0, missing);
    }

    @Disabled("no longer have tripid on time nodes")
    @Test
    void shouldHaveAllTimeNodesWithLTripId() {
        Stream<ImmutableGraphNode> timeNodes = txn.findNodes(GraphLabel.MINUTE);

        long missing = timeNodes.filter(timeNode -> !timeNode.hasTripId()).count();

        assertEquals(0, missing);
    }

    @NotNull
    private List<ImmutableGraphRelationship> getOutboundsServicesForRouteStation(final Station station, final Route route) {
        RouteStation routeStation = stationRepository.getRouteStation(station, route);

        List<ImmutableGraphRelationship> outboundsFromRouteStation = txn.getRouteStationRelationships(routeStation, Direction.OUTGOING);

        return outboundsFromRouteStation.stream().filter(relationship -> relationship.isType(TO_SERVICE)).toList();
    }

    @Test
    void shouldHaveSameOutboundTripIdsForNeighbouringRouteStationWhenSameRouteAndSvc() {
        // outbound from manchester Timperley then Brooklands (not navigation road since some services "turn around" at timperley)
        Station stationA = Timperley.from(stationRepository);
        Station stationB = Brooklands.from(stationRepository);

        Route buryToAlty = tramRouteHelper.getGreen(when);

        RouteStation routeStationA = stationRepository.getRouteStation(stationA, buryToAlty);
        RouteStation routeStationB = stationRepository.getRouteStation(stationB, buryToAlty);

        Set<ImmutableGraphRelationship> svcOutboundsA = txn.getRouteStationRelationships(routeStationA, Direction.OUTGOING).
                stream().
                filter(relationship -> relationship.isType(TO_SERVICE)).
                collect(Collectors.toSet());
        assertFalse(svcOutboundsA.isEmpty());

        Set<ImmutableGraphRelationship> svcOutboundsB = txn.getRouteStationRelationships(routeStationB, Direction.OUTGOING).
                stream().
                filter(relationship -> relationship.isType(TO_SERVICE)).
                collect(Collectors.toSet());
        assertFalse(svcOutboundsB.isEmpty());

        IdSet<Service> stationAServices = svcOutboundsA.stream().map(ImmutableGraphRelationship::getServiceId).collect(IdSet.idCollector());
        IdSet<Service> stationBServices = svcOutboundsA.stream().map(ImmutableGraphRelationship::getServiceId).collect(IdSet.idCollector());

        IdSet<Service> differenceInRelationships = IdSet.disjunction(stationAServices, stationBServices);

        assertTrue(differenceInRelationships.isEmpty(), "not same set of services, diff was " + differenceInRelationships);

        assertFalse(stationAServices.isEmpty());

        // NOTE: Late night services can terminate in unexpected places...
        IdSet<Service> stationAServicesForDate = stationAServices.stream().
                map(serviceId -> serviceRepository.getServiceById(serviceId)).
                filter(service -> service.getCalendar().operatesOn(when)).
                filter(service -> service.getFinishTime().isBefore(TramTime.of(23,0))).
                collect(IdSet.collector());

        assertFalse(stationAServicesForDate.isEmpty());

        stationAServicesForDate.forEach(svcId -> {

            // from A towards B only
            List<ImmutableGraphRelationship> fromA = svcOutboundsA.stream().
                    filter(svcOutbound -> svcOutbound.getEndNode(txn).getTowardsStationId().equals(stationB.getId())).
                    filter(svcOutbound -> svcOutbound.getServiceId().equals(svcId)).toList();
            assertEquals(1, fromA.size(), "On " + when +" could not find " + svcId + " from A towards B " + stationAServicesForDate);

            // from B, excluding back towards A
            List<ImmutableGraphRelationship> fromB = svcOutboundsB.stream().
                    filter(svcOutbound -> !svcOutbound.getEndNode(txn).getTowardsStationId().equals(stationA.getId())).
                    filter(svcOutbounds -> svcOutbounds.getServiceId().equals(svcId)).toList();
            assertEquals(1, fromB.size(), "On " + when +" could not find " + svcId + " from B towards A " + stationAServicesForDate);

//            assertTrue(fromA || fromB, serviceRepository.getServiceById(svcId).toString());

            IdSet<Trip> tripsFromA = fromA.getFirst().getTripIds();
            IdSet<Trip> tripsFromB = fromB.getFirst().getTripIds();

            assertFalse(tripsFromA.isEmpty());
            assertFalse(tripsFromB.isEmpty());

            assertEquals(tripsFromA, tripsFromB);

        });

    }

    @NotNull
    private IdSet<Trip> relevantTripsFor(IdFor<Service> svcId, Route route, Station station, IdFor<Station> towards) {
        final IdFor<Station> stationId = station.getId();
        return transportData.getTrips().stream().
                filter(trip -> trip.getService().getId().equals(svcId)).
                filter(trip -> trip.getRoute().equals(route)).
                filter(trip -> trip.callsAt(stationId)).
                filter(trip -> trip.callsAt(towards)).
                filter(trip -> trip.isAfter(stationId, towards)).
                filter(trip -> !trip.lastStation().equals(stationId)).
                collect(IdSet.collector());
    }

    @Test
    void shouldHaveCorrectRelationshipsAtCornbrook() {

        final Station cornbrook = Cornbrook.from(stationRepository);

        Route tramRouteAltBury = tramRouteHelper.getGreen(when);

        RouteStation routeStationCornbrookAltyPiccRoute = stationRepository.getRouteStation(cornbrook, tramRouteAltBury);
        List<ImmutableGraphRelationship> outboundsA = txn.getRouteStationRelationships(routeStationCornbrookAltyPiccRoute, Direction.OUTGOING);

        assertTrue(outboundsA.size()>1, "have at least one outbound");

    }

    @Test
    void shouldHaveCorrectInboundsAtMediaCity() {

        checkInboundConsistency(MediaCityUK, TFGMRouteNames.Blue);

        checkInboundConsistency(HarbourCity, TFGMRouteNames.Blue);

        checkInboundConsistency(Broadway, TFGMRouteNames.Blue);

    }

    @Test
    void shouldCheckOutboundSvcRelationships() {

        checkOutboundConsistency(StPetersSquare, TFGMRouteNames.Green);

        checkOutboundConsistency(Cornbrook, TFGMRouteNames.Green);

        checkOutboundConsistency(MediaCityUK, TFGMRouteNames.Blue);

        checkOutboundConsistency(HarbourCity, TFGMRouteNames.Blue);

        // these two are not consistent because same svc can go different ways while still having same route code
        // i.e. service from harbour city can go to media city or to Broadway with same svc and route id
        // => end up with two outbound services instead of one, hence numbers looks different
        // graphAndFileConsistencyCheckOutbounds(Stations.Broadway.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        // graphAndFileConsistencyCheckOutbounds(Stations.HarbourCity.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
    }

    private void checkOutboundConsistency(TramStations tramStation, TFGMRouteNames knownRoute) {
        Station station = tramStation.from(stationRepository);
        Route route = tramRouteHelper.getOneRoute(knownRoute, when);

        assertNotNull(route, String.format("Could not find route %s for %s", knownRoute, when));

        checkOutboundConsistency(station, route);
    }

    private void checkOutboundConsistency(Station station, Route route) {
        RouteStation routeStation = stationRepository.getRouteStation(station, route);

        assertNotNull(routeStation, "Could not find route stations for " + station.getId() + " " + route.getId());

        List<ImmutableGraphRelationship> routeStationOutbounds = txn.getRouteStationRelationships(routeStation, Direction.OUTGOING);

        assertFalse(routeStationOutbounds.isEmpty());

        // since can have 'multiple' route stations due to dup routes use set here
       IdSet<Service> serviceRelatIds = routeStationOutbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.TO_SERVICE)).
                map(GraphRelationship::getServiceId).
                collect(IdSet.idCollector());

        Set<Trip> fileCallingTrips =
                transportData.getRouteById(route.getId()).getTrips().stream().

                filter(trip -> trip.getStopCalls().callsAt(station.getId())).
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

        List<ImmutableGraphRelationship> incomingToRouteStation = txn.getRouteStationRelationships(routeStation, Direction.INCOMING);
        long fromStation = incomingToRouteStation.stream().filter(relationship -> relationship.isType(STATION_TO_ROUTE)).count();
        assertNotEquals(0, fromStation);
    }

    @SuppressWarnings("SameParameterValue")
    private void checkInboundConsistency(TramStations tramStation, TFGMRouteNames knownRoute) {
        Route route = tramRouteHelper.getOneRoute(knownRoute, when);
        Station station = tramStation.from(stationRepository);

        checkInboundConsistency(station, route);
    }

    private void checkInboundConsistency(Station station, Route route) {
        RouteStation routeStation = stationRepository.getRouteStation(station, route);
        assertNotNull(routeStation, "Could not find a route for " + station.getId() + " and  " + route.getId());
        List<ImmutableGraphRelationship> inbounds = txn.getRouteStationRelationships(routeStation, Direction.INCOMING);

        List<ImmutableGraphRelationship> graphTramsIntoStation = inbounds.stream().
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
                filter(trip -> trip.callsAt(station.getId())). // calls at , but not starts at because no inbound for these
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
