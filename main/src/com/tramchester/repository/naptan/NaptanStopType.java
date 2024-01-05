package com.tramchester.repository.naptan;

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
    private final EnumSet<TransportMode> validModes;

    private static final Map<String, NaptanStopType> map;

    static {
        map = new HashMap<>();
        for(NaptanStopType type :NaptanStopType.values()) {
            map.put(type.code, type);
        }
    }

    NaptanStopType(String code, EnumSet<TransportMode> validModes) {
        this.code = code;
        this.validModes = validModes;
    }

    public static NaptanStopType parse(String text) {
        if (map.containsKey(text)) {
            return map.get(text);
        }
        return NaptanStopType.unknown;
    }

    public static boolean isInterchange(NaptanStopType stopType) {
        return switch (stopType) {
            case busCoachTrolleyStationBay, busCoachTrolleyStationVariableBay, busCoachStationEntrance,
                    busCoachStationAccess-> true;
            default -> false;
        };
    }

    public static EnumSet<NaptanStopType> getTypesFor(EnumSet<TransportMode> transportModes) {
        Set<NaptanStopType> found = transportModes.stream().flatMap(mode -> getTypesFor(mode).stream()).collect(Collectors.toSet());
        return EnumSet.copyOf(found);
    }

    private static Set<NaptanStopType> getTypesFor(TransportMode mode) {
        return Arrays.stream(values()).filter(type -> type.validFor(mode)).collect(Collectors.toSet());
    }

    private boolean validFor(TransportMode mode) {
        return validModes.contains(mode);
    }

    private static class Constants {
        public static final EnumSet<TransportMode> NotApplicable = EnumSet.noneOf(TransportMode.class);
        public static final EnumSet<TransportMode> Buses = EnumSet.of(Bus, RailReplacementBus);
        public static final EnumSet<TransportMode> Boats = EnumSet.of(Ship, Ferry);
        public static final EnumSet<TransportMode> Rail = EnumSet.of(Train, RailReplacementBus);
        public static final EnumSet<TransportMode> TramOrMetro = EnumSet.of(Tram, Subway);
    }
}
