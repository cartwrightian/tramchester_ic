package com.tramchester.dataimport.rail.records.reference;

// https://wiki.openraildata.com/index.php?title=Activity_codes

import java.util.EnumSet;

public enum LocationActivityCode implements EnumMap.HasCodes {
    StopsToTakeUpAndSetDownPassengers("T"),
    TrainBegins("TB"),
    TrainFinishes("TF"),
    StopsToTakeUpPassengers("U"),
    StopsToSetDownPassengers("D"),
    StopsWhenRequired("R"),

    StopsToDetachVehicles("-D"),
    StopsToAttachAndDetachVehicles("-T"),
    StopsToAttachVehicles("-U"),
    StopsOrShuntsForOtherTrainsToPass("A"),
    AttachDetachAssistingLocomotive("AE"),
    ShowsAsXOnArrival("AX"),
    StopsForBankingLocomotive("BL"),
    StopsToChangeTrainCrew("C"),
    StopsForExamination("E"),
    GBPRTTDataToAdd("G"),
    NotionalActivityToPreventWTTColumnsMerge("H"),
    ToPreventWTTColumnMergeWhere3rdColumn("HH"),
    PassengerCountPoint("K"),
    TicketCollectionAndExaminationPoint("KC"),
    TicketExaminationPoint("KE"),
    TicketExaminationPointFirstClassOnly("KF"),
    SelectiveTicketExaminationPoint("KS"),
    StopsToChangeLocomotive("L"),
    StopNotAdvertised("N"),
    StopsForOtherOperatingReasons("OP"),
    TrainLocomotiveOnRear("OR"),
    PropellingBetweenPointsShown("PR"),
    StopsForReversingMoveOrDriverChangesEnds("RM"),
    StopsForLocomotiveToRunRoundTrain("RR"),
    StopsForRailwayPersonnelOnly("S"),
    ActivityRequestedForTOPSReportingPurposes("TS"),
    StopsOrPassesForTabletStaffOrToken("TW"),
    StopsForWateringOfCoaches("W"),
    PassesAnotherTrainAtCrossingPointOnASingleLine("X"),

    // additional codes added
    None("");

    private static final EnumMap<LocationActivityCode> codes  = new EnumMap<>(values());

    // TrainBegins and TrainFinishes seem to be used inconsistently,
    // i.e. TF is not always paired with T even when train does actually drop off passengers

    private static final EnumSet<LocationActivityCode> dropOffs = EnumSet.of(TrainFinishes, StopsToSetDownPassengers,
            StopsToTakeUpAndSetDownPassengers, StopsWhenRequired);

    private static final EnumSet<LocationActivityCode> pickUps = EnumSet.of(TrainBegins, StopsToTakeUpPassengers,
            StopsToTakeUpAndSetDownPassengers, StopsWhenRequired);

    private static final EnumSet<LocationActivityCode> stops = EnumSet.of(TrainBegins, TrainFinishes,
            StopsToSetDownPassengers, StopsToTakeUpPassengers,
            StopsToTakeUpAndSetDownPassengers, StopsWhenRequired);

    private final String code;

    LocationActivityCode(String code) {
        this.code = code;
    }

    public static EnumSet<LocationActivityCode> parse(final String code) {
        final String lookup = code.trim();
        if (lookup.isEmpty()) {
            return EnumSet.noneOf(LocationActivityCode.class);
        }

        return getCodesFor(lookup);
    }

    private static EnumSet<LocationActivityCode> getCodesFor(final String text) {
       final EnumSet<LocationActivityCode> result = EnumSet.noneOf(LocationActivityCode.class);

        final String[] tokens = text.split(" ");
        for (final String token : tokens) {
            result.addAll(parseToken(token));
        }
        return result;
    }

    private static EnumSet<LocationActivityCode> parseToken(final String token) {
        final EnumSet<LocationActivityCode> result = EnumSet.noneOf(LocationActivityCode.class);

        String toProcess = token;

        while (!toProcess.isEmpty()) {
            int len = Math.min(toProcess.length(), 2);
            LocationActivityCode attempt = parseSingle(toProcess.substring(0,len));
            if (attempt == None) {
                len = Math.min(toProcess.length(), 1);
                attempt = parseSingle(toProcess.substring(0, len));
            }
            if (attempt == None) {
                toProcess = toProcess.substring(2);
            } else {
                result.add(attempt);
                toProcess = toProcess.substring(attempt.code.length());
            }
        }
        return result;

    }

    private static LocationActivityCode parseSingle(final String text) {
        if (codes.containsCode(text)) {
            return codes.get(text);
        }
        return None;
    }

    public static boolean doesStop(final EnumSet<LocationActivityCode> activity) {
        final EnumSet<LocationActivityCode> copyOfStops = EnumSet.copyOf(stops);
        return copyOfStops.removeAll(activity);
    }

    public static boolean doesPickup(final EnumSet<LocationActivityCode> activity) {
        final EnumSet<LocationActivityCode> copyOfPickups = EnumSet.copyOf(pickUps);
        return copyOfPickups.removeAll(activity);
    }

    public static boolean doesDropOff(final EnumSet<LocationActivityCode> activity) {
        final EnumSet<LocationActivityCode> copyOfDropoffs = EnumSet.copyOf(dropOffs);
        return copyOfDropoffs.removeAll(activity);
    }

    //@Override
    public String getCode() {
        return code;
    }

}
