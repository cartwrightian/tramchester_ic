package com.tramchester.graph.databaseManagement;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.facade.ImmuableGraphNode;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;
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

    public boolean isNeighboursEnabled(MutableGraphTransaction txn) {
        return hasAnyNodeWith(txn, GraphLabel.NEIGHBOURS_ENABLED);
    }

    public boolean hasVersionInfo(MutableGraphTransaction txn) {
        return hasAnyNodeWith(txn, GraphLabel.VERSION);
    }

    private boolean hasAnyNodeWith(MutableGraphTransaction txn, GraphLabel label) {
        return txn.hasAnyMatching(label);
    }

    public void setNeighboursEnabled(MutableGraphTransaction txn) {
        txn.createNode(GraphLabel.NEIGHBOURS_ENABLED);
    }

    public Map<String, String> getVersions(MutableGraphTransaction txn) {
        Stream<ImmuableGraphNode> query = txn.findNodes(GraphLabel.VERSION);

        Map<String, String> versions = new HashMap<>();
        query.forEach(versionNode -> {
            final Map<String, Object> nodeProps = versionNode.getAllProperties();
            logger.info("Got properties for VERSION node " + nodeProps.toString());
            nodeProps.forEach((key, value) -> versions.put(key, value.toString()));
        });

        return versions;
    }

    public void createVersionNode(MutableGraphTransaction tx, DataSourceRepository dataSourceRepository) {
        Set<DataSourceInfo> dataSourceInfo = dataSourceRepository.getDataSourceInfo();
        logger.info("Setting version data in DB for " + dataSourceInfo);
        MutableGraphNode node = tx.createNode(GraphLabel.VERSION);
        dataSourceInfo.forEach(node::set);
        logger.info("Set version data");
    }
}
