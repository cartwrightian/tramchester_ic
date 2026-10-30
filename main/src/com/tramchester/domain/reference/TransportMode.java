package com.tramchester.domain.reference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.HasTransportModes;
import com.tramchester.domain.collections.ImmutableEnumSet;

import java.util.Arrays;

public enum TransportMode implements HasTransportMode {
    Bus,
    Tram,
    TramReplacementBus,
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
    public static final ImmutableEnumSet<TransportMode> TramsOnly = ImmutableEnumSet.of(Tram, TramReplacementBus);
    // TODO Include replacements
    public static final ImmutableEnumSet<TransportMode> TrainOnly = Train.singleton();
    public static final ImmutableEnumSet<TransportMode> ReplacementBus = ImmutableEnumSet.of(RailReplacementBus, TramReplacementBus);

    public static final ImmutableEnumSet<TransportMode> Vehicles = ImmutableEnumSet.from(Arrays.asList(Bus, Tram, Train,
            Ferry, Subway, RailReplacementBus, Ship, TramReplacementBus));

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
        return Arrays.stream(divided).map(TransportMode::valueOf).collect(ImmutableEnumSet.collect(TransportMode.class));
    }

    public static ImmutableEnumSet<TransportMode> noneOf() {
        return NoneOf;
    }

    public static ImmutableEnumSet<TransportMode> consolidateReplacements(final ImmutableEnumSet<TransportMode> transportModes) {
        return transportModes.stream().map(TransportMode::mapReplacements).collect(ImmutableEnumSet.collect(TransportMode.class));
    }

    public static TransportMode mapReplacements(final TransportMode item) {
        return switch (item) {
            case RailReplacementBus -> Train;
            case TramReplacementBus -> Tram;
            case Bus, Tram, Train, Walk, Ferry, Subway, Ship, Connect, NotSet, Unknown -> item;
        };
    }

    public static boolean isReplacementBus(TransportMode transportMode) {
        return ReplacementBus.contains(transportMode);
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
