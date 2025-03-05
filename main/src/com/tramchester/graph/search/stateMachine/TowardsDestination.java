package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.MixedLocationSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.facade.ImmutableGraphRelationship;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class TowardsDestination {

    private static final EnumSet<TransportRelationshipTypes> HAS_STATION_ID = EnumSet.of(LEAVE_PLATFORM, INTERCHANGE_DEPART,
            DEPART, WALKS_TO_STATION, DIVERSION_DEPART);

    private final LocationCollection expanded;

    public TowardsDestination(final Location<?> destination) {
        this(MixedLocationSet.singleton(destination));
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

    public FilterByDestinations<ImmutableGraphRelationship> fromRouteStation(final GraphTransaction txn, final GraphNode node) {
        final Stream<ImmutableGraphRelationship> relationships = node.getRelationships(txn, OUTGOING, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART);
        return getTowardsDestination(relationships);
    }

    public FilterByDestinations<ImmutableGraphRelationship> fromPlatform(final GraphTransaction txn, final GraphNode node) {
        return getTowardsDestination(node.getRelationships(txn, OUTGOING, LEAVE_PLATFORM));
    }

    public FilterByDestinations<ImmutableGraphRelationship> fromStation(final GraphTransaction txn, final GraphNode node) {
        return getTowardsDestination(node.getRelationships(txn, OUTGOING, GROUPED_TO_PARENT));
    }

    public FilterByDestinations<ImmutableGraphRelationship> fromWalk(final GraphTransaction txn, final GraphNode node) {
        return getTowardsDestination(node.getRelationships(txn, OUTGOING, WALKS_TO_STATION));
    }

    private <R extends GraphRelationship> FilterByDestinations<R> getTowardsDestination(final Stream<R> outgoing) {
        final List<R> filtered = outgoing.
                filter(depart -> expanded.contains(getLocationIdFor(depart))).
                toList();
        return FilterByDestinations.from(filtered);
    }

    public LocationId<?> getLocationIdFor(final GraphRelationship depart) {
        final TransportRelationshipTypes departType = depart.getType();
        if (HAS_STATION_ID.contains(departType)) {
            return new LocationId<>(depart.getStationId());
        } else if (departType==GROUPED_TO_PARENT) {
            return new LocationId<>(depart.getStationGroupId());
        } else {
            throw new RuntimeException("Unsupported relationship type " + departType);
        }
    }
}
