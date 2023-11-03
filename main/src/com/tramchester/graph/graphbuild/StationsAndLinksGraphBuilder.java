package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.HasGraphDBConfig;
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
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.TimedTransaction;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.metrics.Timing;
import com.tramchester.repository.TransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
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

    // force construction via guide to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    @Inject
    public StationsAndLinksGraphBuilder(GraphDatabase graphDatabase, HasGraphDBConfig config, GraphFilter graphFilter,
                                        TransportData transportData, GraphBuilderCache builderCache) {
        super(graphDatabase, graphFilter, config, builderCache);
        this.transportData = transportData;
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
            buildGraphwithFilter(graphDatabase, builderCache);
            logger.info("Graph rebuild is finished for " + graphDBConfig.getDbPath());
        } else {
            logger.info("No rebuild of graph, using existing data");
            graphDatabase.waitForIndexes();
        }
    }

    private void buildGraphwithFilter(GraphDatabase graphDatabase, GraphBuilderCache builderCache) {
        logger.info("Building graph for feedinfo: " + transportData.summariseDataSourceInfo());
        logMemory("Before graph build");

        graphDatabase.createIndexs();

        try (Timing ignored = new Timing(logger, "graph rebuild")) {
            try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "Adding stations")) {
                GraphTransaction tx = timedTransaction.transaction();
                for(Station station : transportData.getStations()) {
                    if (graphFilter.shouldInclude(station)) {
                        if (station.getTransportModes().isEmpty()) {
                            logger.info("Skipping " + station.getId() + " as no transport modes are set, non stopping station");
                        } else {
                            GraphNode stationNode = createStationNode(tx, station);
                            createPlatformsForStation(tx, station, builderCache);
                            builderCache.putStation(station.getId(), stationNode);
                        }
                    }
                }
                timedTransaction.commit();
            }

            for(Agency agency : transportData.getAgencies()) {
                if (graphFilter.shouldIncludeAgency(agency)) {
                    addRouteStationsAndLinksFor(agency, builderCache);
                }
            }
        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
            throw new RuntimeException("Unable to build graph " + graphDatabase.getDbPath(), except);
        }

        reportStats();
        System.gc();
        logMemory("After graph build");
    }

    private void addRouteStationsAndLinksFor(Agency agency, GraphBuilderCache builderCache) {

        Set<Route> routes = agency.getRoutes().stream().filter(graphFilter::shouldIncludeRoute).collect(Collectors.toSet());
        if (routes.isEmpty()) {
            return;
        }

        logger.info(format("Adding %s routes for agency %s", routes.size(), agency));

        Set<Station> filteredStations = transportData.getActiveStationStream().
                filter(graphFilter::shouldInclude).
                collect(Collectors.toSet());

        // NOTE:
        // The station.servesRouteDropoff(route) || station.servesRoutePickup(route) filter below means route station
        // nodes will not be created for stations that as 'passed' by services that do not call, which is the
        // case for rail transport data.

        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "Adding routes")){
            GraphTransaction tx = timedTransaction.transaction();
            routes.forEach(route -> {
                IdFor<Route> asId = route.getId();
                logger.debug("Adding route " + asId);
                filteredStations.stream().
                        filter(station -> station.servesRouteDropOff(route) || station.servesRoutePickup(route)).
                        map(station -> transportData.getRouteStation(station, route)).
                        forEach(routeStation -> {
                            MutableGraphNode routeStationNode = createRouteStationNode(tx, routeStation, builderCache);
                            linkStationAndRouteStation(tx, routeStation.getStation(), routeStationNode, route.getTransportMode());
                        });

                createLinkRelationships(tx, route, builderCache);

                logger.debug("Route " + asId + " added ");
            });
            timedTransaction.commit();
        }
    }

    private void linkStationAndRouteStation(GraphTransaction txn, Station station, MutableGraphNode routeStationNode, TransportMode transportMode) {
        MutableGraphNode stationNode = builderCache.getStation(txn, station.getId());

        final MutableGraphRelationship stationToRoute = stationNode.createRelationshipTo(txn, routeStationNode, STATION_TO_ROUTE);
        final MutableGraphRelationship routeToStation = routeStationNode.createRelationshipTo(txn, stationNode, ROUTE_TO_STATION);

        final Duration minimumChangeCost = station.getMinChangeDuration();
        stationToRoute.setCost(minimumChangeCost);
        routeToStation.setCost(Duration.ZERO);

        routeToStation.setTransportMode(transportMode);
        stationToRoute.setTransportMode(transportMode);

        stationToRoute.setMaxCost(minimumChangeCost);
        routeToStation.setMaxCost(Duration.ZERO);
    }

    // NOTE: for services that skip some stations, but same stations not skipped by other services
    // this will create multiple links
    private void createLinkRelationships(GraphTransaction tx, Route route, GraphBuilderCache routeBuilderCache) {

        // TODO this uses the first cost we encounter for the link, while this is accurate for tfgm trams it does
        //  not give the correct results for buses and trains where time between station can vary depending upon the
        //  service
        Map<StationIdPair, Duration> pairs = new HashMap<>(); // (start, dest) -> cost
        route.getTrips().forEach(trip -> {
                StopCalls stops = trip.getStopCalls();
                stops.getLegs(graphFilter.isFiltered()).forEach(leg -> {
                    if (includeBothStops(graphFilter, leg)) {
                        GTFSPickupDropoffType pickup = leg.getFirst().getPickupType();
                        GTFSPickupDropoffType dropOff = leg.getSecond().getDropoffType();
                        StationIdPair legStations = leg.getStations();
                        if (pickup==Regular && dropOff==Regular &&
                                !pairs.containsKey(legStations)) {
                            Duration cost = leg.getCost();
                            pairs.put(legStations, cost);
                        }
                    }
                });
            });

        pairs.keySet().forEach(pair -> {
            MutableGraphNode startNode = routeBuilderCache.getStation(tx, pair.getBeginId());
            MutableGraphNode endNode = routeBuilderCache.getStation(tx, pair.getEndId());
            createLinkRelationship(startNode, endNode, route.getTransportMode(), tx);
        });

    }

    private boolean includeBothStops(GraphFilter filter, StopCalls.StopLeg leg) {
        return filter.shouldInclude(leg.getFirst()) && filter.shouldInclude(leg.getSecond());
    }

    private void createLinkRelationship(MutableGraphNode from, MutableGraphNode to, TransportMode mode, GraphTransaction txn) {
        if (from.hasRelationship(OUTGOING, LINKED)) {
            Stream<MutableGraphRelationship> alreadyPresent = from.getRelationshipsMutable(txn, OUTGOING, LINKED);

            // if there is an existing link between stations then update iff the transport mode not already present
            Optional<MutableGraphRelationship> find = alreadyPresent.filter(relation -> relation.getEndNode(txn).equals(to)).findFirst();

            find.ifPresent(found -> {
                EnumSet<TransportMode> currentModes = found.getTransportModes();
                if (!currentModes.contains(mode)) {
                    found.addTransportMode(mode);
                }
            });

            if (find.isPresent()) {
                return;
            }
        }

        MutableGraphRelationship stationsLinked = createRelationship(txn, from, to, LINKED);
        stationsLinked.addTransportMode(mode);
    }

    private void createPlatformsForStation(GraphTransaction txn, Station station, GraphBuilderCache routeBuilderCache) {
        for (Platform platform : station.getPlatforms()) {

            MutableGraphNode platformNode = txn.createNode(GraphLabel.PLATFORM);
            platformNode.set(platform);
            platformNode.set(station);
            platformNode.setPlatformNumber(platform);
            setTransportMode(station, platformNode);

            routeBuilderCache.putPlatform(platform.getId(), platformNode);
        }
    }

    private MutableGraphNode createRouteStationNode(GraphTransaction tx, RouteStation routeStation, GraphBuilderCache builderCache) {
//        Node existing = graphDatabase.findNode(tx,
//                GraphLabel.ROUTE_STATION, GraphPropertyKey.ROUTE_STATION_ID.getText(), routeStation.getId().getGraphId());
//
//        if (existing!=null) {
//            final String msg = "Existing route station node for " + routeStation + " with id " + routeStation.getId();
//            logger.error(msg);
//            throw new RuntimeException(msg);
//        }

        boolean hasAlready = tx.hasAnyMatching(GraphLabel.ROUTE_STATION, GraphPropertyKey.ROUTE_STATION_ID.getText(), routeStation.getId().getGraphId());

        if (hasAlready) {
            final String msg = "Existing route station node for " + routeStation + " with id " + routeStation.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        TransportMode mode = routeStation.getRoute().getTransportMode();
        GraphLabel modeLabel = GraphLabel.forMode(mode);

        Set<GraphLabel> labels = new HashSet<>(Arrays.asList(GraphLabel.ROUTE_STATION, modeLabel));

        MutableGraphNode routeStationNode = createGraphNode(tx, labels);

        logger.debug(format("Creating route station %s nodeId %s", routeStation.getId(), routeStationNode.getId()));
        //GraphProps.setProperty(routeStationNode, routeStation);
        routeStationNode.set(routeStation);
        routeStationNode.set(routeStation.getStation());
        routeStationNode.set(routeStation.getRoute());

        routeStationNode.setTransportMode(mode);
//        setProperty(graphNode.getNode(), mode);

        builderCache.putRouteStation(routeStation.getId(), routeStationNode);
        return routeStationNode;
    }

    private void setTransportMode(Station station, MutableGraphNode node) {
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

//    private void setTransportMode(Station station, Node node) {
//        Set<TransportMode> modes = station.getTransportModes();
//        if (modes.isEmpty()) {
//            logger.error("No transport modes set for " + station.getId());
//            return;
//        }
//        if (modes.size()==1) {
//            TransportMode first = modes.iterator().next();
//            setProperty(node, first);
//        } else {
//            logger.error(format("Unable to set transportmode property, more than one mode (%s) for %s",
//                    modes, station.getId()));
//        }
//    }


    public static class Ready {
        private Ready() {
            // prevent guice creating this, want to create dependency on the Builder
        }
    }

}
