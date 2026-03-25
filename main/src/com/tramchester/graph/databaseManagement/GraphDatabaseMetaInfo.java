package com.tramchester.graph.databaseManagement;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.core.MutableGraphNode;
import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.repository.DataSourceRepository;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


@LazySingleton
public class GraphDatabaseMetaInfo {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseMetaInfo.class);

    private final ProvidesLocalNow providesLocalNow;

    @Inject
    public GraphDatabaseMetaInfo(final ProvidesLocalNow providesLocalNow) {
        this.providesLocalNow = providesLocalNow;
    }

    public boolean isNeighboursEnabled(GraphTransaction txn) {
        return hasAnyNodeWith(txn, GraphLabel.NEIGHBOURS_ENABLED);
    }

    public boolean hasVersionInfo(GraphTransaction txn) {
        return hasAnyNodeWith(txn, GraphLabel.VERSION);
    }

    private boolean hasAnyNodeWith(final GraphTransaction txn, final GraphLabel label) {
        return txn.hasAnyMatching(label);
    }

    public void setNeighboursEnabled(MutableGraphTransaction txn) {
        txn.createNode(GraphLabel.NEIGHBOURS_ENABLED);
    }

    public Map<DataSourceID, String> getVersions(final GraphTransaction txn) {
        List<GraphNode> query = txn.findNodes(GraphLabel.VERSION).toList();

        if (query.isEmpty()) {
            logger.warn("version node not found");
            return Collections.emptyMap();
        }
        final GraphNode versionNode = getSingleVersionNode(query);

        return versionNode.getStoredVersions();
    }

    public static <N extends GraphNode> N getSingleVersionNode(List<N> query) {
        if (query.size()!=1) {
            String message = "Wrong number of VERSION nodes " + query;
            logger.error(message);
            throw new RuntimeException(message);
        }

        return query.getFirst();
    }

    public void createVersionNode(MutableGraphTransaction tx, DataSourceRepository dataSourceRepository) {
        final Set<DataSourceInfo> dataSourceInfo = dataSourceRepository.getDataSourceInfo();
        logger.info("Setting version data in DB for " + dataSourceInfo);
        MutableGraphNode node = tx.createNode(GraphLabel.VERSION);
        dataSourceInfo.forEach(node::set);
        logger.info("Set version data");
    }

    public boolean boundsMatch(final GraphTransaction txn, final BoundingBox boundingBox) {
        final boolean hasBoundsNode = txn.hasAnyMatching(GraphLabel.BOUNDS);

        if (!hasBoundsNode) {
            logger.warn("No " + GraphLabel.BOUNDS + " node is present");
            return false;
        }

        final List<GraphNode> nodes = txn.findNodes(GraphLabel.BOUNDS).toList();
        if (nodes.size()!=1) {
            throw new RuntimeException("Wrong number of " + GraphLabel.BOUNDS + " nodes: " + nodes.size());
        }

        final GraphNode node = nodes.getFirst();

        final BoundingBox fromNode = node.getBounds();

        final boolean match = boundingBox.equals(fromNode);
        if (!match) {
            logger.warn("Mismatch on bounds, expected " + boundingBox + " and got " + fromNode + " from DB");
        }
        return match;
    }

    public void setBounds(final MutableGraphTransaction transaction, final BoundingBox bounds) {
        boolean hasBoundsNode = transaction.hasAnyMatching(GraphLabel.BOUNDS);

        if (hasBoundsNode) {
            // todo could this legit happen??
            throw new RuntimeException("Bounds node already present");
        }

        logger.info("Set " + GraphLabel.BOUNDS + " to " + bounds);
        final MutableGraphNode node = transaction.createNode(GraphLabel.BOUNDS);

        node.setBounds(bounds);
    }

    public ZonedDateTime getTimestamp(final GraphTransaction txn) {
        List<GraphNode> query = txn.findNodes(GraphLabel.VERSION).toList();
        final GraphNode versionNode = getSingleVersionNode(query);

        return getTimestampFor(versionNode);
    }

    public static @NotNull ZonedDateTime getTimestampFor(final GraphNode versionNode) {
        TramTime time = versionNode.getTime();
        TramDate date = versionNode.getStartDate();

        return ZonedDateTime.of(date.toLocalDate(), time.asLocalTime(), TramchesterConfig.TimeZoneId);
    }

    public void setTimestamp(MutableGraphTransaction txn) {
        List<MutableGraphNode> query = txn.findNodesMutable(GraphLabel.VERSION).toList();
        final MutableGraphNode versionNode = getSingleVersionNode(query);

        ZonedDateTime utc = providesLocalNow.getZoneDateTimeUTC();

        TramTime time = TramTime.of(utc.getHour(), utc.getMinute());
        TramDate date = TramDate.of(utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth());

        versionNode.setTime(time);
        versionNode.setStartDate(date);
    }
}
