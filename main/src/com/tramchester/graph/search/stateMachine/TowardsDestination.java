package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationCollectionSingleton;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.MixedLocationSet;
import com.tramchester.domain.collections.IterableWithEmptyCheck;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;

import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.graph.reference.TransportRelationshipTypes.*;

public class TowardsDestination {

    private final LocationCollection expanded;

    public TowardsDestination(final Location<?> destination) {
        this(LocationCollectionSingleton.of(destination));
    }

    public TowardsDestination(final LocationCollection destinations) {
        this.expanded = expand(destinations);
    }

    private LocationCollection expand(final LocationCollection destinations) {
        final LocationSet<Station> contained = destinations.locationStream().
                filter(Location::containsOthers).
                flatMap(location -> location.getAllContained().stream()).
                collect(LocationSet.stationCollector());
        final MixedLocationSet locationSet = new MixedLocationSet();
        locationSet.addAll(destinations);
        locationSet.addAll(contained);
        return locationSet;
    }

    public IterableWithEmptyCheck<GraphRelationship> fromRouteStation(final GraphTransaction txn, final GraphNode node) {
        final Stream<GraphRelationship> relationships = node.getRelationships(txn, GraphDirection.Outgoing, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART);
        return getTowardsDestination(txn, relationships);
    }

    public IterableWithEmptyCheck<GraphRelationship> fromPlatform(final GraphTransaction txn, final GraphNode node) {
        return getTowardsDestination(txn, node.getRelationships(txn, GraphDirection.Outgoing, LEAVE_PLATFORM));
    }

    public IterableWithEmptyCheck<GraphRelationship> fromStation(final GraphTransaction txn, final GraphNode node) {
        return getTowardsDestination(txn, node.getRelationships(txn, GraphDirection.Outgoing, GROUPED_TO_PARENT));
    }

    public IterableWithEmptyCheck<GraphRelationship> fromWalk(final GraphTransaction txn, final GraphNode node) {
        return getTowardsDestination(txn, node.getRelationships(txn, GraphDirection.Outgoing, WALKS_TO_STATION));
    }

    private <R extends GraphRelationship> IterableWithEmptyCheck<R> getTowardsDestination(final GraphTransaction txn, final Stream<R> outgoing) {
        final List<R> filtered = outgoing.
                filter(depart -> expanded.contains(depart.getLocationId(txn))).
                toList();
        return IterableWithEmptyCheck.from(filtered);
    }

}
