package com.tramchester.domain.reference;

import java.util.*;

public enum GTFSTransportationType {

    tram("0"),
    subway("1"),
    train("2"),
    bus("3"),
    ferry("4"),
    cableTram("5"),
    aerialLift("6"),
    funicular("7"),
    trolleyBus("11"),
    monorail("12"),

    // NOTE: These are not official types
    replacementBus("98"),
    unknown("99");

    private static final Map<String, GTFSTransportationType> textMap;

    static {
        textMap = new HashMap<>();
        GTFSTransportationType[] valid = GTFSTransportationType.values();
        for (GTFSTransportationType value : valid) {
            textMap.put(value.getText(), value);
        }
    }

    private final String text;

    GTFSTransportationType(String theText) {
        this.text = theText;
    }

    public static GTFSTransportationType parse(String routeType) {
        if (textMap.containsKey(routeType)) {
            return textMap.get(routeType);
        }
        return unknown;
    }

    private String getText() {
        return text;
    }


    public static TransportMode toTransportMode(GTFSTransportationType transportationType) {
        return switch (transportationType) {
            case tram -> TransportMode.Tram;
            case bus -> TransportMode.Bus;
            case train -> TransportMode.Train;
            case ferry -> TransportMode.Ferry;
            case subway -> TransportMode.Subway;
            case replacementBus -> TransportMode.RailReplacementBus;
            default -> throw new RuntimeException("Unexpected route type (check config?) " + transportationType);
        };
    }

    public static EnumSet<TransportMode> toTransportMode(Set<GTFSTransportationType> gtfsTransportationTypes) {
        EnumSet<TransportMode> result = EnumSet.noneOf(TransportMode.class);
        gtfsTransportationTypes.forEach(gtfsTransportationType -> result.add(toTransportMode(gtfsTransportationType)));
        return result;
    }
}
