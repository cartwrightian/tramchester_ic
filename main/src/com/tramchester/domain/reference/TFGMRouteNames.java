package com.tramchester.domain.reference;

import java.util.HashMap;
import java.util.Map;

public enum TFGMRouteNames {
    BusOne("Replacement Bus 1"),
    BusTwo("Replacement Bus 2"),
    BusThree("Replacement Bus 3"),
    Red("Red Line"),
    Pink("Pink Line"),
    Purple("Purple Line"),
    Green("Green Line"),
    Navy("Navy Line"),
    Yellow("Yellow Line"),
    Blue("Blue Line");

    // this pops up in the data as a metrolink route, but is the circular bus within the city centre
    // exception will be thrown in TransportEntityFactoryForTFGM if it ever appears as a tram route
    public static final String EXT2_IS_A_BUS = "EXT2";

    private final static Map<String, TFGMRouteNames> fromSource;

    static {
        fromSource = new HashMap<>();
        for(TFGMRouteNames name : values()) {
            fromSource.put(name.shortName, name);
        }
    }

    private final String shortName;

    TFGMRouteNames(String shortName) {
        this.shortName = shortName;
    }

    public static TFGMRouteNames parseFromSource(final String text) {
        if (!fromSource.containsKey(text)) {
            throw new RuntimeException("Missing from Enum: route name " + text);
        }
        return fromSource.get(text);
    }

    public static TFGMRouteNames parseFromName(final String name) {
        return Enum.valueOf(TFGMRouteNames.class, name);
    }

    public String getShortName() {
        return shortName;
    }
}
