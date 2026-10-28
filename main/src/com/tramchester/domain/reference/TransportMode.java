package com.tramchester.domain.reference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.HasTransportModes;
import com.tramchester.domain.collections.ImmutableEnumSet;

import java.util.*;
import java.util.stream.Collectors;

public enum TransportMode implements HasTransportMode {
    Bus,
    Tram,
    Train,
    Walk,
    Ferry,
    Subway,
    RailReplacementBus,
    Ship,

    Connect,

    NotSet,

    Unknown;

    public static final ImmutableEnumSet<TransportMode> WalkOnly = Walk.singleton();
    public static final ImmutableEnumSet<TransportMode> TramsOnly = Tram.singleton();
    public static final ImmutableEnumSet<TransportMode> TrainOnly = Train.singleton();

    public static final ImmutableEnumSet<TransportMode> Vehicles = ImmutableEnumSet.from(Arrays.asList(Bus, Tram, Train,
            Ferry, Subway, RailReplacementBus, Ship));

    private static final ImmutableEnumSet<TransportMode> NoneOf = ImmutableEnumSet.noneOf(TransportMode.class);

    @JsonIgnore
    private final ImmutableEnumSet<TransportMode> singleton;

    TransportMode() {
        singleton = ImmutableEnumSet.of(this);
    }

    public static boolean isTram(final HasTransportMode item) {
        return item.getTransportMode().equals(TransportMode.Tram);
    }

    public static boolean isTram(final HasTransportModes hasModes) {
        return hasModes.getTransportModes().contains(TransportMode.Tram);
    }

    public static boolean isVehicle(final HasTransportMode hasTransportMode) {
        return Vehicles.contains(hasTransportMode.getTransportMode());
    }

    public static ImmutableEnumSet<TransportMode> parseCSV(final String csv) {
        final String[] divided = csv.split(",");
        return ImmutableEnumSet.copyOf(Arrays.stream(divided).map(TransportMode::valueOf).collect(Collectors.toSet()));
    }

    public static ImmutableEnumSet<TransportMode> noneOf() {
        return NoneOf;
    }

    @JsonIgnore
    @Override
    public TransportMode getTransportMode() {
        return this;
    }

    public ImmutableEnumSet<TransportMode> singleton() {
        return singleton;
    }
}
