package com.tramchester.graph;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.facade.ImmutableGraphRelationship;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import java.util.*;
import java.util.stream.Collectors;

public enum TransportRelationshipTypes implements RelationshipType {
    TRAM_GOES_TO,
    BUS_GOES_TO,
    TRAIN_GOES_TO,
    FERRY_GOES_TO,
    SUBWAY_GOES_TO,

    // journey planning
    BOARD,
    DEPART,
    INTERCHANGE_BOARD,
    INTERCHANGE_DEPART,
    DIVERSION_DEPART, // might depart here as diversion from this station is in place
    WALKS_TO_STATION,       // WALKS_TO and _FROM added (and then removed) to support journeys involving a walk
    WALKS_FROM_STATION,
    ENTER_PLATFORM,
    LEAVE_PLATFORM,
    TO_SERVICE,
    TO_HOUR,
    TO_MINUTE,

    // between grouped stations to/from contained stations
    GROUPED_TO_PARENT,
    GROUPED_TO_CHILD,
    GROUPED_TO_GROUPED,

    // routes, allow faster initial estimation of cost and 'hop' counts while traversing minimal number of nodes
    ON_ROUTE,  // links route stations on same route
    ROUTE_TO_STATION, // link stations and routes irrespective of whether have platforms or not
    STATION_TO_ROUTE,

    DIVERSION, // temporary link of some kind, usually a walk, only specific dates
    NEIGHBOUR, // stations within N meters, see also Neighbours.DIFF_MODES_ONLY
    LINKED; // capture how stations are linked together, added during graph build, aids debug, visualisation, etc

    private static final TransportRelationshipTypes[] forPlanning;

    static {
        final EnumSet<TransportRelationshipTypes> values = EnumSet.allOf(TransportRelationshipTypes.class);
        values.remove(ON_ROUTE); // not used for traversals
        forPlanning = new TransportRelationshipTypes[values.size()];
        values.toArray(forPlanning);
    }

    public static final EnumSet<TransportRelationshipTypes> NoCost = EnumSet.of(TO_HOUR,TO_MINUTE, TO_SERVICE);
    private static final EnumSet<TransportRelationshipTypes> HasTripId = EnumSet.of(TRAM_GOES_TO, TRAIN_GOES_TO, BUS_GOES_TO,
            FERRY_GOES_TO, SUBWAY_GOES_TO, TO_MINUTE);
    private static final EnumSet<TransportRelationshipTypes> GoesTo = EnumSet.of(TRAM_GOES_TO, BUS_GOES_TO, FERRY_GOES_TO,
            TRAIN_GOES_TO, SUBWAY_GOES_TO);

    public static TransportRelationshipTypes[] forPlanning() {
        return forPlanning;
    }

    public static TransportRelationshipTypes forMode(final TransportMode transportMode) {
        return switch (transportMode) {
            case Train, RailReplacementBus -> TRAIN_GOES_TO;
            case Bus -> BUS_GOES_TO;
            case Tram -> TRAM_GOES_TO;
            case Ferry, Ship -> FERRY_GOES_TO;
            case Subway -> SUBWAY_GOES_TO;
            default -> throw new RuntimeException("Unexpected travel mode " + transportMode);
        };
    }

    public static boolean hasCost(final TransportRelationshipTypes relationshipType) {
        return !NoCost.contains(relationshipType);
    }

    public static boolean hasTripId(final TransportRelationshipTypes relationshipType) {
        return HasTripId.contains(relationshipType);
    }

    public static boolean goesTo(final ImmutableGraphRelationship relationship) {
        return GoesTo.contains(relationship.getType());
    }

    public static TransportRelationshipTypes from(final Relationship relationship) {
        return valueOf(relationship.getType().name());
    }

    public static TransportRelationshipTypes[] forModes(final EnumSet<TransportMode> transportModes) {
        final Set<TransportRelationshipTypes> unique = transportModes.stream().
                map(TransportRelationshipTypes::forMode).collect(Collectors.toSet());

        final TransportRelationshipTypes[] results = new TransportRelationshipTypes[unique.size()];
        int index = 0;
        for (final TransportRelationshipTypes type: unique) {
            results[index] = type;
            index++;
        }
        return results;
    }


}

