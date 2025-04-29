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

    private final static Map<String, TFGMRouteNames> theMap;

    static {
        theMap = new HashMap<>();
        for(TFGMRouteNames name : values()) {
            theMap.put(name.shortName, name);
        }
    }

    private final String shortName;

    TFGMRouteNames(String shortName) {
        this.shortName = shortName;
    }

    public static TFGMRouteNames parse(String text) {
        if (!theMap.containsKey(text)) {
            throw new RuntimeException("Missing route name " + text);
        }
        return theMap.get(text);
    }

    public String getShortName() {
        return shortName;
    }
}
