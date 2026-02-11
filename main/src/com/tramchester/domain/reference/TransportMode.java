package com.tramchester.domain.reference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.HasTransportModes;
import com.tramchester.domain.collections.ImmutableEnumSet;

import java.util.*;
import java.util.stream.Collectors;

public enum TransportMode implements HasTransportMode {
    Bus((short)1),
    Tram((short)2),
    Train((short)3),
    Walk((short)4),
    Ferry((short)5),
    Subway((short)6),
    RailReplacementBus((short)7),
    Ship((short)8),

    Connect((short)52),

    NotSet((short)53),

    Unknown((short)999);

    public static final ImmutableEnumSet<TransportMode> WalkOnly = ImmutableEnumSet.of(Walk);
    public static final ImmutableEnumSet<TransportMode> TramsOnly = ImmutableEnumSet.of(Tram);

    private static final Map<Short, TransportMode> index;

    static {
        index = new HashMap<>();
        for(TransportMode mode : EnumSet.allOf(TransportMode.class)) {
            index.put(mode.graphId, mode);
        }
    }

    @JsonIgnore
    private final short graphId;

    TransportMode(final short graphId) {
        this.graphId = graphId;
    }

    public static boolean isTram(final HasTransportMode item) {
        return item.getTransportMode().equals(TransportMode.Tram);
    }

    public static boolean isTram(final HasTransportModes hasModes) {
        return hasModes.getTransportModes().contains(TransportMode.Tram);
    }

    public static TransportMode fromNumber(final short number) {
        return index.get(number);
    }

    public static ImmutableEnumSet<TransportMode> fromNumbers(final short[] numbers) {
        final Set<TransportMode> result = new HashSet<>();
        for (final short value : numbers) {
            result.add(index.get(value));
        }
        return ImmutableEnumSet.copyOf(result);
    }

    public static boolean anyIntersection(final ImmutableEnumSet<TransportMode> modesA, final ImmutableEnumSet<TransportMode> modesB) {
        return modesA.anyIntersectionWith(modesB);
    }

    public static boolean anyIntersection(final Set<TransportMode> modesA, final ImmutableEnumSet<TransportMode> modesB) {
        return modesB.anyIntersectionWith(modesA);
    }

    public static ImmutableEnumSet<TransportMode> parseCSV(final String csv) {
        final String[] divided = csv.split(",");
        return ImmutableEnumSet.copyOf(Arrays.stream(divided).map(TransportMode::valueOf).collect(Collectors.toSet()));
    }

    @JsonIgnore
    @Override
    public TransportMode getTransportMode() {
        return this;
    }

    public short getNumber() {
        return graphId;
    }
}
