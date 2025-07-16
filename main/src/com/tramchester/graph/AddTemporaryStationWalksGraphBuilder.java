package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.TemporaryStationWalk;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.CreateNodesAndRelationships;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationsWithDiversionRepository;
import com.tramchester.repository.TemporaryStationWalksRepository;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.GraphPropertyKey.SOURCE_NAME_PROP;
import static com.tramchester.graph.TransportRelationshipTypes.DIVERSION;
import static java.lang.String.format;

@LazySingleton
public class AddTemporaryStationWalksGraphBuilder extends CreateNodesAndRelationships implements StationsWithDiversionRepository {
    private static final Logger logger = LoggerFactory.getLogger(AddTemporaryStationWalksGraphBuilder.class);

    private final TemporaryStationWalksRepository walksRepository;
    private final TramchesterConfig config;
    private final GraphFilter graphFilter;
    private final Geography geography;
    private final StationsWithDiversion stationsWithDiversions;
    private final StationRepository stationRepository;

    @Inject
    public AddTemporaryStationWalksGraphBuilder(GraphDatabaseNeo4J database, GraphFilter graphFilter,
                                                TemporaryStationWalksRepository walksRepository,
                                                TramchesterConfig config,
                                                @SuppressWarnings("unused") StationsAndLinksGraphBuilder.Ready ready,
                                                Geography geography, StationsWithDiversion stationsWithDiversions,
                                                StationRepository stationRepository) {
        super(database);
        this.graphFilter = graphFilter;
        this.walksRepository = walksRepository;
        this.config = config;

        this.geography = geography;
        this.stationsWithDiversions = stationsWithDiversions;
        this.stationRepository = stationRepository;

    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        config.getGTFSDataSource().forEach(source -> {
            final boolean hasDBFlag = hasDBFlag(source);

            final String sourceName = source.getName();

            if (config.getMaxWalkingConnections()==0) {
                final String msg = "Max walking connections set to zero, creating walks makes no sense";
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            if (hasDBFlag) {
                logger.info("Node and prop present, assuming walks already built in DB for " + sourceName);
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
        try (final ImmutableGraphTransactionNeo4J txn = graphDatabase.beginTx()) {
            final Stream<ImmutableGraphNode> haveDiversions = txn.findNodes(GraphLabel.HAS_DIVERSION);
            haveDiversions.forEach(node -> recordDiversionsAtNode(node, txn));
        }
    }

    /***
     * Record existing diversions i.e. from prebuilt graph
     * @param node current node
     * @param txn transaction
     */
    private void recordDiversionsAtNode(final ImmutableGraphNode node, final GraphTransactionNeo4J txn) {
        final IdFor<Station> stationId = node.getStationId();
        final Station station = stationRepository.getStationById(stationId);

        final Stream<ImmutableGraphRelationship> outgoingDiversion = node.getRelationships(txn, GraphDirection.Outgoing, DIVERSION);

        final Set<DateTimeRange> ranges = outgoingDiversion.map(ImmutableGraphRelationship::getDateTimeRange).collect(Collectors.toSet());

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
        final List<TemporaryStationWalk> walks = walksRepository.getTemporaryWalksFor(source.getDataSourceId()).stream().
                filter(walk -> graphFilter.shouldInclude(walk.getStationPair())).
                toList();

        try(TimedTransaction txn = graphDatabase.beginTimedTxMutable(logger, "create walks for " +source.getDataSourceId())) {

            for(TemporaryStationWalk walk : walks) {
                addStationWalk(txn, walk);
            }

            txn.commit();
        }
    }

    private boolean hasDBFlag(final GTFSSourceConfig sourceConfig) {
        logger.info("Checking DB if temp walks added for " + sourceConfig.getName());
        final boolean flag;
        try (final MutableGraphTransactionNeo4J txn = graphDatabase.beginTxMutable()) {
            final String value = sourceConfig.getName();

            flag = txn.hasAnyMatching(GraphLabel.TEMP_WALKS_ADDED, SOURCE_NAME_PROP.getText(), value);
        }
        return flag;
    }

    private void addDBFlag(final GTFSSourceConfig sourceConfig) {
        try (MutableGraphTransactionNeo4J txn = graphDatabase.beginTxMutable()) {
            final List<MutableGraphNode> nodes = txn.findNodesMutable(GraphLabel.TEMP_WALKS_ADDED).toList();

            final MutableGraphNode node;
            if (nodes.isEmpty()) {
                logger.info("Creating " + GraphLabel.TEMP_WALKS_ADDED + " node");
                node = createGraphNode(txn, GraphLabel.TEMP_WALKS_ADDED);
            } else {
                if (nodes.size() != 1) {
                    final String message = "Found too many " + GraphLabel.TEMP_WALKS_ADDED + " nodes, should be one only";
                    logger.error(message);
                    throw new RuntimeException(message);
                }
                logger.info("Found " + GraphLabel.TEMP_WALKS_ADDED + " node");
                node = nodes.getFirst();
            }
            node.setSourceName(sourceConfig.getName());

            txn.commit();
        }
    }

    private void addStationWalk(final MutableGraphTransactionNeo4J txn, final TemporaryStationWalk temporaryStationWalk) {
        final Station first = temporaryStationWalk.getStationPair().first();
        final Station second = temporaryStationWalk.getStationPair().second();

        final MutableGraphNode firstNode = findNodeFor(txn, first);
        final MutableGraphNode secondNode = findNodeFor(txn, second);

        logger.debug("Adding Temp Station Walk to/from " + firstNode.getId() + " and " + second.getId());

        final Duration cost = geography.getWalkingDuration(first, second);

        logger.info(format("Create walk to/from %s and %s cost %s", first.getId(), second.getId(), cost));

        final MutableGraphRelationship fromFirst = createRelationship(txn, firstNode, secondNode, DIVERSION);
        final MutableGraphRelationship fromSecond = createRelationship(txn, secondNode, firstNode, DIVERSION);

        setCommonProperties(fromFirst, cost, temporaryStationWalk);
        setCommonProperties(fromSecond, cost, temporaryStationWalk);

        fromFirst.set(second);
        fromSecond.set(first);

        // might be an existing label but that is fine
        firstNode.addLabel(GraphLabel.HAS_DIVERSION);
        secondNode.addLabel(GraphLabel.HAS_DIVERSION);

        final DateTimeRange dateTimeRange = DateTimeRange.of(temporaryStationWalk.getDateRange(), temporaryStationWalk.getTimeRange());
        stationsWithDiversions.add(first, dateTimeRange);
        stationsWithDiversions.add(second, dateTimeRange);
    }

    @NotNull
    private static MutableGraphNode findNodeFor(final MutableGraphTransactionNeo4J txn, final Station station) {
        final MutableGraphNode closedNode = txn.findNodeMutable(station);
        if (closedNode==null) {
            String msg = "Could not find database node for from: " + station.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        return closedNode;
    }

    private void setCommonProperties(final MutableGraphRelationship relationship, final Duration cost, final TemporaryStationWalk stationWalk) {
        relationship.setCost(cost);
        relationship.setDateRange(stationWalk.getDateRange());
        relationship.setTimeRange(TimeRange.AllDay());
    }

    @Override
    public boolean hasDiversions(final Station station) {
        return stationsWithDiversions.hasDiversions(station);
    }


    @Override
    public Set<DateTimeRange> getDateTimeRangesFor(final Station station) {
        return stationsWithDiversions.getDateTimeRangesFor(station);
    }

    public static class Ready {
        private Ready() {

        }
    }
}
