package com.tramchester.graph.databaseManagement;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.geo.BoundingBox;
import com.tramchester.graph.facade.ImmutableGraphNode;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransactionNeo4J;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.repository.DataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;


@LazySingleton
public class GraphDatabaseMetaInfo {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseMetaInfo.class);

    public boolean isNeighboursEnabled(MutableGraphTransactionNeo4J txn) {
        return hasAnyNodeWith(txn, GraphLabel.NEIGHBOURS_ENABLED);
    }

    public boolean hasVersionInfo(MutableGraphTransactionNeo4J txn) {
        return hasAnyNodeWith(txn, GraphLabel.VERSION);
    }

    private boolean hasAnyNodeWith(MutableGraphTransactionNeo4J txn, GraphLabel label) {
        return txn.hasAnyMatching(label);
    }

    public void setNeighboursEnabled(MutableGraphTransactionNeo4J txn) {
        txn.createNode(GraphLabel.NEIGHBOURS_ENABLED);
    }

    public Map<String, String> getVersions(final MutableGraphTransactionNeo4J txn) {
        Stream<ImmutableGraphNode> query = txn.findNodes(GraphLabel.VERSION);

        Map<String, String> versions = new HashMap<>();
        query.forEach(versionNode -> {
            final Map<String, Object> nodeProps = versionNode.getAllProperties();
            logger.info("Got properties for VERSION node " + nodeProps.toString());
            nodeProps.forEach((key, value) -> versions.put(key, value.toString()));
        });

        return versions;
    }

    public void createVersionNode(MutableGraphTransactionNeo4J tx, DataSourceRepository dataSourceRepository) {
        Set<DataSourceInfo> dataSourceInfo = dataSourceRepository.getDataSourceInfo();
        logger.info("Setting version data in DB for " + dataSourceInfo);
        MutableGraphNode node = tx.createNode(GraphLabel.VERSION);
        dataSourceInfo.forEach(node::set);
        logger.info("Set version data");
    }

    public boolean boundsMatch(MutableGraphTransactionNeo4J txn, final BoundingBox boundingBox) {
        final boolean hasBoundsNode = txn.hasAnyMatching(GraphLabel.BOUNDS);

        if (!hasBoundsNode) {
            logger.warn("No " + GraphLabel.BOUNDS + " node is present");
            return false;
        }

        final List<ImmutableGraphNode> nodes = txn.findNodes(GraphLabel.BOUNDS).toList();
        if (nodes.size()!=1) {
            throw new RuntimeException("Wrong number of " + GraphLabel.BOUNDS + " nodes: " + nodes.size());
        }

        final ImmutableGraphNode node = nodes.getFirst();

        final BoundingBox fromNode = node.getBounds();

        final boolean match = boundingBox.equals(fromNode);
        if (!match) {
            logger.warn("Mismatch on bounds, expected " + boundingBox + " and got " + fromNode + " from DB");
        }
        return match;
    }

    public void setBounds(final MutableGraphTransactionNeo4J transaction, final BoundingBox bounds) {
        boolean hasBoundsNode = transaction.hasAnyMatching(GraphLabel.BOUNDS);

        if (hasBoundsNode) {
            // todo could this legit happen??
            throw new RuntimeException("Bounds node already present");
        }

        logger.info("Set " + GraphLabel.BOUNDS + " to " + bounds);
        final MutableGraphNode node = transaction.createNode(GraphLabel.BOUNDS);

        node.setBounds(bounds);
    }
}
