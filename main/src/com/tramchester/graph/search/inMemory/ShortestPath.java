package com.tramchester.graph.search.inMemory;

import com.tramchester.graph.core.*;
import com.tramchester.graph.core.inMemory.GraphPathInMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

public class ShortestPath {
    private static final Logger logger = LoggerFactory.getLogger(ShortestPath.class);

    private final GraphTransaction txn;
    private final GraphNode startNode;

    public ShortestPath(GraphTransaction txn, GraphNode startNode) {
        this.txn = txn;
        this.startNode = startNode;
    }

    public Duration findShortestPathsTo(final GraphNode destNode, final FindPathsForJourney.GraphRelationshipFilter filter) {
        final GraphPathInMemory initialPath = new GraphPathInMemory();

        final SearchState searchState = new SearchState(startNode.getId(), initialPath);

        final List<GraphPath> results = new ArrayList<>();

        while (searchState.hasNodes()) {
            final NodeSearchState nodeSearchState = searchState.getNext();
            final GraphNodeId nextId = nodeSearchState.nodeId();
            visitNodeForShortestPath(nextId, searchState, nodeSearchState.pathToHere(), results, destNode.getId(), filter);
        }

        Optional<Duration> minimum = results.stream().
                map(GraphPath::getTotalCost).
                min(Duration::compareTo);

        return minimum.orElse(FindPathsForJourney.NotVisitiedDuration);

    }

    private void visitNodeForShortestPath(final GraphNodeId currentNodeId, final SearchState searchState,
                                          final GraphPath incomingPath,
                                          final List<GraphPath> results, final GraphNodeId destNodeId,
                                          final FindPathsForJourney.GraphRelationshipFilter filter) {

        if (currentNodeId.equals(destNodeId)) {
            results.add(incomingPath);
            return;
        }

        final GraphNode currentNode = txn.getNodeById(currentNodeId);
        final GraphPathInMemory pathToHere = incomingPath.duplicateWith(txn, currentNode);
        final Duration currentCostToNode = searchState.getCurrentCost(currentNodeId); //pair.getDuration();

        final Stream<GraphRelationship> outgoing = currentNode.getRelationships(txn, GraphDirection.Outgoing).
                filter(filter::include);

        outgoing.forEach(graphRelationship -> {
            final Duration relationshipCost = graphRelationship.getCost();
            final GraphNode nextNode = graphRelationship.getEndNode(txn);
            final GraphNodeId nextNodeId = nextNode.getId();
            final Duration updatedCost = relationshipCost.plus(currentCostToNode);

            final GraphPathInMemory continuePath = pathToHere.duplicateWith(txn, graphRelationship);
            if (searchState.hasSeen(nextNodeId)) {
                final Duration currentDurationForEnd = searchState.getCurrentCost(nextNodeId);
                if (updatedCost.compareTo(currentDurationForEnd) < 0) {
                    searchState.setNewCostFor(nextNodeId, updatedCost, continuePath);
                }
            } else {
                searchState.add(nextNodeId, updatedCost, continuePath);
                //searchState.storeCostOnly(endRelationshipNodeId, newCost);
            }

        });
    }

    private static class SearchState {
        private final Map<GraphNodeId, Duration> currentCost;
        private final PriorityQueue<NodeSearchState> nodeQueue;

        public SearchState(final GraphNodeId id, final GraphPathInMemory initialPath) {
            currentCost = new HashMap<>();
            nodeQueue = new PriorityQueue<>();

            currentCost.put(id, FindPathsForJourney.NotVisitiedDuration);
            nodeQueue.add(new NodeSearchState(id, FindPathsForJourney.NotVisitiedDuration, initialPath));
        }

        public boolean hasNodes() {
            return !nodeQueue.isEmpty();
        }

        public NodeSearchState getNext() {
            return nodeQueue.remove();
        }

        public Duration getCurrentCost(final GraphNodeId nodeId) {
            if (!currentCost.containsKey(nodeId)) {
                throw new RuntimeException("Missing cost for " + nodeId);
            }
            return currentCost.get(nodeId);
        }

        public boolean hasSeen(final GraphNodeId nodeId) {
            return currentCost.containsKey(nodeId);
        }

        public void setNewCostFor(final GraphNodeId nodeId, final Duration duration, final GraphPath path) {
            synchronized (nodeQueue) {
                currentCost.put(nodeId, duration);
                final NodeSearchState updatedState = new NodeSearchState(nodeId, duration, path);
                nodeQueue.remove(updatedState);
                nodeQueue.add(updatedState);
            }
        }

        public void add(final GraphNodeId nodeId, final Duration duration, final GraphPath path) {
            synchronized (nodeQueue) {
                currentCost.put(nodeId, duration);
                final NodeSearchState nodeSearchState = new NodeSearchState(nodeId, duration, path);
                nodeQueue.add(nodeSearchState);
            }
        }
    }

    private record NodeSearchState(GraphNodeId nodeId, Duration cost,
                                   GraphPath pathToHere) implements Comparable<NodeSearchState> {

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                NodeSearchState that = (NodeSearchState) o;
                return Objects.equals(nodeId, that.nodeId);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(nodeId);
            }

        @Override
            public int compareTo(ShortestPath.NodeSearchState other) {
                return this.cost.compareTo(other.cost);
            }
        }
}
