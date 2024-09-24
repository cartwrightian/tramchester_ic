package com.tramchester.livedata.tfgm;

// Live data lines are not same as timetable routes, see also Mapper
public enum OverheadDisplayLines {
    Altrincham("Altrincham"),
    Airport("Airport"),
    Bury("Bury"),
    Eccles("Eccles"),
    EastManchester("East Manchester"),
    OldhamAndRochdale("Oldham & Rochdale"),
    SouthManchester("South Manchester"),
    TraffordPark("Trafford Park"),
    UnknownLine("Unknown");

    private final String name;

    OverheadDisplayLines(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
