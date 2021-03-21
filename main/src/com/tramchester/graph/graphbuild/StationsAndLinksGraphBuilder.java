package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.metrics.Timing;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.GTFSPickupDropoffType.Regular;
import static com.tramchester.graph.TransportRelationshipTypes.LINKED;
import static com.tramchester.graph.graphbuild.GraphProps.*;
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

    // force contsruction via guide to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    @Inject
    public StationsAndLinksGraphBuilder(GraphDatabase graphDatabase, TramchesterConfig config, GraphFilter graphFilter,
                                        NodeTypeRepository nodeIdLabelMap, TransportData transportData,
                                        GraphBuilderCache builderCache) {
        super(graphDatabase, graphFilter, config, builderCache, nodeIdLabelMap);
        this.transportData = transportData;
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        logger.info("Data source name " + transportData.getSourceName());
        if (graphDatabase.isCleanDB()) {
            logger.info("Rebuild of Stations, RouteStations and Links graph DB for " + config.getDbPath());
            if (graphFilter.isFiltered()) {
                logger.warn("Graph is filtered " + graphFilter);
            }
            buildGraphwithFilter(graphFilter, graphDatabase, builderCache);
            logger.info("Graph rebuild is finished for " + config.getDbPath());
        } else {
            logger.info("No rebuild of graph, using existing data");
        }
    }

    @Override
    protected void buildGraphwithFilter(GraphFilter graphFilter, GraphDatabase graphDatabase, GraphBuilderCache builderCache) {
        logger.info("Building graph for feedinfo: " + transportData.getDataSourceInfo());
        logMemory("Before graph build");

        try (Timing timing = new Timing(logger, "graph rebuild")) {
            for(Agency agency : transportData.getAgencies()) {
                if (graphFilter.shouldInclude(agency)) {
                    try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "Adding agency " + agency.getId())) {
                        Transaction tx = timedTransaction.transaction();
                        Stream<Route> routes = agency.getRoutes().stream().filter(graphFilter::shouldInclude);
                        buildGraphForRoutes(tx, graphFilter, routes, builderCache);
                        timedTransaction.commit();
                    }
                } else {
                    logger.warn("Filter out: " + agency);
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

    private void buildGraphForRoutes(Transaction tx, final GraphFilter filter, Stream<Route> routes,
                                     GraphBuilderCache builderCache) {
        Set<Station> filteredStations = filter.isFiltered() ?
                transportData.getStations().stream().filter(filter::shouldInclude).collect(Collectors.toSet()) :
                transportData.getStations();

        routes.forEach(route -> {
            IdFor<Route> asId = route.getId();
            logger.info("Adding route " + asId);
            // create or cache stations and platforms for route, create route stations
            filteredStations.stream().filter(station -> station.servesRoute(route)).
                    forEach(station -> createStationAndRouteStation(tx, route, station, builderCache));

            createLinkRelationships(tx, filter, route, builderCache);

            logger.debug("Route " + asId + " added ");
        });
    }

    private Node getStationNode(Transaction txn, Station station) {
        Set<Labels> labels = Labels.forMode(station.getTransportModes());
        // ought to be able find with any of the labels, os use the first one
        Labels label = labels.iterator().next();

        return graphDatabase.findNode(txn, label, station.getProp().getText(), station.getId().getGraphId());
    }

    private void createStationAndRouteStation(Transaction txn, Route route, Station station, GraphBuilderCache builderCache) {

        RouteStation routeStation = transportData.getRouteStation(station, route);
        createRouteStationNode(txn, routeStation, builderCache);

        Node stationNode = getStationNode(txn, station);
        if (stationNode == null) {
            stationNode = createStationNode(txn, station);
            builderCache.putStation(station.getId(), stationNode);
        }
    }

    // NOTE: for services that skip some stations, but same stations not skipped by other services
    // this will create multiple links
    private void createLinkRelationships(Transaction tx, GraphFilter filter, Route route, GraphBuilderCache routeBuilderCache) {

        // TODO this uses the first cost we encounter for the link, while this is accurate for tfgm trams it does
        //  not give the correct results for buses and trains where time between station can vary depending upon the
        //  service
        Map<StationIdPair, Integer> pairs = new HashMap<>(); // (start, dest) -> cost
        route.getTrips().forEach(trip -> {
                StopCalls stops = trip.getStopCalls();
                stops.getLegs().forEach(leg -> {
                    if (includeBothStops(filter, leg)) {
                        GTFSPickupDropoffType pickup = leg.getFirst().getPickupType();
                        GTFSPickupDropoffType dropOff = leg.getSecond().getDropoffType();
                        StationIdPair legStations = StationIdPair.of(leg.getFirstStation(), leg.getSecondStation());
                        if (pickup==Regular && dropOff==Regular &&
                                !pairs.containsKey(legStations)) {
                            int cost = leg.getCost();
                            pairs.put(legStations, cost);
                        }
                    }
                });
            });

        // TODO Was diagnosing issue with Guice DI
//        if (routeBuilderCache.stationEmpty()) {
//            throw new RuntimeException("No cached station after station build");
//        }

        pairs.keySet().forEach(pair -> {
            Node startNode = routeBuilderCache.getStation(tx, pair.getBeginId());
            Node endNode = routeBuilderCache.getStation(tx, pair.getEndId());
            createLinkRelationship(startNode, endNode, route.getTransportMode());
        });

    }

    private boolean includeBothStops(GraphFilter filter, StopCalls.StopLeg leg) {
        return filter.shouldInclude(leg.getFirst()) && filter.shouldInclude(leg.getSecond());
    }

    private void createLinkRelationship(Node from, Node to, TransportMode mode) {
        if (from.hasRelationship(OUTGOING, LINKED)) {
            Iterable<Relationship> existings = from.getRelationships(OUTGOING, LINKED);

            // if there is an existing link between staions then update iff the transport mode not already present
            for (Relationship existing : existings) {
                if (existing.getEndNode().equals(to)) {
                    Set<TransportMode> existingModes = getTransportModes(existing);
                    if (!existingModes.contains(mode)) {
                        addTransportMode(existing, mode);
                    }
                    return;
                }
            }
        }

        Relationship stationsLinked = from.createRelationshipTo(to, LINKED);
        addTransportMode(stationsLinked, mode);
    }

    private void createRouteStationNode(Transaction tx, RouteStation routeStation, GraphBuilderCache builderCache) {
        Node routeStationNode = createGraphNode(tx, Labels.ROUTE_STATION);

        logger.debug(format("Creating route station %s nodeId %s", routeStation.getId(), routeStationNode.getId()));
        GraphProps.setProperty(routeStationNode, routeStation);
        setProperty(routeStationNode, routeStation.getStation());
        setProperty(routeStationNode, routeStation.getRoute());

        Set<TransportMode> modes = routeStation.getTransportModes();
        if (modes.size()==1) {
            setProperty(routeStationNode, modes.iterator().next());
        } else {
            logger.error("Unable to set transportmode property as more than one mode for " + routeStation);
        }

        builderCache.putRouteStation(routeStation.getId(), routeStationNode);
    }

    private Node createStationNode(Transaction tx, Station station) {

        Set<Labels> labels = Labels.forMode(station.getTransportModes());
        logger.debug(format("Creating station node: %s with labels: %s ", station, labels));
        Node stationNode = createGraphNode(tx, labels);
        setProperty(stationNode, station);
        return stationNode;
    }

    public static class Ready {
        private Ready() {
            // prevent guice creating this, want to create dependency on the Builder
        }
    }

}
