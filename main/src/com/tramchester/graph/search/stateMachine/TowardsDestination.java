package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationCollectionSingleton;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.MixedLocationSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.facade.neo4j.GraphTransactionNeo4J;
import com.tramchester.graph.facade.neo4j.ImmutableGraphRelationship;

import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;

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

    public FilterByDestinations<ImmutableGraphRelationship> fromRouteStation(final GraphTransactionNeo4J txn, final GraphNode node) {
        final Stream<ImmutableGraphRelationship> relationships = node.getRelationships(txn, GraphDirection.Outgoing, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART);
        return getTowardsDestination(relationships);
    }

    public FilterByDestinations<ImmutableGraphRelationship> fromPlatform(final GraphTransactionNeo4J txn, final GraphNode node) {
        return getTowardsDestination(node.getRelationships(txn, GraphDirection.Outgoing, LEAVE_PLATFORM));
    }

    public FilterByDestinations<ImmutableGraphRelationship> fromStation(final GraphTransactionNeo4J txn, final GraphNode node) {
        return getTowardsDestination(node.getRelationships(txn, GraphDirection.Outgoing, GROUPED_TO_PARENT));
    }

    public FilterByDestinations<ImmutableGraphRelationship> fromWalk(final GraphTransactionNeo4J txn, final GraphNode node) {
        return getTowardsDestination(node.getRelationships(txn, GraphDirection.Outgoing, WALKS_TO_STATION));
    }

    private <R extends GraphRelationship> FilterByDestinations<R> getTowardsDestination(final Stream<R> outgoing) {
        final List<R> filtered = outgoing.
                filter(depart -> expanded.contains(depart.getLocationId())).
                toList();
        return FilterByDestinations.from(filtered);
    }

//    public LocationId<?> getLocationIdFor(final GraphRelationship depart) {
//        return depart.getLocationId();
////        final TransportRelationshipTypes departType = depart.getType();
////        if (HAS_STATION_ID.contains(departType)) {
////            return new LocationId<>(depart.getStationId());
////        } else if (departType==GROUPED_TO_PARENT) {
////            return new LocationId<>(depart.getStationGroupId());
////        } else {
////            throw new RuntimeException("Unsupported relationship type " + departType);
////        }
//    }
}
