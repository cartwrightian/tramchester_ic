package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.search.FindLinkedStations;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.core.GraphDirection.Outgoing;
import static com.tramchester.graph.reference.GraphLabel.STATION;
import static com.tramchester.graph.reference.TransportRelationshipTypes.LINKED;

@LazySingleton
public class FindLinkedStationsInMemory extends FindLinkedStations {
    private final GraphDatabaseInMemory graphDatabase;

    @Inject
    public FindLinkedStationsInMemory(GraphDatabaseInMemory graphDatabase,
                                      @SuppressWarnings("unused") StationsAndLinksGraphBuilder.Ready readyToken,
                                      StationRepository stationRepository, Geography geography) {
        super(stationRepository, geography);
        this.graphDatabase = graphDatabase;
    }

    @Override
    public Set<StationToStationConnection> findLinkedFor(final TransportMode mode) {
        final GraphLabel forMode = GraphLabel.forMode(mode);

        try (final GraphTransaction txn = graphDatabase.beginTx()) {
            final Stream<GraphNode> nodes = txn.findNodes(STATION).
                    filter(node -> node.hasLabel(forMode)).
                    filter(node -> node.hasRelationship(txn, Outgoing, LINKED));

            return nodes.flatMap(node -> node.getRelationships(txn, Outgoing, LINKED)).
                    map(this::createConnection).collect(Collectors.toSet());
        }
    }

    @Override
    public IdSet<Station> atLeastNLinkedStations(final TransportMode mode, final int threshhold) {
        final GraphLabel forMode = GraphLabel.forMode(mode);

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            final Stream<GraphNode> nodes = txn.findNodes(STATION).
                    filter(node -> node.hasLabel(forMode)).
                    filter(node -> node.hasRelationship(txn, Outgoing, LINKED)).
                    filter(node -> node.getRelationships(txn, Outgoing, LINKED).count()>=threshhold);

            return nodes.map(GraphNode::getStationId).collect(IdSet.idCollector());

        }
    }
}
