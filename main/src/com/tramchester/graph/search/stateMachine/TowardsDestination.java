package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.MixedLocationSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationId;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphRelationship;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.TransportRelationshipTypes.DIVERSION_DEPART;

public class TowardsDestination {

    private static final EnumSet<TransportRelationshipTypes> haveStationId = EnumSet.of(LEAVE_PLATFORM, INTERCHANGE_DEPART,
            DEPART, WALKS_TO_STATION, DIVERSION_DEPART);

    private final LocationCollection destinations;

    public TowardsDestination(final LocationCollection destinations) {
        this.destinations = destinations;
    }

    public TowardsDestination(final Location<?> destination) {
        this.destinations = MixedLocationSet.singleton(destination);
    }


    public <R extends GraphRelationship> FilterByDestinations<R> getTowardsDestination(final Stream<R> outgoing) {
        final List<R> filtered = outgoing.
                filter(depart -> destinations.contains(getLocationIdFor(depart))).
                toList();
        return FilterByDestinations.from(filtered);
    }

    public LocationId getLocationIdFor(final GraphRelationship depart) {
        final TransportRelationshipTypes departType = depart.getType();
        if (haveStationId.contains(departType)) {
            return new LocationId(depart.getStationId());
        } else if (departType==GROUPED_TO_PARENT) {
            return new LocationId(depart.getStationGroupId());
        } else {
            throw new RuntimeException("Unsupported relationship type " + departType);
        }
    }

    public LocationCollection getDestinations() {
        return destinations;
    }
}
