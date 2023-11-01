package com.tramchester.graph.databaseManagement;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.repository.DataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;


@LazySingleton
public class GraphDatabaseMetaInfo {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseMetaInfo.class);

    public boolean isNeighboursEnabled(GraphTransaction txn) {
        return hasAnyNodeWith(txn, GraphLabel.NEIGHBOURS_ENABLED);
    }

    public boolean hasVersionInfo(GraphTransaction txn) {
        return hasAnyNodeWith(txn, GraphLabel.VERSION);
    }

    private boolean hasAnyNodeWith(GraphTransaction txn, GraphLabel label) {
        return txn.hasAnyMatching(label);
    }

    public void setNeighboursEnabled(GraphTransaction txn) {
        txn.createNode(GraphLabel.NEIGHBOURS_ENABLED);
    }

    public Map<String, String> getVersions(GraphTransaction txn) {
        Stream<GraphNode> query = txn.findNodes(GraphLabel.VERSION);

        Map<String, String> versions = new HashMap<>();
        query.forEach(versionNode -> {
            final Map<String, Object> nodeProps = versionNode.getAllProperties();
            logger.info("Got properties for VERSION node " + nodeProps.toString());
            nodeProps.forEach((key, value) -> versions.put(key, value.toString()));
        });

        return versions;
    }

    public void createVersionNode(GraphTransaction tx, DataSourceRepository dataSourceRepository) {
        Set<DataSourceInfo> dataSourceInfo = dataSourceRepository.getDataSourceInfo();
        logger.info("Setting version data in DB for " + dataSourceInfo);
        MutableGraphNode node = tx.createNode(GraphLabel.VERSION);
        Set<DataSourceInfo> sourceInfo = dataSourceInfo;
        dataSourceInfo.forEach(nameAndVersion -> node.set(nameAndVersion)); // GraphProps.setProp(node, nameAndVersion));
        logger.info("Set version data");
    }
}
