package com.tramchester.graph.reference;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;

public enum GraphLabel { //implements Label {
    GROUPED,  // grouped station node
    ROUTE_STATION,
    STATION, // any station node must have this label
    HAS_PLATFORMS, // label added to stations iff have platforms
    INTERCHANGE, // label added to route stations if they are interchanges
    // transport modes - added to route stations
    TRAM,
    BUS,
    TRAIN,
    FERRY, // aka a port
    SUBWAY,

    HAS_DIVERSION, // impacted by a diversion

    PLATFORM,
    QUERY_NODE, // created to support journeys walking from arbitrary location
    SERVICE,
    HOUR,
    MINUTE,
    // meta labels for versions and feature flags
    VERSION,
    NEIGHBOURS_ENABLED,
    BOUNDS,
    WALK_FOR_CLOSED_ENABLED,
    TEMP_WALKS_ADDED,
    COMPOSITES_ADDED;

    public static final EnumSet<GraphLabel> TransportModesLabels = EnumSet.of(TRAM, BUS, TRAIN, FERRY, SUBWAY);

    public static final ImmutableEnumSet<GraphLabel> CoreDomain = ImmutableEnumSet.copyOf(
            EnumSet.of(STATION, ROUTE_STATION, PLATFORM, SERVICE, MINUTE));

    public static final ImmutableEnumSet<GraphLabel> NoneOf = ImmutableEnumSet.noneOf(GraphLabel.class);

    private final ImmutableEnumSet<GraphLabel> singleton;

    GraphLabel() {
        singleton = ImmutableEnumSet.of(this);
    }

    public static GraphLabel forMode(final TransportMode mode) {
        return switch (mode) {
            case Tram -> TRAM;
            case Bus -> BUS;
            case Train, RailReplacementBus -> TRAIN;
            case Ferry, Ship -> FERRY;
            case Subway -> SUBWAY;
            case Walk -> QUERY_NODE; // TODO This is inconsistent!
            default -> throw new RuntimeException("Unsupported mode " + mode);
        };
    }

    public static ImmutableEnumSet<GraphLabel> forModes(final ImmutableEnumSet<TransportMode> modes) {
        return modes.convertTo(GraphLabel.class, GraphLabel::forMode);
    }

    public static GraphLabel from(final String name) {
        return valueOf(name);
    }

    public ImmutableEnumSet<GraphLabel> addTo(ImmutableEnumSet<GraphLabel> labels) {
        final EnumSet<GraphLabel> updated = ImmutableEnumSet.createEnumSet(labels);
        updated.add(this);
        return ImmutableEnumSet.copyOf(updated);
    }

    public ImmutableEnumSet<GraphLabel> singleton() {
        return singleton;
    }
}
