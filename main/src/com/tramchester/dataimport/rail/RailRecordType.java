package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.Line;

import java.util.HashMap;

public enum RailRecordType {
    TiplocInsert("TI"),
    BasicSchedule("BS"),
    BasicScheduleExtra("BX"),
    ChangesEnRoute("CR"),
    IntermediateLocation("LI"),
    OriginLocation("LO"),
    TerminatingLocation("LT"),
    Header("HD"),
    Association("AA"),
    Trailer("ZZ"),
    //
    Skipped("00"),
    Unknown("99");

    private final Line code;

    private static final HashMap<Line, RailRecordType> map = new HashMap<>();

    static {
        for(final RailRecordType recordType : RailRecordType.values()) {
            map.put(recordType.code, recordType);
        }
    }

    RailRecordType(final String code) {
        this.code = Line.of(code);
    }

    public static RailRecordType parse(final Line code) {
        if (map.containsKey(code)) {
            return map.get(code);
        }
        return Unknown;
    }
}
