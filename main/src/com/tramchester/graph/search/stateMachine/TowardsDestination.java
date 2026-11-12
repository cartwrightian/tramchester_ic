package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationCollectionSingleton;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.MixedLocationSet;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.collections.IterableWithEmptyCheck;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.core.GraphDirection.Outgoing;
import static com.tramchester.graph.reference.TransportRelationshipTypes.*;

public class TowardsDestination {
    private static final Logger logger = LoggerFactory.getLogger(TowardsDestination.class);

    private static final ImmutableEnumSet<TransportRelationshipTypes> departs = ImmutableEnumSet.of(DEPART,
            INTERCHANGE_DEPART, DIVERSION_DEPART);

    private final LocationCollection destinations;
    private final GraphTransaction txn;

    private Set<LocationId<Station>> neighbours;
    private boolean started;
    private LocationCollection expanded;

    public TowardsDestination(final Location<?> destination, GraphTransaction txn) {
        this(LocationCollectionSingleton.of(destination), txn);
    }

    public TowardsDestination(final LocationCollection destinations, final GraphTransaction txn) {
        this.destinations = destinations;
        this.txn = txn;
        started = false;
    }

    public void start() {
        this.expanded = expand(destinations);
        includeNeighbours();
        started = true;
    }

    private void includeNeighbours() {
        final Set<GraphNode> stationNodes = expanded.locationStream().
                filter(location -> location.getLocationType() == LocationType.Station).
                map(txn::findNode).
                filter(Objects::nonNull).
                collect(Collectors.toSet());

        neighbours = stationNodes.stream().
                filter(node -> node.hasRelationship(txn, Outgoing, NEIGHBOUR)).
                flatMap(node -> node.getRelationships(txn, Outgoing, NEIGHBOUR)).
                map(GraphRelationship::getStationId).
                map(LocationId::wrap).
                collect(Collectors.toSet());

        if (neighbours.isEmpty()) {
            logger.info("No neighbours added");
        } else {
            logger.info("Added %s neighbours as destinations ".formatted(neighbours.size()));
        }
    }

    private LocationCollection expand(final LocationCollection destinations) {
        final int initial = destinations.size();
        final LocationSet<Station> contained = destinations.locationStream().
                filter(Location::containsOthers).
                flatMap(location -> location.getAllContained().stream()).
                collect(LocationSet.stationCollector());
        final MixedLocationSet locationSet = new MixedLocationSet();
        locationSet.addAll(destinations);
        locationSet.addAll(contained);

        if (locationSet.size()!=initial) {
            logger.info("Expanded from %s to %s".formatted(initial, locationSet.size()));
        }
        return locationSet;
    }

    public IterableWithEmptyCheck<GraphRelationship> fromRouteStation(final GraphNode node) {
        final Stream<GraphRelationship> relationships = node.getRelationships(txn, Outgoing, departs);
        return getTowardsDestination(txn, relationships);
    }

    public IterableWithEmptyCheck<GraphRelationship> fromPlatform(final GraphNode node) {
        return getTowardsDestination(txn, node.getRelationships(txn, Outgoing, LEAVE_PLATFORM));
    }

    public IterableWithEmptyCheck<GraphRelationship> fromStation(final GraphNode node) {
        return getTowardsDestination(txn, node.getRelationships(txn, Outgoing,
                ImmutableEnumSet.of(NEIGHBOUR, GROUPED_TO_PARENT)));
    }

    public IterableWithEmptyCheck<GraphRelationship> fromWalk(final GraphNode node) {
        return getTowardsDestination(txn, node.getRelationships(txn, Outgoing, WALKS_TO_STATION));
    }

    private <R extends GraphRelationship> IterableWithEmptyCheck<R> getTowardsDestination(final GraphTransaction txn, final Stream<R> outgoing) {
        if (!started) {
            String message = "Not started";
            logger.error(message);
            throw new RuntimeException(message);
        }
        final List<R> filtered = outgoing.
                filter(this::isContains).
                toList();
        return IterableWithEmptyCheck.from(filtered);
    }

    private boolean isContains(final GraphRelationship depart) {
        final LocationId<?> locationId = depart.getLocationId(txn);
        return expanded.contains(locationId) || neighbours.contains(locationId);
    }

}
