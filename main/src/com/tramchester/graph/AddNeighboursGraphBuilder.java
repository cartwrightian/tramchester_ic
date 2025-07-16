package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.NeighbourConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.databaseManagement.GraphDatabaseMetaInfo;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransactionNeo4J;
import com.tramchester.graph.facade.TimedTransaction;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.CreateNodesAndRelationships;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Set;

@LazySingleton
public class AddNeighboursGraphBuilder extends CreateNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(AddNeighboursGraphBuilder.class);

    private final GraphDatabaseMetaInfo databaseMetaInfo;
    private final StationRepository stationRepository;
    private final NeighboursRepository neighboursRepository;
    private final TramchesterConfig config;
    private final GraphFilter filter;

    @Inject
    public AddNeighboursGraphBuilder(GraphDatabaseNeo4J database, GraphDatabaseMetaInfo databaseMetaInfo, GraphFilter filter,
                                     StationRepository repository,
                                     TramchesterConfig config,
                                     @SuppressWarnings("unused") StationsAndLinksGraphBuilder.Ready ready,
                                     NeighboursRepository neighboursRepository) {
        super(database);
        this.databaseMetaInfo = databaseMetaInfo;
        this.filter = filter;
        this.stationRepository = repository;
        this.config = config;
        this.neighboursRepository = neighboursRepository;

    }

    @PostConstruct
    public void start() {

        logger.info("starting");

        boolean hasDBFlag = hasDBFlag();

        if (!config.hasNeighbourConfig()) {
            logger.info("Create neighbours is disabled in configuration");
            if (hasDBFlag) {
                final String message = "DB rebuild is required, mismatch on config (false) and db (true)";
                logger.error(message);
                throw new RuntimeException(message);
            }
            return;
        }

        NeighbourConfig neighbourConfig = config.getNeighbourConfig();

        if (neighbourConfig.getMaxNeighbourConnections()==0) {
            String msg = "createNeighbours is true but maxNeighbourConnections==0";
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        if (hasDBFlag) {
            logger.info("Node NEIGHBOURS_ENABLED present, assuming neighbours already built in DB");
            return;
        }

        createNeighboursInDB();
        addDBFlag();
        reportStats();
        logger.info("started");
    }

    public Ready getReady() {
        return new Ready();
    }

    private void createNeighboursInDB() {
        try(TimedTransaction txn = graphDatabase.beginTimedTxMutable(logger, "create neighbours")) {
                stationRepository.getActiveStationStream().
                    filter(filter::shouldInclude).
                    filter(station -> neighboursRepository.hasNeighbours(station.getId())).
                    forEach(station -> {
                        final Set<StationToStationConnection> links = neighboursRepository.getNeighbourLinksFor(station.getId());
                        addNeighbourRelationships(txn, filter, station, links);
                });
            txn.commit();
        }
    }

    private boolean hasDBFlag() {
        boolean flag;
        try (MutableGraphTransactionNeo4J txn = graphDatabase.beginTxMutable()) {
            flag = databaseMetaInfo.isNeighboursEnabled(txn);
        }
        return flag;
    }

    private void addDBFlag() {
        try (MutableGraphTransactionNeo4J txn = graphDatabase.beginTxMutable()) {
            databaseMetaInfo.setNeighboursEnabled(txn);
            txn.commit();
        }
    }

    private void addNeighbourRelationships(MutableGraphTransactionNeo4J txn, GraphFilter graphFilter, Station from, Set<StationToStationConnection> links) {
        final MutableGraphNode fromNode = txn.findNodeMutable(from);
        if (fromNode==null) {
            String msg = "Could not find database node for from: " + from.getId();
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        logger.debug("Adding neighbour relations from " + from.getId());

        links.stream().
                filter(link -> graphFilter.shouldInclude(link.getEnd())).
                forEach(link -> {
                    final Station station = link.getEnd();
                    final MutableGraphNode toNode = txn.findNodeMutable(station);
                    if (toNode==null) {
                        String msg = "Could not find database node for to: " + link.getEnd().getId();
                        logger.error(msg);
                        throw new RuntimeException(msg);
                    }
                    final Duration walkingCost = link.getConnectionTime();
                    addNeighbourRelationship(txn, fromNode, toNode, walkingCost);
                });
    }

    public static class Ready {
        private Ready() {

        }
    }


}
