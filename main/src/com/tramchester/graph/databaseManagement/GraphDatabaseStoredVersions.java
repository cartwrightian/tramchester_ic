package com.tramchester.graph.databaseManagement;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.geo.BoundingBox;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.neo4j.GraphDatabaseNeo4J;
import com.tramchester.graph.core.neo4j.GraphTransactionFactory;
import com.tramchester.repository.DataSourceRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

@LazySingleton
public class GraphDatabaseStoredVersions {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseStoredVersions.class);

    private final TramchesterConfig config;
    private final GraphDatabaseMetaInfo databaseMetaInfo;

    @Inject
    public GraphDatabaseStoredVersions(TramchesterConfig config, GraphDatabaseMetaInfo databaseMetaInfo) {
        this.config = config;
        this.databaseMetaInfo = databaseMetaInfo;
    }

    @Deprecated
    public boolean upToDate(final GraphTransactionFactory transactionFactory, final DataSourceRepository dataSourceRepository) {
        try(GraphTransaction transaction = transactionFactory.begin(GraphDatabaseNeo4J.DEFAULT_TXN_TIMEOUT)) {
            return upToDate(dataSourceRepository, transaction);
        }
    }

    public boolean upToDate(final DataSourceRepository dataSourceRepository, final GraphTransaction transaction) {
        logger.info("Checking graph version information ");

        if (neighboursEnabledMismatch(transaction)) {
            return false;
        }

        if (boundingBoxMismatch(transaction, config.getBounds())) {
            logger.error("Mismatch on bounds, expected " + config.getBounds());
            return false;
        }

        if (!databaseMetaInfo.hasVersionInfo(transaction)) {
            logger.warn("Missing VERSION node, cannot check versions");
            return false;
        }

        Map<String, String> versionsFromDB = databaseMetaInfo.getVersions(transaction);

        Set<DataSourceInfo> dataSourceInfo = dataSourceRepository.getDataSourceInfo();

        if (versionsFromDB.size()!=dataSourceInfo.size()) {
            logger.warn("VERSION node property mismatch, got " +versionsFromDB.size() + " expected " + dataSourceInfo.size());
            return false;
        }

        Map<DataSourceInfo, Boolean> upToDate = checkIfDatasourcesUpToDate(dataSourceInfo, versionsFromDB);
        boolean allSourcesUpToDate = upToDate.values().stream().allMatch(flag -> flag);
        return allSourcesUpToDate;
    }

    private static Map<DataSourceInfo, Boolean> checkIfDatasourcesUpToDate(Set<DataSourceInfo> dataSourceInfo,
                                                                           Map<String, String> versionsFromDB) {
        final Map<DataSourceInfo, Boolean> upToDate = new HashMap<>();
        dataSourceInfo.forEach(sourceInfo -> {
            DataSourceID sourceName = sourceInfo.getID();
            String name = sourceName.name();
            logger.info("Checking version for " + sourceName);

            if (versionsFromDB.containsKey(name)) {
                String graphValue = versionsFromDB.get(name);
                boolean matches = sourceInfo.getVersion().equals(graphValue);
                upToDate.put(sourceInfo, matches);
                if (matches) {
                    logger.info("Got correct VERSION node value for " + sourceInfo);
                } else {
                    logger.warn(format("Mismatch on graph VERSION, got graph:'%s' datasource: '%s' for sourceinfo: %s",
                            graphValue, sourceInfo.getVersion(), sourceInfo));
                }
            } else {
                upToDate.put(sourceInfo, false);
                logger.warn("Could not find version for " + name + " properties were " + versionsFromDB);
            }
        });
        return upToDate;
    }

    private boolean boundingBoxMismatch(final GraphTransaction transaction, final BoundingBox bounds) {
        final boolean match = databaseMetaInfo.boundsMatch(transaction, bounds);
        if (!match) {
            logger.warn("Mismatch on bounds, did not match " + bounds);
        }
        return !match;
    }

    private boolean neighboursEnabledMismatch(GraphTransaction txn) {

        boolean fromDB = databaseMetaInfo.isNeighboursEnabled(txn);
        boolean fromConfig = config.hasNeighbourConfig();

        boolean matched = fromDB == fromConfig;
        if (matched) {
            logger.info("CreateNeighbours config matches DB setting of: " + fromDB);
        } else {
            logger.warn("CreateNeighbours config " + fromConfig + " does not match DB setting of: " + fromDB +
                    " Might be due to partial DB creation for some tests");
        }
        return !matched;
    }

}
