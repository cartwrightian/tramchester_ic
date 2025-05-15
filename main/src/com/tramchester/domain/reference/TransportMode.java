package com.tramchester.domain.reference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.HasTransportModes;

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

    public static EnumSet<TransportMode> fromNumbers(final short[] numbers) {
        final Set<TransportMode> result = new HashSet<>();
        for (final short value : numbers) {
            result.add(index.get(value));
        }
        return EnumSet.copyOf(result);
    }

    public static boolean anyIntersection(final EnumSet<TransportMode> modesA, final EnumSet<TransportMode> modesB) {
        for (final TransportMode mode:modesA) {
            if (modesB.contains(mode)) {
                return true;
            }
        }
        return false;
        // slow
        //return !SetUtils.intersection(modesA, modesB).isEmpty();
    }

    public static EnumSet<TransportMode> parseCSV(final String csv) {
        final String[] divided = csv.split(",");
        return EnumSet.copyOf(Arrays.stream(divided).map(TransportMode::valueOf).collect(Collectors.toSet()));
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
