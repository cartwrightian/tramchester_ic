package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.StationTime;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.*;
import com.tramchester.graph.databaseManagement.GraphDatabaseMetaInfo;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphRelationship;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.caching.*;
import com.tramchester.metrics.Timing;
import com.tramchester.repository.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.graphbuild.GraphLabel.INTERCHANGE;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

@LazySingleton
public class StagedTransportGraphBuilder extends GraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(StagedTransportGraphBuilder.class);

    ///
    // Station -[enter]-> Platform -[board]-> RouteStation -[toSvc]-> Service -> Hour-[toMinute]->
    //          -> Minute -> RouteStation-[depart]-> Platform -[leave]-> Station
    //
    // OR
    //
    // Station -[board]-> RouteStation -[toSvc]-> Service -> Hour-[toMinute]->
    //          -> Minute -> RouteStation-[depart]-> Station
    //
    // RouteStation-[onRoute]->RouteStation
    //
    ///

    private final TramchesterConfig config;
    private final TransportData transportData;
    private final InterchangeRepository interchangeRepository;
    private final GraphDatabaseMetaInfo databaseMetaInfo;
    private final StopCallRepository stopCallRepository;
    private final StationsWithDiversionRepository stationsWithDiversionRepository;

    // force contsruction via guice to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    @Inject
    public StagedTransportGraphBuilder(GraphDatabase graphDatabase, TramchesterConfig config, GraphFilter graphFilter,
                                       TransportData transportData, InterchangeRepository interchangeRepository,
                                       GraphBuilderCache builderCache,
                                       @SuppressWarnings("unused") StationsAndLinksGraphBuilder.Ready stationAndLinksBuilt,
                                       @SuppressWarnings("unused") AddNeighboursGraphBuilder.Ready neighboursReady,
                                       @SuppressWarnings("unused") AddWalksForClosedGraphBuilder.Ready walksForClosedReady,
                                       GraphDatabaseMetaInfo databaseMetaInfo, StopCallRepository stopCallRepository,
                                       StationsWithDiversionRepository stationsWithDiversionRepository) {
        super(graphDatabase, graphFilter, config, builderCache);
        this.config = config;
        this.transportData = transportData;
        this.interchangeRepository = interchangeRepository;
        this.databaseMetaInfo = databaseMetaInfo;
        this.stopCallRepository = stopCallRepository;
        this.stationsWithDiversionRepository = stationsWithDiversionRepository;
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        if (graphDatabase.isCleanDB()) {
            logger.info("Rebuild of TimeTable graph DB for " + graphDBConfig.getDbPath());
            if (graphFilter.isFiltered()) {
                logger.warn("Graph is filtered " + graphFilter);
            }
            buildGraphWithFilter(graphDatabase);
            graphDatabase.waitForIndexes();
            logger.info("Graph rebuild is finished for " + graphDBConfig.getDbPath());
        } else {
            logger.info("No rebuild of graph");
            graphDatabase.waitForIndexes();
        }
        logger.info("started");
    }

    private void buildGraphWithFilter(GraphDatabase graphDatabase) {
        logger.info("Building graph for data source: " + transportData.summariseDataSourceInfo());
        logMemory("Before graph build");

        try(Timing ignored = new Timing(logger, "Graph rebuild")) {

            StationAndPlatformNodeCache stationAndPlatformNodeCache = getStationAndPlatformNodeCache();
            RouteStationNodeCache routeStationNodeCache = getRouteStationNodeCache();

            BoardingDepartNodeCache boardingDepartNodeCache = new BoardingDepartNodeCache();

            // just for tfgm trams currently
            linkStationsAndPlatforms(stationAndPlatformNodeCache);

            // TODO Agencies could be done in parallel as should be no overlap except at station level?
            for(Agency agency : transportData.getAgencies()) {
                if (graphFilter.shouldIncludeAgency(agency)) {
                    try (Timing ignored1 = new Timing(logger,"Add agency " + agency.getId() + " " + agency.getName())) {
                        buildForAgency(graphDatabase, agency, stationAndPlatformNodeCache, routeStationNodeCache, boardingDepartNodeCache);
                    }
                }
            }

            // only add version node if we manage to build graph, so partial builds that fail cause a rebuild
            addVersionNode(graphDatabase, transportData);

        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
            throw new RuntimeException("Unable to build graph", except);
        }

        super.fullClearCache();
        reportStats();
        System.gc(); // for testing, was causing issue on the main test run
        logMemory("After graph build");
    }

    private void linkStationsAndPlatforms(StationAndPlatformNodeCache stationAndPlatformNodeCache) {

        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "link stations & platforms")) {
            MutableGraphTransaction txn = timedTransaction.transaction();
            transportData.getActiveStationStream().
                    filter(Station::hasPlatforms).
                    filter(graphFilter::shouldInclude).
                    forEach(station -> linkStationAndPlatforms(txn, station, stationAndPlatformNodeCache));
            timedTransaction.commit();
        }
    }

    private void addVersionNode(GraphDatabase graphDatabase, DataSourceRepository infos) {
        if (!infos.hasDataSourceInfo()) {
            logger.error("No data source info was provided, version will not be set in the DB");
            return;
        }

        try(MutableGraphTransaction tx = graphDatabase.beginTxMutable()) {
            logger.info("Adding version node to the DB");
            databaseMetaInfo.createVersionNode(tx, infos);
            tx.commit();
        }
    }

    private void buildForAgency(final GraphDatabase graphDatabase, final Agency agency, StationAndPlatformNodeCache stationAndPlatformCache,
                                RouteStationNodeCache routeStationNodeCache, BoardingDepartNodeCache boardingDepartNodeCache) {

        // serviceNodeCache and hourNodeCache
        AgencyBuilderNodeCache agencyBuilderNodeCache = new AgencyBuilderNodeCache();

        if (getRoutesForAgency(agency).findAny().isEmpty()) {
            return;
        }

        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "onRoute for " + agency.getId())) {
            MutableGraphTransaction tx = timedTransaction.transaction();
            getRoutesForAgency(agency).forEach(route -> createOnRouteRelationships(tx, route, routeStationNodeCache));
            timedTransaction.commit();
        }

        try(Timing ignored = new Timing(logger,"service, hour for " + agency.getId())) {
            // removed the parallel, undefined behaviours with shared txn
            getRoutesForAgency(agency).forEach(route -> {
                try (MutableGraphTransaction tx = graphDatabase.beginTxMutable()) {
                    createServiceAndHourNodesForRoute(tx, route, routeStationNodeCache, agencyBuilderNodeCache, agencyBuilderNodeCache);
                    tx.commit();
                }
            });
        }

        try(Timing ignored = new Timing(logger,"time and update for trips for " + agency.getId())) {
            // removed the parallel, undefined behaviours with shared txn

            BatchTransactionStrategy transactionStrategy = new BatchTransactionStrategy(graphDatabase,500);
            getRoutesForAgency(agency).forEach(route -> {
//                int numTrips = route.getNumberTrips();
//                final TransactionStrategy transactionStrategy = new RouteTripTransactionStrategy(graphDatabase, config, numTrips);
                transactionStrategy.routeBegin(route);
                createMinuteNodesAndRecordUpdatesForTrips(transactionStrategy, route, agencyBuilderNodeCache, routeStationNodeCache);
                transactionStrategy.routeDone();
            });
            transactionStrategy.close();
        }

        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "boards & departs for " + agency.getId())) {
            MutableGraphTransaction tx = timedTransaction.transaction();
            getRoutesForAgency(agency).forEach(route -> buildGraphForBoardsAndDeparts(route, tx, stationAndPlatformCache, routeStationNodeCache,
                    boardingDepartNodeCache));
            timedTransaction.commit();
        }

        agencyBuilderNodeCache.clear();
    }

    @NotNull
    private Stream<Route> getRoutesForAgency(Agency agency) {
        return agency.getRoutes().stream().filter(graphFilter::shouldIncludeRoute);
    }

    private void createMinuteNodesAndRecordUpdatesForTrips(TransactionStrategy strategy, Route route,
                                                           HourNodeCache hourNodeCache,
                                                           RouteStationNodeCache routeStationNodeCache) {
        // time nodes and relationships for trips
        for (Trip trip : route.getTrips()) {
            strategy.tripBegin(trip);
            final MutableGraphTransaction tx = strategy.currentTxn();
            final Map<StationTime, MutableGraphNode> timeNodes = createMinuteNodes(tx, trip, hourNodeCache);
            createTripRelationships(tx, route, trip, routeStationNodeCache, timeNodes);
            timeNodes.clear();
            strategy.tripDone();
        }
    }

    private void createTripRelationships(MutableGraphTransaction tx, Route route, Trip trip,
                                         RouteStationNodeCache routeBuilderCache,
                                         Map<StationTime, MutableGraphNode> timeNodes) {
        StopCalls stops = trip.getStopCalls();

        stops.getLegs(graphFilter.isFiltered()).forEach(leg -> {
            if (includeBothStops(leg)) {
                StopCall first = leg.getFirst();
                StopCall second = leg.getSecond();
                createRelationshipTimeNodeToRouteStation(tx, route, trip, first, second, routeBuilderCache, timeNodes);
            }
        });
    }

    private void buildGraphForBoardsAndDeparts(Route route, MutableGraphTransaction tx, StationAndPlatformNodeCache stationAndPlatformCache,
                                               RouteStationNodeCache routeStationNodeCache, BoardingDepartNodeCache boardingDepartNodeCache) {
        for (Trip trip : route.getTrips()) {
            trip.getStopCalls().stream().
                    filter(graphFilter::shouldInclude).
                    forEach(stopCall -> createBoardingAndDepart(tx, stopCall, route, trip, stationAndPlatformCache,
                            routeStationNodeCache, boardingDepartNodeCache));
        }
    }

    private void createServiceAndHourNodesForRoute(MutableGraphTransaction tx, Route route,
                                                   RouteStationNodeCache routeStationNodeCache, ServiceNodeCache serviceNodeCache,
                                                   HourNodeCache hourNodeCache) {
        // TODO Create and return hour node cache from
        route.getTrips().forEach(trip -> {
                final StopCalls stops = trip.getStopCalls();
                final List<StopCalls.StopLeg> legs = stops.getLegs(graphFilter.isFiltered());
                legs.forEach(leg -> {

                    if (includeBothStops(leg)) {
                        if (!leg.getDepartureTime().isValid()) {
                            throw new RuntimeException("Invalid dept time for " + leg);
                        }

                        final IdFor<Station> beginId = leg.getFirstStation().getId();
                        final IdFor<Station> endId = leg.getSecondStation().getId();

                        final Service service = trip.getService();
                        final MutableGraphNode serviceNode = createServiceNodeAndRelationshipFromRouteStation(tx, route, service,
                                beginId, endId, routeStationNodeCache, serviceNodeCache);

                        createHourNodeAndRelationshipFromService(tx, route.getId(), service,
                                beginId, leg.getDepartureTime().getHourOfDay(), hourNodeCache, serviceNode);
                    }
                });
        });
    }

    private MutableGraphNode createServiceNodeAndRelationshipFromRouteStation(MutableGraphTransaction tx, Route route, Service service,
                                                                              IdFor<Station> beginId, IdFor<Station> endId,
                                                                              RouteStationNodeCache routeStationNodeCache,
                                                                              ServiceNodeCache serviceNodeCache) {

        if (serviceNodeCache.hasServiceNode(route.getId(), service, beginId, endId)) {
            return serviceNodeCache.getServiceNode(tx, route.getId(), service, beginId, endId);
        }

        // Node for the service
        // -route ID here as some towardsServices can go via multiple routes, this seems to be associated with the depots
        // -some towardsServices can go in two different directions from a station i.e. around Media City UK

        MutableGraphNode svcNode =  tx.createNode(GraphLabel.SERVICE); //createGraphNodeOld(tx, GraphLabel.SERVICE);
        svcNode.set(service);
        svcNode.set(route);
        // TODO This is used to look up station and hence lat/long for distance ordering, store
        //  org.neo4j.graphdb.spatial.Point instead?
        svcNode.setTowards(endId);

        serviceNodeCache.putService(route.getId(), service, beginId, endId, svcNode);

        // start route station -> svc node
        MutableGraphNode routeStationStart = routeStationNodeCache.getRouteStation(tx, route, beginId);
        MutableGraphRelationship svcRelationship = createRelationship(tx, routeStationStart, svcNode, TO_SERVICE);
        svcRelationship.set(service);
        svcRelationship.setCost(Duration.ZERO);
        svcRelationship.set(route);
        return svcNode;

    }

    private void linkStationAndPlatforms(MutableGraphTransaction txn, Station station, StationAndPlatformNodeCache stationAndPlatformNodeCache) {

        MutableGraphNode stationNode = stationAndPlatformNodeCache.getStation(txn, station.getId());
        if (stationNode!=null) {
            for (Platform platform : station.getPlatforms()) {
                MutableGraphNode platformNode = stationAndPlatformNodeCache.getPlatform(txn, platform.getId());
                createPlatformStationRelationships(station, stationNode, platform, platformNode, txn);
            }
        } else {
            throw new RuntimeException("Missing station node for " + station);
        }
    }

    private void createOnRouteRelationships(final MutableGraphTransaction tx, final Route route, final RouteStationNodeCache routeBuilderCache) {

//        final TransportMode routeTransportMode = route.getTransportMode();
//        final boolean isBusRoute = routeTransportMode == TransportMode.Bus;

        final Set<StopCalls.StopLeg> pairs = new HashSet<>();
        route.getTrips().forEach(trip -> {
            final StopCalls stops = trip.getStopCalls();
            stops.getLegs(graphFilter.isFiltered()).forEach(leg -> {
                if (includeBothStops(leg)) {
                    pairs.add(leg);
//                    if (!pairs.contains(leg)) {
//                        // TODO need cost representative of the route as whole
//                        final Duration cost = leg.getCost();
//                        if (cost.isZero() && !isBusRoute) {
//                            // this can happen a lot for buses
//                            logger.warn(format("Zero cost for trip %s for %s", trip.getId(), leg));
//                        }
//                        pairs.put(leg, cost);
//                    }
                }
            });
        });

        pairs.forEach((leg) -> {
            final IdFor<Station> beginId = leg.getFirstStation().getId();
            final IdFor<Station> endId = leg.getSecondStation().getId();
            if (!routeBuilderCache.hasRouteStation(route, beginId)) {
                String message = format("Missing first route station (%s, %s) in cache for route: %s and leg: %s",
                        route.getId(), beginId, route, leg);
                throw new RuntimeException(message);
            }
            if (!routeBuilderCache.hasRouteStation(route, endId)) {
                String message = format("Missing second route station (%s, %s) in cache for %s and %s",
                        route.getId(), beginId, route, leg.getFirst());
                throw new RuntimeException(message);
            }
            final MutableGraphNode startNode = routeBuilderCache.getRouteStation(tx, route, beginId);
            final MutableGraphNode endNode = routeBuilderCache.getRouteStation(tx, route, endId);

            final StopCallRepository.Costs costs = stopCallRepository.getCostsBetween(route, leg.getFirstStation(), leg.getSecondStation());

            createOnRouteRelationship(startNode, endNode, route, costs, tx);
        });
}

    private boolean includeBothStops(StopCalls.StopLeg leg) {
        return graphFilter.shouldInclude(leg.getFirst()) && graphFilter.shouldInclude(leg.getSecond());
    }

    private void createBoardingAndDepart(MutableGraphTransaction tx, StopCall stopCall,
                                         Route route, Trip trip,
                                         StationAndPlatformNodeCache stationAndPlatformNodeCache,
                                         RouteStationNodeCache routeStationNodeCache,
                                         BoardingDepartNodeCache boardingDepartNodeCache) {

        if (!stopCall.callsAtStation()) {
            if (route.getTransportMode()==Tram) {
                logger.warn("No pickup or drop-off for " + stopCall);
            }
            return;
        }

        boolean pickup = stopCall.getPickupType().equals(GTFSPickupDropoffType.Regular);
        boolean dropoff = stopCall.getDropoffType().equals(GTFSPickupDropoffType.Regular);

        Station station = stopCall.getStation();

        // TODO when filtering this isn't really valid, we might only see a small segment of a larger trip....
        // In unfiltered situations (i.e. not testing) it is fine

        boolean isFirstStop = stopCall.equals(trip.getStopCalls().getFirstStop());
        if (isFirstStop && dropoff && !trip.isFiltered()) {
            String msg = "Drop off at first station for stop " + station.getId() + " trip " + trip.getId() + " " + stopCall.getDropoffType()
                    + " seq:" + stopCall.getGetSequenceNumber();
            logger.debug(msg);
        }

        boolean isLastStop = stopCall.equals(trip.getStopCalls().getLastStop());
        if (isLastStop && pickup && !trip.isFiltered()) {
            String msg = "Pick up at last station for stop " + station.getId() + " trip " + trip.getId() + " " + stopCall.getPickupType()
                    + " seq:" + stopCall.getGetSequenceNumber();
            logger.debug(msg);
        }

        boolean isInterchange = interchangeRepository.isInterchange(station);

        // If bus we board to/from station, for trams it is from the platform
        MutableGraphNode platformOrStation = station.hasPlatforms() ? stationAndPlatformNodeCache.getPlatform(tx, stopCall.getPlatform().getId())
                : stationAndPlatformNodeCache.getStation(tx, station.getId());
        IdFor<RouteStation> routeStationId = RouteStation.createId(station.getId(), route.getId());
        MutableGraphNode routeStationNode = routeStationNodeCache.getRouteStation(tx, routeStationId);

        if (isInterchange) {
            routeStationNode.addLabel(INTERCHANGE);
        }

        // boarding: platform/station ->  callingPoint , NOTE: no boarding at the last stop of a trip
        if (pickup && !boardingDepartNodeCache.hasBoarding(platformOrStation.getId(), routeStationNode.getId())) {
            createBoarding(boardingDepartNodeCache, stopCall, route, station, isInterchange, platformOrStation, routeStationId,
                    routeStationNode, tx);
        }

        // leave: route station -> platform/station , NOTE: no towardsStation at first stop of a trip
        if (dropoff && !boardingDepartNodeCache.hasDeparts(platformOrStation.getId(), routeStationNode.getId()) ) {
            createDeparts(boardingDepartNodeCache, station, isInterchange, platformOrStation, routeStationId, routeStationNode, tx);
        }

    }

    private void createDeparts(BoardingDepartNodeCache boardingDepartNodeCache, Station station, boolean isInterchange,
                               MutableGraphNode boardingNode, IdFor<RouteStation> routeStationId,
                               MutableGraphNode routeStationNode, MutableGraphTransaction txn) {

        TransportRelationshipTypes departType;
        if (isInterchange) {
            departType = INTERCHANGE_DEPART;
        } else if (stationsWithDiversionRepository.hasDiversions(station)) {
            departType = DIVERSION_DEPART;
        } else {
            departType = DEPART;
        }

        Duration departCost = Duration.ZERO;

        MutableGraphRelationship departRelationship = createRelationship(txn, routeStationNode, boardingNode, departType);
        departRelationship.setCost(departCost);
        departRelationship.setCost(departCost);
        departRelationship.setRouteStationId(routeStationId);
        departRelationship.set(station);
        boardingDepartNodeCache.putDepart(boardingNode.getId(), routeStationNode.getId());

        if (departType.equals(DIVERSION_DEPART)) {
            Set<DateRange> ranges = stationsWithDiversionRepository.getDateRangesFor(station);
            ranges.forEach(departRelationship::setDateRange);
        }
    }

    private void createBoarding(BoardingDepartNodeCache boardingDepartNodeCache, StopCall stop, Route route, Station station,
                                boolean isInterchange, MutableGraphNode platformOrStation, IdFor<RouteStation> routeStationId,
                                MutableGraphNode routeStationNode, MutableGraphTransaction txn) {
        TransportRelationshipTypes boardType = isInterchange ? INTERCHANGE_BOARD : BOARD;
        MutableGraphRelationship boardRelationship = createRelationship(txn, platformOrStation, routeStationNode, boardType);

        Duration boardCost = Duration.ZERO;

        boardRelationship.setCost(boardCost);
        boardRelationship.setRouteStationId(routeStationId);
        boardRelationship.set(route);
        boardRelationship.set(station);
        // No platform ID on buses
        if (stop.hasPlatfrom()) {
            boardRelationship.set(stop.getPlatform());
        }
        boardingDepartNodeCache.putBoarding(platformOrStation.getId(), routeStationNode.getId());
    }

    private void createOnRouteRelationship(final MutableGraphNode from, final MutableGraphNode to, final Route route,
                                           final StopCallRepository.Costs costs,
                                           final MutableGraphTransaction txn) {

        final Set<GraphNodeId> endNodesIds;
        if (from.hasRelationship(OUTGOING, ON_ROUTE)) {
            // diff outbounds for same route actually a normal situation, where (especially) trains go via
            // different paths even thought route is the "same", or back to the depot
            endNodesIds = from.getRelationships(txn, OUTGOING, ON_ROUTE).
                    map(relationship -> relationship.getEndNodeId(txn)).
                    collect(Collectors.toSet());
        } else {
            endNodesIds = Collections.emptySet();
        }

        if (!endNodesIds.contains(to.getId())) {
            MutableGraphRelationship onRoute = createRelationship(txn, from, to, ON_ROUTE);
            onRoute.set(route);

            onRoute.setCost(costs.average());
            onRoute.setMaxCost(costs.max());
            onRoute.setTransportMode(route.getTransportMode());
        }
    }

    private void createPlatformStationRelationships(Station station, MutableGraphNode stationNode, Platform platform,
                                                    MutableGraphNode platformNode, MutableGraphTransaction txn) {

        // station -> platform
        Duration enterPlatformCost = station.getMinChangeDuration();

        MutableGraphRelationship crossToPlatform = createRelationship(txn, stationNode, platformNode, ENTER_PLATFORM);
        crossToPlatform.setCost(enterPlatformCost);
        crossToPlatform.set(platform);

        // platform -> station
        Duration leavePlatformCost = Duration.ZERO;

        MutableGraphRelationship crossFromPlatform = createRelationship(txn, platformNode, stationNode, LEAVE_PLATFORM);
        crossFromPlatform.setCost(leavePlatformCost);
        crossFromPlatform.set(station);
    }


    private void createRelationshipTimeNodeToRouteStation(MutableGraphTransaction tx, Route route, Trip trip,
                                                          StopCall beginStop, StopCall endStop,
                                                          RouteStationNodeCache routeStationNodeCache,
                                                          Map<StationTime, MutableGraphNode> timeNodes) {
        Station startStation = beginStop.getStation();
        TramTime departureTime = beginStop.getDepartureTime();

        // time node -> end route station
        MutableGraphNode routeStationEnd = routeStationNodeCache.getRouteStation(tx, route, endStop.getStation().getId());
        MutableGraphNode timeNode = timeNodes.get(StationTime.of(startStation, beginStop.getDepartureTime()));
        TransportRelationshipTypes transportRelationshipType = TransportRelationshipTypes.forMode(route.getTransportMode());
        MutableGraphRelationship goesToRelationship = createRelationship(tx, timeNode, routeStationEnd, transportRelationshipType);
        // properties on relationship
        goesToRelationship.set(trip);

        Duration cost = TramTime.difference(endStop.getArrivalTime(), departureTime);
        goesToRelationship.setCost(cost);
        // TODO Still useful?
        goesToRelationship.set(trip.getService());
        goesToRelationship.set(route);
        goesToRelationship.setStopSeqNum(endStop.getGetSequenceNumber());
    }

    private Map<StationTime, MutableGraphNode> createMinuteNodes(MutableGraphTransaction tx, Trip trip, HourNodeCache hourNodeCache) {

        Map<StationTime, MutableGraphNode> timeNodes = new HashMap<>();

        final StopCalls stops = trip.getStopCalls();
        stops.getLegs(graphFilter.isFiltered()).forEach(leg -> {
            if (includeBothStops(leg)) {
                final Station start = leg.getFirstStation();
                final TramTime departureTime = leg.getDepartureTime();
                MutableGraphNode timeNode = createTimeNodeAndRelationshipFromHour(tx, trip, start.getId(), departureTime, hourNodeCache);
                timeNodes.put(StationTime.of(start, departureTime), timeNode);
            }
        });

        return timeNodes;
    }

    private MutableGraphNode createTimeNodeAndRelationshipFromHour(final MutableGraphTransaction tx, final Trip trip,
                                                                   final IdFor<Station> startId,
                                                                   final TramTime departureTime,
                                                                   final HourNodeCache hourNodeCache) {

        MutableGraphNode timeNode = createGraphNode(tx, GraphLabel.MINUTE);
        timeNode.setTime(departureTime);
        timeNode.set(trip);

        // hour node -> time node
        MutableGraphNode hourNode = hourNodeCache.getHourNode(tx, trip.getRoute().getId(), trip.getService(), startId,
                departureTime.getHourOfDay());
        MutableGraphRelationship fromPrevious = createRelationship(tx, hourNode, timeNode, TransportRelationshipTypes.TO_MINUTE);
        fromPrevious.setCost(Duration.ZERO);
        fromPrevious.setTime(departureTime);
        fromPrevious.set(trip);

        return timeNode;
    }

    private void createHourNodeAndRelationshipFromService(MutableGraphTransaction tx, IdFor<Route> routeId, Service service, IdFor<Station> startId,
                                                          Integer hour, HourNodeCache hourNodeCache, MutableGraphNode serviceNode) {

        if (!hourNodeCache.hasHourNode(routeId, service, startId, hour)) {
            MutableGraphNode hourNode = createGraphNode(tx, GraphLabel.HOUR);
            hourNode.setHourProp(hour);
            hourNode.addLabel(GraphLabel.getHourLabel(hour));
            hourNodeCache.putHour(routeId, service, startId, hour, hourNode);

            // service node -> time node
            MutableGraphRelationship serviceNodeToHour = createRelationship(tx, serviceNode, hourNode, TransportRelationshipTypes.TO_HOUR);
            serviceNodeToHour.setCost(Duration.ZERO);
            serviceNodeToHour.setHour(hour);
        }

    }

    public static class Ready {
        private Ready() {
            // prevent guice creating this, want to create dependency on the Builder
        }
    }

}
