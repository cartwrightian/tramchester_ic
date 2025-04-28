package com.tramchester.domain.reference;

public enum TFGMTramLines {
    BusOne("Replacement Bus 1"),
    BusTwo("Replacement Bus 2"),
    Red("Red Line"),
    Pink("Pink Line"),
    Purple("Purple Line"),
    Green("Green Line"),
    Navy("Navy Line"),
    Yellow("Yellow Line"),
    Brown("Brown Line"),
    Blue("Blue Line");

    private final String shortName;

    TFGMTramLines(String shortName) {
        this.shortName = shortName;
    }

    public String getShortName() {
        return shortName;
    }
}
