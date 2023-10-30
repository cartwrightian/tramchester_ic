package com.tramchester.graph.databaseManagement;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.GraphNode;
import com.tramchester.graph.GraphTransaction;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.repository.DataSourceRepository;
import org.neo4j.graphdb.Node;
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
//        ResourceIterator<Node> query = txn.findNodes(label);
//        return query.stream().findAny().isPresent();
    }

    public void setNeighboursEnabled(GraphTransaction txn) {
        txn.createNode(GraphLabel.NEIGHBOURS_ENABLED);
    }

    public Map<String, String> getVersions(GraphTransaction txn) {
        Stream<Node> query = txn.findNodesOLD(GraphLabel.VERSION);

        Map<String, String> versions = new HashMap<>();
        query.forEach(versionNode -> {
            final Map<String, Object> nodeProps = versionNode.getAllProperties();
            logger.info("Got properties for VERSION node " + nodeProps.toString());
            nodeProps.forEach((key, value) -> versions.put(key, value.toString()));
        });

        return versions;
    }

    public void createVersionNode(GraphTransaction tx, DataSourceRepository dataSourceRepository) {
        logger.info("Setting version data in DB for " + dataSourceRepository);
        GraphNode node = tx.createNode(GraphLabel.VERSION);
        Set<DataSourceInfo> sourceInfo = dataSourceRepository.getDataSourceInfo();
        sourceInfo.forEach(nameAndVersion -> GraphProps.setProp(node, nameAndVersion));
    }
}
