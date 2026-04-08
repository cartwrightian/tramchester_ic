package com.tramchester.dataimport.rail.records;

import com.tramchester.dataimport.rail.RailRecordType;

public class UnknownRecord implements RailTimetableRecord {
    private final Line line;

    public UnknownRecord(Line line) {
        this.line = line;
    }

    @Override
    public RailRecordType getRecordType() {
        return RailRecordType.Unknown;
    }

    @Override
    public String toString() {
        return "UnknownRecord{" +
                "line=" + line +
                '}';
    }
}
