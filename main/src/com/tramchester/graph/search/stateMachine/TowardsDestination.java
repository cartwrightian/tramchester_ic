package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationCollectionSingleton;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.MixedLocationSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.core.*;
import com.tramchester.graph.core.neo4j.ResourceIterableEnhanced;

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

    public ResourceIterableEnhanced<ImmutableGraphRelationship> fromRouteStation(final GraphTransaction txn, final GraphNode node) {
        final Stream<ImmutableGraphRelationship> relationships = node.getRelationships(txn, GraphDirection.Outgoing, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART);
        return getTowardsDestination(relationships);
    }

    public ResourceIterableEnhanced<ImmutableGraphRelationship> fromPlatform(final GraphTransaction txn, final GraphNode node) {
        return getTowardsDestination(node.getRelationships(txn, GraphDirection.Outgoing, LEAVE_PLATFORM));
    }

    public ResourceIterableEnhanced<ImmutableGraphRelationship> fromStation(final GraphTransaction txn, final GraphNode node) {
        return getTowardsDestination(node.getRelationships(txn, GraphDirection.Outgoing, GROUPED_TO_PARENT));
    }

    public ResourceIterableEnhanced<ImmutableGraphRelationship> fromWalk(final GraphTransaction txn, final GraphNode node) {
        return getTowardsDestination(node.getRelationships(txn, GraphDirection.Outgoing, WALKS_TO_STATION));
    }

    private <R extends GraphRelationship> ResourceIterableEnhanced<R> getTowardsDestination(final Stream<R> outgoing) {
        final List<R> filtered = outgoing.
                filter(depart -> expanded.contains(depart.getLocationId())).
                toList();
        return ResourceIterableEnhanced.from(filtered);
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
