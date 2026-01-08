package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.metrics.Timing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class MutableTransactionGraph implements Graph {
    private static final Logger logger = LoggerFactory.getLogger(MutableTransactionGraph.class);

    private final GraphCore parent;
    private final GraphCore localGraph;
    private final Set<RelationshipIdInMemory> locallyCreatedRelationships;
    private final Set<NodeIdInMemory> locallyCreatedNodes;
    private final Set<RelationshipIdInMemory> relationshipsToDelete;
    private final Set<NodeIdInMemory> notesToDelete;

    MutableTransactionGraph(final GraphCore parent, final GraphIdFactory graphIdFactory) {
        this.parent = parent;
        this.localGraph = new GraphCore(graphIdFactory);

        localGraph.start();

        locallyCreatedRelationships = new HashSet<>();
        locallyCreatedNodes = new HashSet<>();
        notesToDelete = new HashSet<>();
        relationshipsToDelete = new HashSet<>();
    }

    @Override
    public synchronized GraphNodeInMemory createNode(final EnumSet<GraphLabel> labels) {
        final GraphNodeInMemory result = localGraph.createNode(labels);
        locallyCreatedNodes.add(result.getId());
        return result;
    }

    @Override
    public synchronized GraphRelationshipInMemory createRelationship(TransportRelationshipTypes relationshipType, GraphNodeInMemory begin, GraphNodeInMemory end) {
        GraphRelationshipInMemory result = localGraph.createRelationship(relationshipType, begin, end);
        locallyCreatedRelationships.add(result.getId());
        return result;
    }

    @Override
    public synchronized void delete(final RelationshipIdInMemory id) {
        if (locallyCreatedRelationships.contains(id)) {
            localGraph.delete(id);
            locallyCreatedRelationships.remove(id);
        } else {
            if (localGraph.hasRelationshipId(id)) {
                localGraph.delete(id);
            }
            relationshipsToDelete.add(id);
        }
    }

    @Override
    public synchronized void delete(final NodeIdInMemory id) {
        if (locallyCreatedNodes.contains(id)) {
            localGraph.delete(id);
            locallyCreatedNodes.remove(id);
        } else {
            if (localGraph.hasNodeId(id)) {
                localGraph.delete(id);
            }
            notesToDelete.add(id);
        }
    }

    @Override
    public void addLabel(final NodeIdInMemory id, final GraphLabel label) {
        if (localGraph.hasNodeId(id)) {
            localGraph.addLabel(id, label);
        } else {
            copyNodeIntoLocal(id, true);
            localGraph.addLabel(id, label);
        }
    }

    private GraphNodeInMemory copyNodeIntoLocal(final NodeIdInMemory nodeId, final boolean includeRelationships) {
        if (!parent.hasNodeId(nodeId)) {
            throw new RuntimeException("Could node find node with id " + nodeId);
        }
        if (localGraph.hasNodeId(nodeId)) {
            return localGraph.getNodeMutable(nodeId);
        }
        final GraphNodeInMemory original = parent.getNodeMutable(nodeId);
        final GraphNodeInMemory result = localGraph.insertNode(original.copy(), original.getLabels());

        if (includeRelationships) {
            final Stream<RelationshipIdInMemory> relNotCopiedIn = parent.
                    getRelationshipsIdsFor(nodeId).
                    filter(relId -> !localGraph.hasRelationshipId(relId));

            relNotCopiedIn.forEach(this::copyRelationshipIntoLocal);
        }

        return result;
    }

    private GraphRelationshipInMemory copyRelationshipIntoLocal(final RelationshipIdInMemory relId) {
        if (!parent.hasRelationshipId(relId)) {
            throw new RuntimeException("Could node find relationship with id " + relId);
        }
        if (localGraph.hasRelationshipId(relId)) {
            return localGraph.getRelationship(relId);
        }

        final GraphRelationshipInMemory original = parent.getRelationship(relId);
        final GraphNodeInMemory begin = copyNodeIntoLocal(original.getStartId(), false);
        final GraphNodeInMemory end = copyNodeIntoLocal(original.getEndId(), false);
        return localGraph.insertRelationship(original.getType(), original.copy(), begin.getId(), end.getId());

    }

    @Override
    public GraphNodeInMemory getNodeMutable(final NodeIdInMemory nodeId) {
        if (localGraph.hasNodeId(nodeId)) {
            return localGraph.getNodeMutable(nodeId);
        } else {
            return copyNodeIntoLocal(nodeId, true);
        }
    }

    @Override
    public GraphRelationshipInMemory getSingleRelationshipMutable(NodeIdInMemory nodeId, GraphDirection direction, TransportRelationshipTypes transportRelationshipType) {
        // locally created node, so only check locally
        if (locallyCreatedNodes.contains(nodeId)) {
            return localGraph.getSingleRelationshipMutable(nodeId, direction, transportRelationshipType);
        }

        // have node locally, and always copy in relationships
        if (localGraph.hasNodeId(nodeId)) {
            return localGraph.getSingleRelationshipMutable(nodeId, direction, transportRelationshipType);
        }

        // not local, need to find relationship id first
        if (parent.hasNodeId(nodeId)) {
           // node present, is relationship in parent?
           final GraphRelationshipInMemory originalRelationship = parent.getSingleRelationshipMutable(nodeId, direction,
                   transportRelationshipType);

           if (originalRelationship!=null) {
               // found in parent
               final RelationshipIdInMemory originalRelationshipId = originalRelationship.getId();

               if (localGraph.hasRelationshipId(originalRelationshipId)) {
                   // already copied into local, which should mean node is also copied in
                   return localGraph.getRelationship(originalRelationshipId);
               } else {
                   // copy into local (which includes the nodes)
                   return copyRelationshipIntoLocal(originalRelationshipId);
               }
           }
        }
        // node id not found locally or in parent
        throw new RuntimeException("Could node find node (local or in parent) with id " + nodeId);
    }

    @Override
    public Stream<GraphRelationshipInMemory> findRelationshipsMutableFor(final NodeIdInMemory nodeId, final GraphDirection direction) {
        if (locallyCreatedNodes.contains(nodeId)) {
            return localGraph.findRelationshipsMutableFor(nodeId, direction);
        }

        if (localGraph.hasNodeId(nodeId)) {
            // will have copied in relationships alongside node
            return localGraph.findRelationshipsMutableFor(nodeId, direction);
        }

        if (parent.hasNodeId(nodeId)) {
            final Stream<RelationshipIdInMemory> originalRelationships = parent.findRelationshipsMutableFor(nodeId, direction)
                    .map(GraphRelationshipInMemory::getId);

            final Stream<RelationshipIdInMemory> needCopyIn = originalRelationships.
                    filter(relId -> !localGraph.hasRelationshipId(relId));

            return needCopyIn.map(this::copyRelationshipIntoLocal);
        }

        throw new RuntimeException("Did not find node " + nodeId);
    }

    @Override
    public Stream<GraphNodeInMemory> findNodesMutable(final GraphLabel graphLabel) {
        final List<NodeIdInMemory> localIds = localGraph.findNodesMutable(graphLabel).
                map(GraphNodeInMemory::getId).
                toList();
        final List<NodeIdInMemory> fromParent = parent.findNodesMutable(graphLabel).
                map(GraphNodeInMemory::getId).
                filter(node -> !localIds.contains(node)).toList();

        final Stream<GraphNodeInMemory> copiedIn = fromParent.stream().
                map(node -> copyNodeIntoLocal(node, true));
        final Stream<GraphNodeInMemory> localNodes = localIds.stream().
                map(this::getNodeMutable);
        return Stream.concat(localNodes, copiedIn);
    }

    ///  immutable
    ///

    @Override
    public Stream<GraphNode> findNodesImmutable(final GraphLabel graphLabel) {
        final List<GraphNode> local = localGraph.findNodesImmutable(graphLabel).toList();
        final Stream<GraphNode> fromParent = parent.findNodesImmutable(graphLabel).
                filter(node -> !local.contains(node));
        return Stream.concat(local.stream(), fromParent);
    }

    @Override
    public Stream<GraphNode> findNodesImmutable(GraphLabel label, GraphPropertyKey key, String value) {
        final List<GraphNode> local = localGraph.findNodesImmutable(label, key, value).toList();
        final Stream<GraphNode> fromParent = parent.findNodesImmutable(label, key, value).
                filter(node -> !local.contains(node));
        return Stream.concat(local.stream(), fromParent);
    }

    @Override
    public GraphNode getNodeImmutable(final NodeIdInMemory nodeId) {
        if (localGraph.hasNodeId(nodeId)) {
            return localGraph.getNodeImmutable(nodeId);
        }
        return parent.getNodeImmutable(nodeId);
    }

    @Override
    public Stream<GraphRelationship> findRelationships(TransportRelationshipTypes type) {
        final List<GraphRelationship> local = localGraph.findRelationships(type).toList();
        final Stream<GraphRelationship> fromParent = parent.findRelationships(type).filter(rel -> !local.contains(rel));
        return Stream.concat(local.stream(), fromParent);
    }

    @Override
    public Stream<GraphRelationship> findRelationshipsImmutableFor(final NodeIdInMemory nodeId, final GraphDirection direction) {
        final List<GraphRelationship> local = localGraph.findRelationshipsImmutableFor(nodeId, direction).toList();
        final Stream<GraphRelationship> fromParent = parent.findRelationshipsImmutableFor(nodeId, direction).
                filter(graphRelationship -> !local.contains(graphRelationship));
        return Stream.concat(local.stream(), fromParent);
    }

    @Override
    public GraphRelationship getRelationship(final RelationshipIdInMemory graphRelationshipId) {
        if (localGraph.hasRelationshipId(graphRelationshipId)) {
            return localGraph.getRelationship(graphRelationshipId);
        }
        return parent.getRelationship(graphRelationshipId);
    }

    @Override
    public long getNumberOf(final TransportRelationshipTypes relationshipType) {
        return parent.getNumberOf(relationshipType) + locallyCreatedRelationships.size();
    }

    @Override
    public synchronized void commit(final GraphTransaction owningTransaction) {
        try (Timing ignored = new Timing(logger, "commit for " + owningTransaction)) {
            parent.commitChanges(this, relationshipsToDelete, notesToDelete);
        }
    }

    @Override
    public void close(final GraphTransaction owningTransaction) {
        localGraph.stop();
        locallyCreatedNodes.clear();
        locallyCreatedRelationships.clear();
        relationshipsToDelete.clear();
        notesToDelete.clear();
    }

    @Override
    public Stream<GraphNodeInMemory> getUpdatedNodes() {
        final Set<GraphNodeInMemory> allLocalNodes = localGraph.getNodesAndEdges().getNodes();
        return allLocalNodes.stream().
                filter(node -> locallyCreatedNodes.contains(node.getId()) || node.isDirty());
    }

    @Override
    public Stream<GraphRelationshipInMemory> getUpdatedRelationships() {
        final Set<GraphRelationshipInMemory> allLocalRelationships = localGraph.getNodesAndEdges().getRelationships();
        return allLocalRelationships.stream().
                filter(rel -> locallyCreatedRelationships.contains(rel.getId()) || rel.isDirty());
    }
}
