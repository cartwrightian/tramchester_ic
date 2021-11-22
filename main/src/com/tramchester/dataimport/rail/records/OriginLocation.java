package com.tramchester.dataimport.rail.records;

public class OriginLocation implements RailTimetableRecord {
    // 1 Record Identity 2 1-2 With the constant value ‘LO’.
    // 2 Location 8 3-10 TIPLOC + Suffix.
    // 3 Scheduled Departure Time 5 11-15
    // 4 Public Departure Time 4 16-19
    // 5 Platform 3 20-22
    // 6 Line 3 23-25
    // 7 Engineering Allowance 2 26-27
    // 8 Pathing Allowance 2 28-29
    // 9 Activity 12 30-41
    // 10 Performance Allowance 2 42-43
    // 11 Spare 37 44-80
}