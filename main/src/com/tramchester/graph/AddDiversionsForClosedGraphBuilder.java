package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.CreateNodesAndRelationships;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.id.HasId.asIds;
import static com.tramchester.graph.GraphPropertyKey.SOURCE_NAME_PROP;
import static com.tramchester.graph.TransportRelationshipTypes.DIVERSION;
import static java.lang.String.format;

@LazySingleton
public class AddDiversionsForClosedGraphBuilder extends CreateNodesAndRelationships  {
    private static final Logger logger = LoggerFactory.getLogger(AddDiversionsForClosedGraphBuilder.class);

    private final ClosedStationsRepository closedStationsRepository;
    private final TramchesterConfig config;
    private final GraphFilter graphFilter;
    private final Geography geography;
    private final StationsWithDiversion stationsWithDiversions;
    private final StationRepository stationRepository;

    @Inject
    public AddDiversionsForClosedGraphBuilder(GraphDatabase database, GraphFilter graphFilter,
                                              ClosedStationsRepository closedStationsRepository,
                                              TramchesterConfig config,
                                              @SuppressWarnings("unused") StationsAndLinksGraphBuilder.Ready ready,
                                              StationsWithDiversion stationsWithDiversion,
                                              Geography geography, StationRepository stationRepository) {
        super(database);
        this.graphFilter = graphFilter;
        this.closedStationsRepository = closedStationsRepository;
        this.config = config;

        this.geography = geography;
        this.stationRepository = stationRepository;
        this.stationsWithDiversions = stationsWithDiversion;
    }

    @PostConstruct
    public void start() {

        logger.info("starting");

        config.getGTFSDataSource().forEach(source -> {
            final boolean hasDBFlag = hasDBFlag(source);

            final String sourceName = source.getName();
            if (!source.getAddWalksForClosed()) {
                logger.info("Create walks is disabled in configuration for " + sourceName);
                if (hasDBFlag) {
                    final String message = "DB rebuild is required, mismatch on config (false) and db (true) for " + sourceName;
                    logger.error(message);
                    throw new RuntimeException(message);
                }
                return;
            }
            // else enabled for this source

            if (config.getMaxWalkingConnections()==0) {
                final String msg = "Max walking connections set to zero, creating walks for neighbours makes no sense";
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            if (hasDBFlag) {
                logger.info("Node and prop present, assuming walks for closed already built in DB for " + sourceName);
                recordDiversionsInPlace();
                return;
            }

            createWalksForClosed(source);
            addDBFlag(source);
            reportStats();
        });
        logger.info("started");
    }

    private void recordDiversionsInPlace() {
        try (final ImmutableGraphTransaction txn = graphDatabase.beginTx()) {
            final Stream<ImmutableGraphNode> haveDiversions = txn.findNodes(GraphLabel.HAS_DIVERSION);
            haveDiversions.forEach(node -> recordDiversionsAtNode(node, txn));
        }
    }

    /***
     * For pre-built graphs load the diversions into the repository
     * @param node current node
     * @param txn transaction
     */
    private void recordDiversionsAtNode(final ImmutableGraphNode node, final GraphTransaction txn) {
        final IdFor<Station> stationId = node.getStationId();
        final Station station = stationRepository.getStationById(stationId);

        final Stream<ImmutableGraphRelationship> outgoingDiversion = node.getRelationships(txn, Direction.OUTGOING, DIVERSION);

        final Set<DateTimeRange> ranges = outgoingDiversion.map(GraphRelationship::getDateTimeRange).collect(Collectors.toSet());

        logger.info("Recording diversion for " + stationId + " for " + ranges);

        stationsWithDiversions.set(station, ranges);
    }

    @PreDestroy
    public void stop() {
        stationsWithDiversions.close();
    }

    public Ready getReady() {
        return new Ready();
    }

    private void createWalksForClosed(final GTFSSourceConfig source) {
        logger.info("Add walks for closed stations for " + source.getName());
        final Set<ClosedStation> closedStations = closedStationsRepository.getClosedStationsFor(source.getDataSourceId());

        if (closedStations.isEmpty()) {
            logger.warn("No station closures are given for " + source.getName());
            return;
        }

        closedStations.stream().
                filter(closedStation -> graphFilter.shouldInclude(closedStation.getStationId())).
                forEach(this::createWalks);
    }

    private void createWalks(final ClosedStation closedStation) {
        try(TimedTransaction txn = graphDatabase.beginTimedTxMutable(logger, "create diversions for " +closedStation.getStationId())) {
            addDiversionsToAndFromClosed(txn, closedStation);
            final int added = addDiversionsAroundClosed(txn, closedStation);
            if (added==0) {
                logger.warn("Did not create any diversions around closure of " + closedStation.getStationId());
            }
            txn.commit();
        }
    }

    private boolean hasDBFlag(final GTFSSourceConfig sourceConfig) {
        logger.info("Checking DB if walks added for " + sourceConfig.getName() +  " closed stations");
        final boolean flag;
        try (final MutableGraphTransaction txn = graphDatabase.beginTxMutable()) {
            final String value = sourceConfig.getName();

            flag = txn.hasAnyMatching(GraphLabel.WALK_FOR_CLOSED_ENABLED, SOURCE_NAME_PROP.getText(), value);
        }
        return flag;
    }

    private void addDBFlag(final GTFSSourceConfig sourceConfig) {
        try (MutableGraphTransaction txn = graphDatabase.beginTxMutable()) {
            final List<MutableGraphNode> nodes = txn.findNodesMutable(GraphLabel.WALK_FOR_CLOSED_ENABLED).toList();

            final MutableGraphNode node;
            if (nodes.isEmpty()) {
                logger.info("Creating " + GraphLabel.WALK_FOR_CLOSED_ENABLED + " node");
                node = createGraphNode(txn, GraphLabel.WALK_FOR_CLOSED_ENABLED);
            } else {
                if (nodes.size() != 1) {
                    final String message = "Found too many " + GraphLabel.WALK_FOR_CLOSED_ENABLED + " nodes, should be one only";
                    logger.error(message);
                    throw new RuntimeException(message);
                }
                logger.info("Found " + GraphLabel.WALK_FOR_CLOSED_ENABLED + " node");
                node = nodes.getFirst();
            }
            node.setSourceName(sourceConfig.getName());

            txn.commit();
        }
    }

    private void addDiversionsToAndFromClosed(final MutableGraphTransaction txn, final ClosedStation closedStation) {
        final Station actualStation = stationRepository.getStationById(closedStation.getStationId());

        final Set<Station> others = closedStation.getDiversionToFromClosure();

        final MutableGraphNode closedNode = txn.findNodeMutable(actualStation);
        if (closedNode==null) {
            String msg = "Could not find database node for from: " + actualStation.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        logger.info("Adding Diversion relations to/from closed " + actualStation.getId() + " to/from " + asIds(others));

        others.stream().filter(graphFilter::shouldInclude).forEach(otherStation -> {

            final Duration cost = geography.getWalkingDuration(actualStation, otherStation);

            logger.info(format("Create diversion to/from %s and %s cost %s", actualStation.getId(), otherStation.getId(), cost));

            final MutableGraphNode otherNode = txn.findNodeMutable(otherStation);
            if (otherNode==null) {
                String msg = "Could not find database node for to: " + otherStation.getId();
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            final MutableGraphRelationship fromClosed = createRelationship(txn, closedNode, otherNode, DIVERSION);
            final MutableGraphRelationship fromOther = createRelationship(txn, otherNode, closedNode, DIVERSION);

            setCommonProperties(fromClosed, cost, closedStation);
            setCommonProperties(fromOther, cost, closedStation);

            fromClosed.set(otherStation);
            fromOther.set(actualStation);

            otherNode.addLabel(GraphLabel.HAS_DIVERSION);
            stationsWithDiversions.add(otherStation, closedStation.getDateTimeRange());

        });
    }

    /***
     * Create diversions around a closed station (not to/from that station)
     * @param txn current transaction
     * @param closedStation station closure details
     * @return number added
     */
    private int addDiversionsAroundClosed(final MutableGraphTransaction txn, final ClosedStation closedStation) {

        final Set<Station> stationsToLink = closedStation.getDiversionAroundClosure();

        final Set<Pair<Station, Station>> toLinkViaDiversion = stationsToLink.stream().
            flatMap(nearbyA -> stationsToLink.stream().map(nearbyB -> Pair.of(nearbyA, nearbyB))).
            filter(pair -> !pair.getLeft().equals(pair.getRight())).
            collect(Collectors.toSet());

        if (toLinkViaDiversion.isEmpty()) {
            return 0;
        }

        logger.info("Create " + toLinkViaDiversion.size() + " diversions to/from stations around closure");

        final Set<Station> uniqueStations = new HashSet<>();

        toLinkViaDiversion.forEach(pair -> {
            final Station first = pair.getLeft();
            final Station second = pair.getRight();

            uniqueStations.add(first);
            uniqueStations.add(second);

            final Duration cost = geography.getWalkingDuration(first, second);

            logger.info(format("Create diversion between %s and %s cost %s", first.getId(), second.getId(), cost));

            final MutableGraphNode firstNode = txn.findNodeMutable(first);

//            Stream<ImmutableGraphRelationship> alreadyPresent = firstNode.getRelationships(txn, Direction.OUTGOING, DIVERSION);

            final MutableGraphNode secondNode = txn.findNodeMutable(second);

            firstNode.addLabel(GraphLabel.HAS_DIVERSION);

            final MutableGraphRelationship relationship = createRelationship(txn, firstNode, secondNode, DIVERSION);
            setCommonProperties(relationship, cost, closedStation);
            relationship.set(second);
        });

        uniqueStations.forEach(station -> stationsWithDiversions.add(station, closedStation.getDateTimeRange()));

        return toLinkViaDiversion.size();
    }

    private void setCommonProperties(final MutableGraphRelationship relationship, final Duration cost, final ClosedStation closure) {
        relationship.setCost(cost);
        relationship.setDateTimeRange(closure.getDateTimeRange());
    }

    public static class Ready {
        private Ready() {

        }
    }
}
