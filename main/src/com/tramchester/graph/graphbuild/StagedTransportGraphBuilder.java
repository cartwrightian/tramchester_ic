package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
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
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.graphbuild.GraphLabel.INTERCHANGE;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.INCOMING;
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

    private final TransportData transportData;
    private final InterchangeRepository interchangeRepository;
    private final GraphDatabaseMetaInfo databaseMetaInfo;
    private final StopCallRepository stopCallRepository;
    private final StationsWithDiversionRepository stationsWithDiversionRepository;
    private final TramchesterConfig tramchesterConfig;

    // force construction via guice to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    @Inject
    public StagedTransportGraphBuilder(GraphDatabase graphDatabase, TramchesterConfig config, GraphFilter graphFilter,
                                       TransportData transportData, InterchangeRepository interchangeRepository,
                                       GraphBuilderCache builderCache,
                                       @SuppressWarnings("unused") StationsAndLinksGraphBuilder.Ready stationAndLinksBuilt,
                                       @SuppressWarnings("unused") AddNeighboursGraphBuilder.Ready neighboursReady,
                                       @SuppressWarnings("unused") AddDiversionsForClosedGraphBuilder.Ready walksForClosedReady,
                                       @SuppressWarnings("unused") AddTemporaryStationWalksGraphBuilder tempWalksReady,
                                       @SuppressWarnings("unused") StationGroupsGraphBuilder.Ready groupsReady,
                                       GraphDatabaseMetaInfo databaseMetaInfo, StopCallRepository stopCallRepository,
                                       StationsWithDiversionRepository stationsWithDiversionRepository) {
        super(graphDatabase, graphFilter, config, builderCache);
        this.tramchesterConfig = config;
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

    private void buildGraphWithFilter(final GraphDatabase graphDatabase) {
        logger.info("Building graph for data source: " + transportData.summariseDataSourceInfo());
        logMemory("Before graph build");

        try(Timing ignored = new Timing(logger, "Graph rebuild")) {

            final StationAndPlatformNodeCache stationAndPlatformNodeCache = getStationAndPlatformNodeCache();
            final RouteStationNodeCache routeStationNodeCache = getRouteStationNodeCache();

            final BoardingDepartNodeCache boardingDepartNodeCache = new BoardingDepartNodeCache();

            // just for tfgm trams currently
            linkStationsAndPlatforms(stationAndPlatformNodeCache);

            transportData.getAgencies().stream().
                    parallel().
                    filter(graphFilter::shouldIncludeAgency).
                    forEach(agency ->  {
                        try (Timing ignored1 = new Timing(logger,"Add agency " + agency.getId() + " " + agency.getName())) {
                            buildForAgency(graphDatabase, agency, stationAndPlatformNodeCache, routeStationNodeCache, boardingDepartNodeCache);
                        }
                    });

            // only add version node if we manage to build graph, so partial builds that fail cause a rebuild
            addVersionNodes(graphDatabase, transportData);

        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
            throw new RuntimeException("Unable to build graph", except);
        }

        super.fullClearCache();
        reportStats();
        System.gc(); // for testing, was causing issue on the main test run
        logMemory("After graph build");
    }



    private void linkStationsAndPlatforms(final StationAndPlatformNodeCache stationAndPlatformNodeCache) {

        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "link stations & platforms")) {
            final MutableGraphTransaction txn = timedTransaction.transaction();
            transportData.getActiveStationStream().
                    filter(Station::hasPlatforms).
                    filter(graphFilter::shouldInclude).
                    forEach(station -> linkStationAndPlatforms(txn, station, stationAndPlatformNodeCache));
            timedTransaction.commit();
        }
    }

    private void addVersionNodes(final GraphDatabase graphDatabase, final DataSourceRepository sourceRepository) {
        if (!sourceRepository.hasDataSourceInfo()) {
            logger.error("No data source info was provided, version will not be set in the DB");
            return;
        }

        try(MutableGraphTransaction tx = graphDatabase.beginTxMutable()) {
            logger.info("Adding version node to the DB");
            databaseMetaInfo.createVersionNode(tx, sourceRepository);
            tx.commit();
        }
    }

    private void buildForAgency(final GraphDatabase graphDatabase, final Agency agency, final StationAndPlatformNodeCache stationAndPlatformCache,
                                final RouteStationNodeCache routeStationNodeCache, final BoardingDepartNodeCache boardingDepartNodeCache) {

        // serviceNodeCache and hourNodeCache
        final AgencyBuilderNodeCache agencyBuilderNodeCache = new AgencyBuilderNodeCache();

        if (getRoutesForAgency(agency).findAny().isEmpty()) {
            return;
        }

        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "onRoute for " + agency.getId())) {
            final MutableGraphTransaction tx = timedTransaction.transaction();
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
            // moved the parallel up one level, undefined behaviours with shared txn
            final BatchTransactionStrategy transactionStrategy = new BatchTransactionStrategy(graphDatabase,100, agency.getId());
            getRoutesForAgency(agency).forEach(route -> {
                transactionStrategy.routeBegin(route);
                createMinuteNodesAndRecordUpdatesForTrips(transactionStrategy, route, agencyBuilderNodeCache, routeStationNodeCache, agencyBuilderNodeCache);
                transactionStrategy.routeDone();
            });
            transactionStrategy.close();
        }

        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "boards & departs for " + agency.getId())) {
            final MutableGraphTransaction tx = timedTransaction.transaction();
            getRoutesForAgency(agency).forEach(route -> buildGraphForBoardsAndDeparts(route, tx, stationAndPlatformCache, routeStationNodeCache,
                    boardingDepartNodeCache));
            timedTransaction.commit();
        }

        agencyBuilderNodeCache.clear();
    }

    @NotNull
    private Stream<Route> getRoutesForAgency(final Agency agency) {
        return agency.getRoutes().stream().filter(graphFilter::shouldIncludeRoute);
    }

    private void createMinuteNodesAndRecordUpdatesForTrips(final TransactionStrategy strategy, final Route route,
                                                           final HourNodeCache hourNodeCache,
                                                           final RouteStationNodeCache routeStationNodeCache,
                                                           ServiceNodeCache serviceNodeCache) {
        // time nodes and relationships for trips
        for (final Trip trip : route.getTrips()) {
            strategy.tripBegin(trip);
            final MutableGraphTransaction tx = strategy.currentTxn();
            final Map<StationTime, MutableGraphNode> timeNodes = createMinuteNodes(tx, trip, hourNodeCache, serviceNodeCache);
            createTripRelationships(tx, route, trip, routeStationNodeCache, timeNodes);
            timeNodes.clear();
            strategy.tripDone();
        }
    }

    private void createTripRelationships(final MutableGraphTransaction tx, final Route route, final Trip trip,
                                         final RouteStationNodeCache routeBuilderCache,
                                         final Map<StationTime, MutableGraphNode> timeNodes) {
        final StopCalls stops = trip.getStopCalls();

        stops.getLegs(graphFilter.isFiltered()).forEach(leg -> {
            if (includeBothStops(leg)) {
                StopCall first = leg.getFirst();
                StopCall second = leg.getSecond();
                createRelationshipTimeNodeToRouteStation(tx, route, trip, first, second, routeBuilderCache, timeNodes);
            }
        });
    }

    private void buildGraphForBoardsAndDeparts(final Route route, final MutableGraphTransaction tx, final StationAndPlatformNodeCache stationAndPlatformCache,
                                               final RouteStationNodeCache routeStationNodeCache, final BoardingDepartNodeCache boardingDepartNodeCache) {
        for (final Trip trip : route.getTrips()) {
            trip.getStopCalls().stream().
                    filter(graphFilter::shouldInclude).
                    forEach(stopCall -> createBoardingAndDepart(tx, stopCall, route, trip, stationAndPlatformCache,
                            routeStationNodeCache, boardingDepartNodeCache));
        }
    }

    private void createServiceAndHourNodesForRoute(final MutableGraphTransaction tx, final Route route,
                                                   final RouteStationNodeCache routeStationNodeCache, final ServiceNodeCache serviceNodeCache,
                                                   final HourNodeCache hourNodeCache) {
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

                        final MutableGraphNode serviceNode = createServiceNodeAndRelationshipFromRouteStation(tx, route, trip,
                                beginId, endId, routeStationNodeCache, serviceNodeCache);

                        createHourNodeAndRelationshipFromService(tx, leg.getDepartureTime().getHourOfDay(), hourNodeCache, serviceNode);
                    }
                });
        });
    }

    private MutableGraphNode createServiceNodeAndRelationshipFromRouteStation(final MutableGraphTransaction tx, final Route route, final Trip trip,
                                                                              final IdFor<Station> beginId, final IdFor<Station> nextStationId,
                                                                              final RouteStationNodeCache routeStationNodeCache,
                                                                              final ServiceNodeCache serviceNodeCache) {

        final Service service = trip.getService();

        // Node for the service
        // -route ID here as some towardsServices can go via multiple routes, this seems to be associated with the depots
        // -some towardsServices can go in two different directions from a station i.e. around Media City UK

        final MutableGraphNode svcNode;
        final boolean existing;
        if (serviceNodeCache.hasServiceNode(route.getId(), service, beginId, nextStationId)) {
            svcNode = serviceNodeCache.getServiceNode(tx, route.getId(), service, beginId, nextStationId);
            existing = true;
        } else {
            svcNode =  tx.createNode(GraphLabel.SERVICE);
            svcNode.set(service);
            svcNode.set(route);

            svcNode.setTowards(nextStationId);

            serviceNodeCache.putService(route.getId(), service, beginId, nextStationId, svcNode);
            existing = false;
        }

        // start route station -> svc node
        final MutableGraphNode routeStationNode = routeStationNodeCache.getRouteStation(tx, route, beginId);
        final MutableGraphRelationship svcRelationship;
        if (!existing) {
            svcRelationship = createRelationship(tx, routeStationNode, svcNode, TO_SERVICE);
            svcRelationship.set(service);
            svcRelationship.setCost(Duration.ZERO);
            svcRelationship.set(route);
        } else {
            // note: switch of direction here, can't use OUTGOING from routeStationStart since has multiple links to services
            svcRelationship = svcNode.getSingleRelationshipMutable(tx, TO_SERVICE, INCOMING);
        }

        svcRelationship.addTripId(trip.getId());

        return svcNode;

    }

    private void linkStationAndPlatforms(final MutableGraphTransaction txn, final Station station, final StationAndPlatformNodeCache stationAndPlatformNodeCache) {

        final MutableGraphNode stationNode = stationAndPlatformNodeCache.getStation(txn, station.getId());
        if (stationNode!=null) {
            for (final Platform platform : station.getPlatforms()) {
                final MutableGraphNode platformNode = stationAndPlatformNodeCache.getPlatform(txn, platform.getId());
                createPlatformStationRelationships(station, stationNode, platform, platformNode, txn);
            }
        } else {
            throw new RuntimeException("Missing station node for " + station);
        }
    }

    private void createOnRouteRelationships(final MutableGraphTransaction tx, final Route route, final RouteStationNodeCache routeBuilderCache) {

        final boolean graphIsFiltered = graphFilter.isFiltered();
        final Stream<StopCalls.StopLeg> legsToInclude = route.getTrips().stream().
                map(Trip::getStopCalls).
                flatMap(stopCalls -> stopCalls.getLegs(graphIsFiltered).stream()).
                filter(this::includeBothStops);

        legsToInclude.forEach((leg) -> {
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

    private boolean includeBothStops(final StopCalls.StopLeg leg) {
        return graphFilter.shouldInclude(leg.getFirst()) && graphFilter.shouldInclude(leg.getSecond());
    }

    private void createBoardingAndDepart(final MutableGraphTransaction tx, final StopCall stopCall,
                                         final Route route, final Trip trip,
                                         final StationAndPlatformNodeCache stationAndPlatformNodeCache,
                                         final RouteStationNodeCache routeStationNodeCache,
                                         final BoardingDepartNodeCache boardingDepartNodeCache) {

        if (!stopCall.callsAtStation()) {
            if (route.getTransportMode()==Tram) {
                logger.warn("No pickup or drop-off for " + stopCall);
            }
            return;
        }

        final boolean pickup = stopCall.getPickupType().equals(GTFSPickupDropoffType.Regular);
        final boolean dropoff = stopCall.getDropoffType().equals(GTFSPickupDropoffType.Regular);

        final Station station = stopCall.getStation();

        // TODO when filtering this isn't really valid, we might only see a small segment of a larger trip....
        // In unfiltered situations (i.e. not testing) it is fine

        final boolean isFirstStop = stopCall.equals(trip.getStopCalls().getFirstStop());
        if (isFirstStop && dropoff && !trip.isFiltered()) {
            String msg = "Drop off at first station for stop " + station.getId() + " trip " + trip.getId() + " " + stopCall.getDropoffType()
                    + " seq:" + stopCall.getGetSequenceNumber();
            logger.debug(msg);
        }

        final boolean isLastStop = stopCall.equals(trip.getStopCalls().getLastStop());
        if (isLastStop && pickup && !trip.isFiltered()) {
            String msg = "Pick up at last station for stop " + station.getId() + " trip " + trip.getId() + " " + stopCall.getPickupType()
                    + " seq:" + stopCall.getGetSequenceNumber();
            logger.debug(msg);
        }

        final boolean isInterchange = interchangeRepository.isInterchange(station);


        // If Bus, for example, we board to/from station, for trams it is from the platform
        final MutableGraphNode platformOrStation = station.hasPlatforms() ? stationAndPlatformNodeCache.getPlatform(tx, stopCall.getPlatform().getId())
                : stationAndPlatformNodeCache.getStation(tx, station.getId());
        final IdFor<RouteStation> routeStationId = RouteStation.createId(station.getId(), route.getId());
        final MutableGraphNode routeStationNode = routeStationNodeCache.getRouteStation(tx, routeStationId);

        if (isInterchange) {
            routeStationNode.addLabel(INTERCHANGE);
            final EnumSet<TransportMode> interchangeModes = interchangeRepository.getInterchangeModes(station);
            GraphLabel.forModes(interchangeModes).forEach(routeStationNode::addLabel);
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

    private void createDeparts(final BoardingDepartNodeCache boardingDepartNodeCache, final Station station, final boolean isInterchange,
                               final MutableGraphNode boardingNode, final IdFor<RouteStation> routeStationId,
                               final MutableGraphNode routeStationNode, final MutableGraphTransaction txn) {

        final TransportRelationshipTypes departType;
        if (isInterchange) {
            departType = INTERCHANGE_DEPART;
        } else if (stationsWithDiversionRepository.hasDiversions(station)) {
            departType = DIVERSION_DEPART;
        } else {
            departType = DEPART;
        }

        final MutableGraphRelationship departRelationship = createRelationship(txn, routeStationNode, boardingNode, departType);
        departRelationship.setCost(Duration.ZERO);
        departRelationship.setRouteStationId(routeStationId);
        departRelationship.set(station);
        boardingDepartNodeCache.putDepart(boardingNode.getId(), routeStationNode.getId());

        if (departType.equals(DIVERSION_DEPART)) {
            final Set<DateTimeRange> ranges = stationsWithDiversionRepository.getDateTimeRangesFor(station);
            ranges.forEach(departRelationship::setDateTimeRange);
        }
    }

    private void createBoarding(final BoardingDepartNodeCache boardingDepartNodeCache, final StopCall stop, final Route route, final Station station,
                                final boolean isInterchange, final MutableGraphNode platformOrStation, final IdFor<RouteStation> routeStationId,
                                final MutableGraphNode routeStationNode, final MutableGraphTransaction txn) {
        final TransportRelationshipTypes boardType = isInterchange ? INTERCHANGE_BOARD : BOARD;
        final MutableGraphRelationship boardRelationship = createRelationship(txn, platformOrStation, routeStationNode, boardType);

        boardRelationship.setCost(Duration.ZERO);
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
//            onRoute.setMaxCost(costs.max());
            onRoute.setTransportMode(route.getTransportMode());
        }
    }

    private void createPlatformStationRelationships(final Station station, final MutableGraphNode stationNode, final Platform platform,
                                                    final MutableGraphNode platformNode, final MutableGraphTransaction txn) {

        // station -> platform
        final Duration enterPlatformCost = station.getMinChangeDuration();

        final MutableGraphRelationship crossToPlatform = createRelationship(txn, stationNode, platformNode, ENTER_PLATFORM);
        crossToPlatform.setCost(enterPlatformCost);
        crossToPlatform.set(platform);

        // platform -> station
        final MutableGraphRelationship crossFromPlatform = createRelationship(txn, platformNode, stationNode, LEAVE_PLATFORM);
        crossFromPlatform.setCost(Duration.ZERO);
        crossFromPlatform.set(station);
    }


    private void createRelationshipTimeNodeToRouteStation(final MutableGraphTransaction tx, final Route route, final Trip trip,
                                                          final StopCall beginStop, final StopCall endStop,
                                                          final RouteStationNodeCache routeStationNodeCache,
                                                          final Map<StationTime, MutableGraphNode> timeNodes) {
        final Station startStation = beginStop.getStation();
        final TramTime departureTime = beginStop.getDepartureTime();

        // time node -> end route station
        final MutableGraphNode routeStationEnd = routeStationNodeCache.getRouteStation(tx, route, endStop.getStation().getId());
        final MutableGraphNode timeNode = timeNodes.get(StationTime.of(startStation, beginStop.getDepartureTime()));
        final TransportRelationshipTypes transportRelationshipType = TransportRelationshipTypes.forMode(route.getTransportMode());
        final MutableGraphRelationship goesToRelationship = createRelationship(tx, timeNode, routeStationEnd, transportRelationshipType);
        // properties on relationship
        goesToRelationship.set(trip);

        final Duration cost = TramTime.difference(endStop.getArrivalTime(), departureTime);
        goesToRelationship.setCost(cost);
        // TODO Still useful?
        goesToRelationship.set(trip.getService());
        goesToRelationship.set(route);
        goesToRelationship.setStopSeqNum(endStop.getGetSequenceNumber());
    }

    private Map<StationTime, MutableGraphNode> createMinuteNodes(final MutableGraphTransaction tx, final Trip trip,
                                                                 final HourNodeCache hourNodeCache, ServiceNodeCache serviceNodeCache) {

        final Map<StationTime, MutableGraphNode> timeNodes = new HashMap<>();

        final StopCalls stopCalls = trip.getStopCalls();
        stopCalls.getLegs(graphFilter.isFiltered()).forEach(leg -> {
            if (includeBothStops(leg)) {
                final Station start = leg.getFirstStation();
                final TramTime departureTime = leg.getDepartureTime();
                final MutableGraphNode timeNode = createTimeNodeAndRelationshipFromHour(tx, trip, leg, departureTime,
                        hourNodeCache, serviceNodeCache);
                timeNodes.put(StationTime.of(start, departureTime), timeNode);
            }
        });

        return timeNodes;
    }

    private MutableGraphNode createTimeNodeAndRelationshipFromHour(final MutableGraphTransaction tx, final Trip trip,
                                                                   final StopCalls.StopLeg leg,
                                                                   final TramTime departureTime,
                                                                   final HourNodeCache hourNodeCache,
                                                                   final ServiceNodeCache serviceNodeCache) {

        final MutableGraphNode timeNode = createGraphNode(tx, GraphLabel.MINUTE);
        timeNode.setTime(departureTime);
        timeNode.set(trip);

        IdFor<Station> startId = leg.getFirstStation().getId();
        IdFor<Station> endId = leg.getSecondStation().getId();

        // hour node -> time node
        MutableGraphNode svcNode = serviceNodeCache.getServiceNode(tx, trip.getRoute().getId(), trip.getService(), startId, endId);
        final MutableGraphNode hourNode = hourNodeCache.getHourNode(tx, svcNode.getId(),
                departureTime.getHourOfDay());
        final MutableGraphRelationship fromPrevious = createRelationship(tx, hourNode, timeNode, TransportRelationshipTypes.TO_MINUTE);
        fromPrevious.setCost(Duration.ZERO);
        fromPrevious.setTime(departureTime);
        fromPrevious.set(trip);

        return timeNode;
    }

    private void createHourNodeAndRelationshipFromService(final MutableGraphTransaction tx, final int hour,
                                                          final HourNodeCache hourNodeCache, final MutableGraphNode serviceNode) {


        // TODO THIS IS now ambiguous
        if (!hourNodeCache.hasHourNode(serviceNode.getId(), hour)) {
            final MutableGraphNode hourNode = createGraphNode(tx, GraphLabel.HOUR);
            hourNode.setHourProp(hour);
            hourNode.addLabel(GraphLabel.getHourLabel(hour));
            hourNodeCache.putHour(serviceNode.getId(), hour, hourNode);

            // service node -> time node
            final MutableGraphRelationship serviceNodeToHour = createRelationship(tx, serviceNode, hourNode, TransportRelationshipTypes.TO_HOUR);
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
