package com.tramchester.domain.reference;

import java.util.HashMap;
import java.util.Map;

public enum TFGMRouteNames {

    RochdaleOldham("Replacement Bus Rochhdale - Oldham", true),
    FreeholdRochdale("Replacement Bus Freehold - Rochdale", true),
    PiccadillyAltrincham("Replacement Bus Piccadilly - Altrincham", true),
    VictoriaPiccadilly("Replacement Bus Victoria - Piccadilly", true),

    Red("Red Line", false),
    Pink("Pink Line", false),
    Purple("Purple Line", false),
    Green("Green Line", false),
    Navy("Navy Line", false),
    Yellow("Yellow Line", false),
    Blue("Blue Line", false);

    public boolean isReplacementBus() {
        return replacementBus;
    }

    // this pops up in the data as a metrolink route, but is the circular bus within the city centre
    // exception will be thrown in TransportEntityFactoryForTFGM if it ever appears as a tram route


    private final static Map<String, TFGMRouteNames> routeNameMap;

    static {
        routeNameMap = new HashMap<>();
        for(TFGMRouteNames name : values()) {
            routeNameMap.put(name.shortName, name);
        }
    }

    private final String shortName;
    private final boolean replacementBus;

    TFGMRouteNames(String shortName, boolean replacementBus) {
        this.shortName = shortName;
        this.replacementBus = replacementBus;
    }

    public static TFGMRouteNames parseFromSource(final String text) {
        if (!routeNameMap.containsKey(text)) {
            throw new RuntimeException("Missing from TFGMRouteNames: '" + text + "' not found in" + routeNameMap);
        }
        return routeNameMap.get(text);
    }

    // remember - TramRouteId contains TFGMRouteNames
    public static TFGMRouteNames parseFromName(final String name) {
        try {
            return Enum.valueOf(TFGMRouteNames.class, name);
        }
        catch(IllegalArgumentException missing) {
            throw new RuntimeException("Could not parse '"+name+"'", missing);
        }
    }

    // remember - TramRouteId contains TFGMRouteNames
    public String getShortName() {
        return shortName;
    }

    public static class Constants {
        public static final String REPLACEMENT_BUS_PREFIX = "Replacement Bus";
        public static final String EXT2_IS_A_BUS = "EXT2";
    }
}
