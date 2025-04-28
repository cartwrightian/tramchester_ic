package com.tramchester.testSupport.reference;

public enum KnownTramLines {
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

    KnownTramLines(String shortName) {
        this.shortName = shortName;
    }

    public String getShortName() {
        return shortName;
    }
}
