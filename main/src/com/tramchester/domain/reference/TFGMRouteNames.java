package com.tramchester.domain.reference;

import java.util.HashMap;
import java.util.Map;

public enum TFGMRouteNames {
    BusOne("Replacement Bus 1"),
    BusTwo("Replacement Bus 2"),
    Red("Red Line"),
    Pink("Pink Line"),
    Purple("Purple Line"),
    Green("Green Line"),
    Navy("Navy Line"),
    Yellow("Yellow Line"),
    //Brown("Brown Line"),
    Blue("Blue Line");

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
            throw new RuntimeException("Missing route name " + text);
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
