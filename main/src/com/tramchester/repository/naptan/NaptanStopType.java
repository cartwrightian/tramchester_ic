package com.tramchester.repository.naptan;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.*;

public enum NaptanStopType {

    airportEntrance("AIR", Constants.NotApplicable),
    airAccessArea("GAT", Constants.NotApplicable),

    taxiRank("TXR", Constants.NotApplicable),
    sharedTaxiRank("STR", Constants.NotApplicable),
    carSetDownPickUpArea("SDA", Constants.NotApplicable),

    ferryTerminalDockEntrance("FTD", Constants.NotApplicable),
    ferryOrPortAccess("FER", Constants.NotApplicable),
    ferryOrPortBerth("FBT", Constants.Boats),

    railStationEntrance("RSE", Constants.NotApplicable),
    railAccess("RLY", Constants.Rail),
    railPlatform("RPL", Constants.Rail),

    tramMetroUndergroundEntrance("TMU", Constants.TramOrMetro),
    tramMetroUndergroundAccess("MET", Constants.TramOrMetro),
    tramMetroUndergroundPlatform("PLT", Constants.TramOrMetro),

    liftOrCableCarStationEntrance("LCE", Constants.NotApplicable),
    liftOrCableCarSetDownPickUpArea("LPL", Constants.NotApplicable),

    busCoachStationEntrance("BCE", Constants.NotApplicable),
    busCoachStationAccess("BST", Constants.NotApplicable),

    busCoachTrolleyStationBay("BCS", Constants.Buses),
    busCoachTrolleyStationVariableBay("BCQ", Constants.Buses),
    busCoachTrolleyStopOnStreet("BCT", Constants.Buses),

    busCoachPrivate("BCP", Constants.NotApplicable),
    unknown("UNKNOWN",  Constants.NotApplicable);

    private final String code;
    private final ImmutableEnumSet<TransportMode> validModes;

    private static final Map<String, NaptanStopType> map;

    static {
        map = new HashMap<>();
        for(NaptanStopType type :NaptanStopType.values()) {
            map.put(type.code, type);
        }
    }

    NaptanStopType(String code, ImmutableEnumSet<TransportMode> validModes) {
        this.code = code;
        this.validModes = validModes;
    }

    public static NaptanStopType parse(final String text) {
        if (map.containsKey(text)) {
            return map.get(text);
        }
        return NaptanStopType.unknown;
    }

    public static boolean isInterchange(final NaptanStopType stopType) {
        return switch (stopType) {
            case busCoachTrolleyStationBay, busCoachTrolleyStationVariableBay, busCoachStationEntrance,
                    busCoachStationAccess-> true;
            default -> false;
        };
    }

    public static ImmutableEnumSet<NaptanStopType> getTypesFor(final ImmutableEnumSet<TransportMode> transportModes) {
        final Set<NaptanStopType> found = transportModes.stream().
                flatMap(mode -> getTypesFor(mode).stream()).
                collect(Collectors.toSet());
        return ImmutableEnumSet.copyOf(found);
    }

    public static Set<NaptanStopType> getTypesFor(final TransportMode mode) {
        return Arrays.stream(values()).filter(type -> type.validFor(mode)).collect(Collectors.toSet());
    }

    private boolean validFor(final TransportMode mode) {
        return validModes.contains(mode);
    }

    private static class Constants {
        public static final ImmutableEnumSet<TransportMode> NotApplicable = ImmutableEnumSet.noneOf(TransportMode.class);
        public static final ImmutableEnumSet<TransportMode> Buses = ImmutableEnumSet.of(Bus, RailReplacementBus);
        public static final ImmutableEnumSet<TransportMode> Boats = ImmutableEnumSet.of(Ship, Ferry);
        public static final ImmutableEnumSet<TransportMode> Rail = ImmutableEnumSet.of(Train, RailReplacementBus);
        public static final ImmutableEnumSet<TransportMode> TramOrMetro = ImmutableEnumSet.of(Tram, Subway);
    }
}
