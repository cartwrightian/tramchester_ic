package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ClosedStation;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphRelationship;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.CreateNodesAndRelationships;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationsWithDiversionRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.id.HasId.asIds;
import static com.tramchester.graph.GraphPropertyKey.SOURCE_NAME_PROP;
import static com.tramchester.graph.TransportRelationshipTypes.DIVERSION;
import static java.lang.String.format;

@LazySingleton
public class AddWalksForClosedGraphBuilder extends CreateNodesAndRelationships implements StationsWithDiversionRepository {
    private static final Logger logger = LoggerFactory.getLogger(AddWalksForClosedGraphBuilder.class);

    private final GraphDatabase database;
    private final ClosedStationsRepository closedStationsRepository;
    private final TramchesterConfig config;
    private final GraphFilter filter;
    private final Geography geography;
    private final StationsWithDiversions stationsWithDiversions;

    @Inject
    public AddWalksForClosedGraphBuilder(GraphDatabase database, GraphFilter filter,
                                         ClosedStationsRepository closedStationsRepository,
                                         TramchesterConfig config,
                                         @SuppressWarnings("unused") StationsAndLinksGraphBuilder.Ready ready,
                                         Geography geography) {
        super(database);
        this.database = database;
        this.filter = filter;
        this.closedStationsRepository = closedStationsRepository;
        this.config = config;

        this.geography = geography;

        stationsWithDiversions = new StationsWithDiversions();
    }

    @PostConstruct
    public void start() {

        logger.info("starting");

        config.getGTFSDataSource().forEach(source -> {
            boolean hasDBFlag = hasDBFlag(source);

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
                return;
            }

            createWalksForClosed(source);
            addDBFlag(source);
            reportStats();
        });
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        stationsWithDiversions.close();
    }

    public Ready getReady() {
        return new Ready();
    }

    private void createWalksForClosed(GTFSSourceConfig source) {
        logger.info("Add walks for closed stations for " + source.getName());
        final Set<ClosedStation> closures = closedStationsRepository.getClosedStationsFor(source.getDataSourceId());
        final IdSet<Station> closedStationIds = closures.stream().map(ClosedStation::getStationId).collect(IdSet.idCollector());

        if (closures.isEmpty()) {
            logger.warn("No station closures are given for " + source.getName());
            return;
        }

        final MarginInMeters range = MarginInMeters.of(config.getNearestStopForWalkingRangeKM());

        closures.stream().
                filter(closedStation -> filter.shouldInclude(closedStation.getStation())).
                filter(closedStation -> closedStation.getStation().getGridPosition().isValid()).
            forEach(closedStation -> {

                // TODO This excludes all stations with closures, more sophisticated would be to ID how/if closure
                // dates overlaps and only exclude if really necessary
                Set<Station> nearbyOpenStations = closedStation.getNearbyLinkedStation().stream().
                        filter(nearby -> !closedStationIds.contains(nearby.getId())).
                        collect(Collectors.toSet());

                if (nearbyOpenStations.isEmpty()) {
                    logger.error("Unable to find any walks to add for " + closedStation);
                } else {
                    createWalks(nearbyOpenStations, closedStation, range);
                }

            });
    }

    private void createWalks(Set<Station> linkedNearby, ClosedStation closedStation, MarginInMeters range) {
        try(TimedTransaction timedTransaction = new TimedTransaction(database, logger, "create diversions for " +closedStation.getStationId())) {
            MutableGraphTransaction txn = timedTransaction.transaction();
            addDiversionsToAndFromClosed(txn, linkedNearby, closedStation);
            int added = addDiversionsAroundClosed(txn, linkedNearby, closedStation, range);
            if (added==0) {
                logger.warn("Did not create any diversions around closure of " + closedStation.getStationId());
            }
            timedTransaction.commit();
        }
    }

    private boolean hasDBFlag(GTFSSourceConfig sourceConfig) {
        logger.info("Checking DB if walks added for " + sourceConfig.getName() +  " closed stations");
        boolean flag;
        try (MutableGraphTransaction txn = graphDatabase.beginTxMutable()) {
            String value = sourceConfig.getName();

            flag = txn.hasAnyMatching(GraphLabel.WALK_FOR_CLOSED_ENABLED, SOURCE_NAME_PROP.getText(), value);
        }
        return flag;
    }

    private void addDBFlag(GTFSSourceConfig sourceConfig) {
        try (MutableGraphTransaction txn = graphDatabase.beginTxMutable()) {
            Stream<MutableGraphNode> query = txn.findNodesMutable(GraphLabel.WALK_FOR_CLOSED_ENABLED);
            List<MutableGraphNode> nodes = query.toList();

            MutableGraphNode node;
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

    private void addDiversionsToAndFromClosed(MutableGraphTransaction txn, Set<Station> others, ClosedStation closure) {
        Station closedStation = closure.getStation();

        MutableGraphNode closedNode = txn.findNodeMutable(closedStation);
        if (closedNode==null) {
            String msg = "Could not find database node for from: " + closedStation.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        logger.info("Adding Diversion relations to/from closed " + closedStation.getId() + " to/from " + asIds(others));

        others.stream().filter(filter::shouldInclude).forEach(otherStation -> {

            Duration cost = geography.getWalkingDuration(closedStation, otherStation);

            logger.info(format("Create diversion to/from %s and %s cost %s", closedStation.getId(), otherStation.getId(), cost));

            MutableGraphNode otherNode = txn.findNodeMutable(otherStation);
            if (otherNode==null) {
                String msg = "Could not find database node for to: " + otherStation.getId();
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            MutableGraphRelationship fromClosed = createRelationship(txn, closedNode, otherNode, DIVERSION);
            MutableGraphRelationship fromOther = createRelationship(txn, otherNode, closedNode, DIVERSION);

            setCommonProperties(fromClosed, cost, closure);
            setCommonProperties(fromOther, cost, closure);

            fromClosed.set(otherStation);
            fromOther.set(closedStation);

            stationsWithDiversions.add(otherStation, closure.getDateRange());

        });
    }

    private int addDiversionsAroundClosed(MutableGraphTransaction txn, Set<Station> nearbyStations, ClosedStation closure, MarginInMeters range) {
        Set<Pair<Station, Station>> toLinkViaDiversion = nearbyStations.stream().
                flatMap(nearbyA -> nearbyStations.stream().map(nearbyB -> Pair.of(nearbyA, nearbyB))).
                filter(pair -> !pair.getLeft().equals(pair.getRight())).
                filter(pair -> range.within(geography.getDistanceBetweenInMeters(pair.getLeft(), pair.getRight()))).
                collect(Collectors.toSet());

        if (toLinkViaDiversion.isEmpty()) {
            return 0;
        }

        logger.info("Create " + toLinkViaDiversion.size() + " diversions to/from stations");

        Set<Station> uniqueStations = new HashSet<>();

        toLinkViaDiversion.forEach(pair -> {
            Station first = pair.getLeft();
            Station second = pair.getRight();

            uniqueStations.add(first);
            uniqueStations.add(second);

            Duration cost = geography.getWalkingDuration(first, second);

            logger.info(format("Create diversion between %s and %s cost %s", first.getId(), second.getId(), cost));

            MutableGraphNode firstNode = txn.findNodeMutable(first);
            MutableGraphNode secondNode = txn.findNodeMutable(second);

            MutableGraphRelationship relationship = createRelationship(txn, firstNode, secondNode, DIVERSION);
            setCommonProperties(relationship, cost, closure);
            relationship.set(second);
        });

        uniqueStations.forEach(station -> stationsWithDiversions.add(station, closure.getDateRange()));

        return toLinkViaDiversion.size();
    }

    private void setCommonProperties(MutableGraphRelationship relationship, Duration cost, ClosedStation closure) {
        relationship.setCost(cost);
        relationship.setDateRange(closure.getDateRange());
    }

    @Override
    public boolean hasDiversions(Station station) {
        return stationsWithDiversions.hasDiversions(station);
    }

    @Override
    public Set<DateRange> getDateRangesFor(Station station) {
        return stationsWithDiversions.getDateRangesFor(station);
    }

    public static class Ready {
        private Ready() {

        }
    }

    private static class StationsWithDiversions implements StationsWithDiversionRepository {

        private final Map<Station, Set<DateRange>> closures;

        private StationsWithDiversions() {
            closures = new HashMap<>();
        }

        @Override
        public boolean hasDiversions(Station station) {
            return closures.containsKey(station);
        }

        @Override
        public Set<DateRange> getDateRangesFor(Station station) {
            return closures.get(station);
        }

        public void add(Station station, DateRange dateRange) {
            if (!closures.containsKey(station)) {
                closures.put(station, new HashSet<>());
            }
            closures.get(station).add(dateRange);
        }

        public void close() {
            closures.clear();
        }
    }
}
