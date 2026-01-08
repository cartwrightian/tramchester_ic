package com.tramchester.graph.search;

import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.core.MutableGraphNode;
import com.tramchester.graph.core.MutableGraphRelationship;
import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.tramchester.graph.reference.TransportRelationshipTypes.WALKS_FROM_STATION;
import static com.tramchester.graph.reference.TransportRelationshipTypes.WALKS_TO_STATION;
import static java.lang.String.format;

public class WalkNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(WalkNodesAndRelationships.class);

    private final MutableGraphTransaction txn;
    private final List<MutableGraphRelationship> relationships;
    private final List<MutableGraphNode> nodes;

    public WalkNodesAndRelationships(final MutableGraphTransaction txn) {
        this.txn = txn;
        this.relationships = new ArrayList<>();
        this.nodes = new ArrayList<>();
    }

    public void delete() {
        // cache is updated by the delete methods
        relationships.forEach(relationship -> relationship.delete(txn));
        nodes.forEach(node -> node.delete(txn));
        logger.info("Removed added walks and walk node(s)");
    }

    public void addAll(List<MutableGraphRelationship> relationshipList) {
        relationships.addAll(relationshipList);
    }

    public MutableGraphNode createWalkingNode(Location<?> location, JourneyRequest journeyRequest) {
        final MutableGraphNode walkingNode = createWalkingNode(txn, location.getLatLong(), journeyRequest.getUid());
        nodes.add(walkingNode);
        return walkingNode;
    }

    public void createWalksToStart(final MutableGraphNode node, final Set<StationWalk> walks) {
        createWalkRelationships(node, walks, WALKS_TO_STATION);
    }

    public void createWalksToDest(final MutableGraphNode node, final Set<StationWalk> walks) {
        createWalkRelationships(node, walks, WALKS_FROM_STATION);
    }

    private void createWalkRelationships(final MutableGraphNode node, final Set<StationWalk> walks, final TransportRelationshipTypes direction) {
        List<MutableGraphRelationship> addedRelationships = new ArrayList<>();
        walks.forEach(stationWalk -> addedRelationships.add(createWalkRelationship(node, stationWalk, direction)));
        relationships.addAll(addedRelationships);
    }

    private MutableGraphRelationship createWalkRelationship(final MutableGraphNode walkNode, final StationWalk stationWalk,
                                                            final TransportRelationshipTypes direction) {
        final Station walkStation = stationWalk.getStation();
        final TramDuration cost = stationWalk.getCost();

        final MutableGraphRelationship walkingRelationship;
        final MutableGraphNode stationNode = txn.findNodeMutable(walkStation);
        if (stationNode == null) {
            throw new RuntimeException("Could not find node for " + walkStation);
        }
        if (!stationNode.hasLabel(GraphLabel.STATION)) {
            throw new RuntimeException("Not a STATION node " + stationNode);
        }

        if (direction == WALKS_FROM_STATION) {
            walkingRelationship = stationNode.createRelationshipTo(txn, walkNode, direction);
            logger.info(format("Add %s relationship %s (%s) to node %s cost %s",
                    direction, walkStation.getId(), walkStation.getName(), walkNode.getId(), cost));
        } else if (direction == WALKS_TO_STATION) {
            walkingRelationship = walkNode.createRelationshipTo(txn, stationNode, direction);
            logger.info(format("Add %s relationship between node %s to %s (%s) cost %s",
                    direction, walkNode.getId(), walkStation.getId(), walkStation.getName(), cost));
        } else {
            throw new RuntimeException("Unknown direction " + direction);
        }

        walkingRelationship.setCost(cost);
        walkingRelationship.set(walkStation);
        return walkingRelationship;
    }

    private MutableGraphNode createWalkingNode(final MutableGraphTransaction txn, final LatLong origin, final UUID uniqueId) {
        final MutableGraphNode startOfWalkNode = txn.createNode(GraphLabel.QUERY_NODE);
        startOfWalkNode.setLatLong(origin);
        startOfWalkNode.setWalkId(origin, uniqueId);
        logger.info(format("Added walking node at %s as %s", origin, startOfWalkNode));
        return startOfWalkNode;
    }

}
