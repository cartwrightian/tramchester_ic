package com.tramchester.domain.reference;

import com.tramchester.domain.collections.ImmutableEnumSet;

import java.util.HashMap;
import java.util.Map;

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
    //replacementBus("98"),
    tramReplacementBus("81"),
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
    private final ImmutableEnumSet<GTFSTransportationType> singleton;

    GTFSTransportationType(String theText) {
        singleton = ImmutableEnumSet.of(this);
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

    public ImmutableEnumSet<GTFSTransportationType> singleton() {
        return singleton;
    }

    public static TransportMode toTransportMode(GTFSTransportationType transportationType) {
        return switch (transportationType) {
            case tram -> TransportMode.Tram;
            case bus -> TransportMode.Bus;
            case train -> TransportMode.Train;
            case ferry -> TransportMode.Ferry;
            case subway -> TransportMode.Subway;
            case tramReplacementBus -> TransportMode.TramReplacementBus;
            case cableTram, aerialLift, funicular, trolleyBus, monorail, unknown ->
                    throw new RuntimeException("Unexpected route type (check config?) " + transportationType);
        };
    }

    public static ImmutableEnumSet<TransportMode> toTransportMode(ImmutableEnumSet<GTFSTransportationType> gtfsTransportationTypes) {
        return gtfsTransportationTypes.stream().
                map(GTFSTransportationType::toTransportMode).
                collect(ImmutableEnumSet.collect(TransportMode.class));
    }
}
