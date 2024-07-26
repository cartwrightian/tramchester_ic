package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ClosedStation;
import com.tramchester.domain.dates.DateRange;
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

    private final GraphDatabase database;
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
        this.database = database;
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

    private void recordDiversionsAtNode(final ImmutableGraphNode node, final GraphTransaction txn) {
        final IdFor<Station> stationId = node.getStationId();
        final Station station = stationRepository.getStationById(stationId);

        final Stream<ImmutableGraphRelationship> outgoingDiversion = node.getRelationships(txn, Direction.OUTGOING, DIVERSION);

        final Set<DateRange> ranges = outgoingDiversion.map(ImmutableGraphRelationship::getDateRange).collect(Collectors.toSet());

        logger.info("Recording diversion for " + stationId + " for " + ranges);

        stationsWithDiversions.add(station, ranges);
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
//        final IdSet<Station> closedStationIds = closedStations.stream().map(ClosedStation::getStationId).collect(IdSet.idCollector());

        if (closedStations.isEmpty()) {
            logger.warn("No station closures are given for " + source.getName());
            return;
        }

        closedStations.stream().
                filter(closedStation -> graphFilter.shouldInclude(closedStation.getStation())).
//                filter(closedStation -> closedStation.getStation().getGridPosition().isValid()).
            forEach(closedStation -> {

                // TODO This excludes all stations with closures, more sophisticated would be to ID how/if closure
                // dates overlaps and only exclude if really necessary
//                Set<Station> nearbyOpenStations = closedStation.getDiversionAroundClosure().stream().
//                        filter(nearby -> !closedStationIds.contains(nearby.getId())).
//                        collect(Collectors.toSet());

//                if (nearbyOpenStations.isEmpty()) {
//                    logger.error("Unable to find any walks to add for " + closedStation);
//                } else {
//                }
                createWalks(closedStation);


            });
    }

    private void createWalks(final ClosedStation closedStation) {
        try(TimedTransaction timedTransaction = new TimedTransaction(database, logger, "create diversions for " +closedStation.getStationId())) {
            final MutableGraphTransaction txn = timedTransaction.transaction();
            addDiversionsToAndFromClosed(txn, closedStation);
            final int added = addDiversionsAroundClosed(txn, closedStation);
            if (added==0) {
                logger.warn("Did not create any diversions around closure of " + closedStation.getStationId());
            }
            timedTransaction.commit();
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
                node = nodes.get(0);
            }
            node.setSourceName(sourceConfig.getName());

            txn.commit();
        }
    }

    private void addDiversionsToAndFromClosed(final MutableGraphTransaction txn,final ClosedStation closure) {
        final Station closedStation = closure.getStation();

        Set<Station> others = closure.getDiversionToFromClosure();

        final MutableGraphNode closedNode = txn.findNodeMutable(closedStation);
        if (closedNode==null) {
            String msg = "Could not find database node for from: " + closedStation.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        logger.info("Adding Diversion relations to/from closed " + closedStation.getId() + " to/from " + asIds(others));

        others.stream().filter(graphFilter::shouldInclude).forEach(otherStation -> {

            final Duration cost = geography.getWalkingDuration(closedStation, otherStation);

            logger.info(format("Create diversion to/from %s and %s cost %s", closedStation.getId(), otherStation.getId(), cost));

            final MutableGraphNode otherNode = txn.findNodeMutable(otherStation);
            if (otherNode==null) {
                String msg = "Could not find database node for to: " + otherStation.getId();
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            final MutableGraphRelationship fromClosed = createRelationship(txn, closedNode, otherNode, DIVERSION);
            final MutableGraphRelationship fromOther = createRelationship(txn, otherNode, closedNode, DIVERSION);

            setCommonProperties(fromClosed, cost, closure);
            setCommonProperties(fromOther, cost, closure);

            fromClosed.set(otherStation);
            fromOther.set(closedStation);

            otherNode.addLabel(GraphLabel.HAS_DIVERSION);
            stationsWithDiversions.add(otherStation, closure.getDateRange());

        });
    }

    /***
     * Create diversions around a closed station (not to/from that station)
     * @param txn current transaction
     * @param closure station closure details
     * @return number added
     */
    private int addDiversionsAroundClosed(final MutableGraphTransaction txn, final ClosedStation closure) {

        final Set<Station> stationsToLink = closure.getDiversionAroundClosure();

//        final Set<Pair<Station, Station>> toLinkViaDiversion = nearbyStations.stream().
//                flatMap(nearbyA -> nearbyStations.stream().map(nearbyB -> Pair.of(nearbyA, nearbyB))).
//                filter(pair -> !pair.getLeft().equals(pair.getRight())).
//                filter(pair -> range.within(geography.getDistanceBetweenInMeters(pair.getLeft(), pair.getRight()))).
//                collect(Collectors.toSet());

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

            Stream<ImmutableGraphRelationship> alreadyPresent = firstNode.getRelationships(txn, Direction.OUTGOING, DIVERSION);
            guardNotOverlappingExisting(alreadyPresent,closure);

            final MutableGraphNode secondNode = txn.findNodeMutable(second);

            firstNode.addLabel(GraphLabel.HAS_DIVERSION);

            final MutableGraphRelationship relationship = createRelationship(txn, firstNode, secondNode, DIVERSION);
            setCommonProperties(relationship, cost, closure);
            relationship.set(second);
        });

        // TODO Is this correct?
        uniqueStations.forEach(station -> stationsWithDiversions.add(station, closure.getDateRange()));

        return toLinkViaDiversion.size();
    }

    private void guardNotOverlappingExisting(Stream<ImmutableGraphRelationship> alreadyPresent, ClosedStation closure) {
        alreadyPresent.forEach(relationship -> {
            if (relationship.getDateRange().overlapsWith(closure.getDateRange())) {
                String msg = format("Existing DIVERSION relationship daterange %s overlaps with new closure %s", relationship.getDateRange(), closure.getDateRange());
                logger.error(msg);
                throw new RuntimeException(msg);
            }
        });
    }

    private void setCommonProperties(final MutableGraphRelationship relationship, final Duration cost, final ClosedStation closure) {
        relationship.setCost(cost);
        relationship.setDateRange(closure.getDateRange());
    }

    public static class Ready {
        private Ready() {

        }
    }
}
