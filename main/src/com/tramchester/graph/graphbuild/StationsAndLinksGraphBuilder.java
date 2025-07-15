package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabaseNeo4J;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.databaseManagement.GraphDatabaseMetaInfo;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.caching.GraphBuilderCache;
import com.tramchester.graph.graphbuild.caching.RouteStationNodeCache;
import com.tramchester.graph.graphbuild.caching.StationAndPlatformNodeCache;
import com.tramchester.metrics.Timing;
import com.tramchester.repository.TransportData;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.GTFSPickupDropoffType.Regular;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

@LazySingleton
public class StationsAndLinksGraphBuilder extends GraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(StationsAndLinksGraphBuilder.class);

    ///
    // Create Station, RouteStations
    //
    // Station-[linked]->Station
    ///

    private final TransportData transportData;
    private final GraphDatabaseMetaInfo databaseMetaInfo;
    private final TramchesterConfig tramchesterConfig;

    // force construction via guide to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    @Inject
    public StationsAndLinksGraphBuilder(GraphDatabaseNeo4J graphDatabase, TramchesterConfig config, GraphFilter graphFilter,
                                        TransportData transportData, GraphBuilderCache builderCache,
                                        GraphDatabaseMetaInfo databaseMetaInfo) {
        super(graphDatabase, graphFilter, config, builderCache);
        this.tramchesterConfig = config;
        this.transportData = transportData;
        this.databaseMetaInfo = databaseMetaInfo;
    }

    @PostConstruct
    public void start() {
        if (!graphDatabase.isAvailable(1000)) {
            final String message = "Graph database is not available (if this is test: check ResourceTramTestConfig and for JourneyPlanningMarker)";
            logger.error(message);
            throw new RuntimeException(message);
        }
        logger.info("start");
        logger.info("Data source name " + transportData.getSourceName());
        if (graphDatabase.isCleanDB()) {
            logger.info("Rebuild of Stations, RouteStations and Links graph DB for " + graphDBConfig.getDbPath());
            if (graphFilter.isFiltered()) {
                logger.warn("Graph is filtered " + graphFilter);
            }
            buildGraphwithFilter(graphDatabase, super.getStationAndPlatformNodeCache(), super.getRouteStationNodeCache());
            logger.info("Graph rebuild is finished for " + graphDBConfig.getDbPath());
        } else {
            logger.info("No rebuild of graph, using existing data");
            graphDatabase.waitForIndexes();
        }
    }

    private void buildGraphwithFilter(final GraphDatabaseNeo4J graphDatabase, final StationAndPlatformNodeCache stationAndPlatformNodeCache,
                                      final RouteStationNodeCache routeStationNodeCache) {
        logger.info("Building graph for feedinfo: " + transportData.summariseDataSourceInfo());
        logMemory("Before graph build");

        graphDatabase.createIndexes();

        try (Timing ignored = new Timing(logger, "graph rebuild")) {
            try(TimedTransaction timedTransaction = graphDatabase.beginTimedTxMutable(logger, "Adding stations")) {
                for(final Station station : transportData.getStations()) {
                    if (graphFilter.shouldInclude(station)) {
                        if (station.getTransportModes().isEmpty()) {
                            logger.info("Skipping " + station.getId() + " as no transport modes are set, non stopping station");
                        } else {
                            final GraphNode stationNode = createStationNode(timedTransaction, station);
                            createPlatformsForStation(timedTransaction, station, stationAndPlatformNodeCache);
                            stationAndPlatformNodeCache.putStation(station.getId(), stationNode);
                        }
                    }
                }
                timedTransaction.commit();
            }

            for(final Agency agency : transportData.getAgencies()) {
                if (graphFilter.shouldIncludeAgency(agency)) {
                    addRouteStationsAndLinksFor(agency, stationAndPlatformNodeCache, routeStationNodeCache);
                }
            }

            addBoundsNode();

        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
            throw new RuntimeException("Unable to build graph " + graphDatabase.getDbPath(), except);
        }

        reportStats();
        System.gc();
        logMemory("After graph build");
    }

    private void addBoundsNode() {

        try(MutableGraphTransaction tx = graphDatabase.beginTxMutable()) {
            logger.info("Adding bounds to the DB");
            databaseMetaInfo.setBounds(tx, tramchesterConfig.getBounds());
            tx.commit();
        }
    }

    private void addRouteStationsAndLinksFor(final Agency agency, final StationAndPlatformNodeCache stationAndPlatformNodeCache,
                                             final RouteStationNodeCache routeStationNodeCache) {

        final Set<Route> routes = agency.getRoutes().stream().filter(graphFilter::shouldIncludeRoute).collect(Collectors.toSet());
        if (routes.isEmpty()) {
            return;
        }

        logger.info(format("Adding %s routes for agency %s", routes.size(), agency));

        final Set<Station> filteredStations = transportData.getActiveStationStream().
                filter(graphFilter::shouldInclude).
                collect(Collectors.toSet());

        // NOTE:
        // The station.servesRouteDropoff(route) || station.servesRoutePickup(route) filter below means route station
        // nodes will not be created for stations that as 'passed' by services that do not call, which is the
        // case for rail transport data.

        try(TimedTransaction txn = graphDatabase.beginTimedTxMutable(logger, "Adding routes")){
            routes.forEach(route -> {
                final IdFor<Route> asId = route.getId();
                logger.debug("Adding route " + asId);
                filteredStations.stream().
                        filter(station -> station.servesRouteDropOff(route) || station.servesRoutePickup(route)).
                        map(station -> transportData.getRouteStation(station, route)).
                        forEach(routeStation -> {
                            final MutableGraphNode routeStationNode = createRouteStationNode(txn, routeStation, routeStationNodeCache);
                            linkStationAndRouteStation(txn, routeStation.getStation(), routeStationNode, route.getTransportMode(),
                                    stationAndPlatformNodeCache);
                        });

                createLinkRelationships(txn, route, stationAndPlatformNodeCache);

                logger.debug("Route " + asId + " added ");
            });
            txn.commit();
        }
    }

    private void linkStationAndRouteStation(final MutableGraphTransaction txn, final Station station, final MutableGraphNode routeStationNode,
                                            final TransportMode transportMode, final StationAndPlatformNodeCache cache) {
        final MutableGraphNode stationNode = cache.getStation(txn, station.getId());

        final MutableGraphRelationship stationToRoute = stationNode.createRelationshipTo(txn, routeStationNode, STATION_TO_ROUTE);
        final MutableGraphRelationship routeToStation = routeStationNode.createRelationshipTo(txn, stationNode, ROUTE_TO_STATION);

        final Duration minimumChangeCost = station.getMinChangeDuration();
        stationToRoute.setCost(minimumChangeCost);
        routeToStation.setCost(Duration.ZERO);

        routeToStation.setTransportMode(transportMode);
        stationToRoute.setTransportMode(transportMode);

//        stationToRoute.setMaxCost(minimumChangeCost);
//        routeToStation.setMaxCost(Duration.ZERO);
    }

    // NOTE: for services that skip some stations, but same stations not skipped by other services
    // this will create multiple links
    private void createLinkRelationships(MutableGraphTransaction tx, final Route route, final StationAndPlatformNodeCache stationAndPlatformNodeCache) {

        // TODO this uses the first cost we encounter for the link, while this is accurate for tfgm trams it does
        //  not give the correct results for buses and trains where time between station can vary depending upon the
        //  service
        final Map<StationIdPair, Duration> pairs = new HashMap<>(); // (start, dest) -> cost
        route.getTrips().forEach(trip -> {
                final StopCalls stops = trip.getStopCalls();
                stops.getLegs(graphFilter.isFiltered()).forEach(leg -> {
                    if (includeBothStops(graphFilter, leg)) {
                        final GTFSPickupDropoffType pickup = leg.getFirst().getPickupType();
                        final GTFSPickupDropoffType dropOff = leg.getSecond().getDropoffType();
                        final StationIdPair legStations = leg.getStations();
                        if (pickup==Regular && dropOff==Regular && !pairs.containsKey(legStations)) {
                            final Duration cost = leg.getCost();
                            pairs.put(legStations, cost);
                        }
                    }
                });
            });

        pairs.keySet().forEach(pair -> {
            final MutableGraphNode startNode = stationAndPlatformNodeCache.getStation(tx, pair.getBeginId());
            final MutableGraphNode endNode = stationAndPlatformNodeCache.getStation(tx, pair.getEndId());
            createLinkRelationship(startNode, endNode, route.getTransportMode(), tx);
        });

    }

    private boolean includeBothStops(final GraphFilter filter, final StopCalls.StopLeg leg) {
        return filter.shouldInclude(leg.getFirst()) && filter.shouldInclude(leg.getSecond());
    }

    private void createLinkRelationship(final MutableGraphNode from, final MutableGraphNode to, final TransportMode mode, final MutableGraphTransaction txn) {
        if (from.hasRelationship(OUTGOING, LINKED)) {

            // update existing relationships if not already present

            final GraphNodeId toNodeId = to.getId();
            final Stream<MutableGraphRelationship> alreadyPresent = from.getRelationshipsMutable(txn, OUTGOING, LINKED);

            // if there is an existing link between stations then update iff the transport mode not already present
            final Optional<MutableGraphRelationship> find = alreadyPresent.
                    filter(relation -> relation.getEndNodeId(txn).equals(toNodeId)).findFirst();

            find.ifPresent(existingRelationship -> {
                final EnumSet<TransportMode> currentModes = existingRelationship.getTransportModes();
                if (!currentModes.contains(mode)) {
                    existingRelationship.addTransportMode(mode);
                }
            });

            if (find.isPresent()) {
                return;
            }
        }

        // else create new

        final MutableGraphRelationship stationsLinked = createRelationship(txn, from, to, LINKED);
        stationsLinked.addTransportMode(mode);
    }

    private void createPlatformsForStation(final MutableGraphTransaction txn, final Station station, final StationAndPlatformNodeCache stationAndPlatformNodeCache) {
        for (final Platform platform : station.getPlatforms()) {

            final MutableGraphNode platformNode = txn.createNode(GraphLabel.PLATFORM);
            platformNode.set(platform);
            platformNode.set(station);
            platformNode.setPlatformNumber(platform);
            setTransportMode(station, platformNode);

            stationAndPlatformNodeCache.putPlatform(platform.getId(), platformNode);
        }
    }

    private MutableGraphNode createRouteStationNode(final MutableGraphTransaction tx, final RouteStation routeStation, final RouteStationNodeCache routeStationNodeCache) {

        final boolean hasAlready = tx.hasAnyMatching(GraphLabel.ROUTE_STATION, GraphPropertyKey.ROUTE_STATION_ID.getText(), routeStation.getId().getGraphId());

        if (hasAlready) {
            final String msg = "Existing route station node for " + routeStation + " with id " + routeStation.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        /*** NOTE: when we apply INTERCHANGE LABEL we update the modes
         * @see StagedTransportGraphBuilder#createBoardingAndDepart
         */
        final TransportMode mode = routeStation.getRoute().getTransportMode();
        final GraphLabel modeLabel = GraphLabel.forMode(mode);

        final EnumSet<GraphLabel> labels = EnumSet.of(GraphLabel.ROUTE_STATION, modeLabel);

        final MutableGraphNode routeStationNode = createGraphNode(tx, labels);

        logger.debug(format("Creating route station %s nodeId %s", routeStation.getId(), routeStationNode.getId()));
        routeStationNode.set(routeStation);
        routeStationNode.set(routeStation.getStation());
        routeStationNode.set(routeStation.getRoute());

        routeStationNode.setTransportMode(mode);

        routeStationNodeCache.putRouteStation(routeStation.getId(), routeStationNode);
        return routeStationNode;
    }

    private void setTransportMode(final Station station, final MutableGraphNode node) {
        Set<TransportMode> modes = station.getTransportModes();
        if (modes.isEmpty()) {
            logger.error("No transport modes set for " + station.getId());
            return;
        }
        if (modes.size()==1) {
            TransportMode first = modes.iterator().next();
            node.setTransportMode(first);
//            setProperty(node, first);
        } else {
            logger.error(format("Unable to set transportmode property, more than one mode (%s) for %s",
                    modes, station.getId()));
        }
    }

    public static class Ready {
        private Ready() {
            // prevent guice creating this, want to create dependency on the Builder
        }
    }

}
