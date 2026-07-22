package com.tramchester.dataimport.rail.records.reference;

// https://wiki.openraildata.com/index.php?title=Activity_codes

import com.tramchester.domain.collections.ImmutableEnumSet;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    private static final ImmutableEnumSet<LocationActivityCode> dropOffs = ImmutableEnumSet.of(TrainFinishes, StopsToSetDownPassengers,
            StopsToTakeUpAndSetDownPassengers, StopsWhenRequired);

    private static final ImmutableEnumSet<LocationActivityCode> pickUps = ImmutableEnumSet.of(TrainBegins, StopsToTakeUpPassengers,
            StopsToTakeUpAndSetDownPassengers, StopsWhenRequired);

    private static final ImmutableEnumSet<LocationActivityCode> stops = ImmutableEnumSet.copyOf(EnumSet.of(TrainBegins, TrainFinishes,
            StopsToSetDownPassengers, StopsToTakeUpPassengers,
            StopsToTakeUpAndSetDownPassengers, StopsWhenRequired));

    private final String code;

    LocationActivityCode(final String code) {
        this.code = code;
    }

    private static ImmutableEnumSet<LocationActivityCode> parse(final String code) {
        final String lookup = code.trim();
        if (lookup.isEmpty()) {
            return ImmutableEnumSet.noneOf(LocationActivityCode.class);
        }

        return getCodesFor(lookup);
    }

    private static ImmutableEnumSet<LocationActivityCode> getCodesFor(final String text) {
       final EnumSet<LocationActivityCode> result = EnumSet.noneOf(LocationActivityCode.class);

        final String[] tokens = StringUtils.split(text,' ');
        for (final String token : tokens) {
            result.addAll(parseToken(token, result));
        }
        return ImmutableEnumSet.copyOf(result);
    }

    private static EnumSet<LocationActivityCode> parseToken(final String token, final EnumSet<LocationActivityCode> accumulator) {

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
                accumulator.add(attempt);
                toProcess = toProcess.substring(attempt.code.length());
            }
        }
        return accumulator;

    }

    private static LocationActivityCode parseSingle(final String text) {
        if (codes.containsCode(text)) {
            return codes.get(text);
        }
        return None;
    }

    public static boolean doesStop(final ImmutableEnumSet<LocationActivityCode> activity) {
        return stops.anyIntersectionWith(activity);
    }

    public static boolean doesPickup(final ImmutableEnumSet<LocationActivityCode> activity) {
        return pickUps.anyIntersectionWith(activity);
    }

    public static boolean doesDropOff(final ImmutableEnumSet<LocationActivityCode> activity) {
        return dropOffs.anyIntersectionWith(activity);
    }

    @Override
    public String getCode() {
        return code;
    }

    public static class Parser {

        private final ConcurrentMap<String, ImmutableEnumSet<LocationActivityCode>> cache;

        public Parser() {
            cache = new ConcurrentHashMap<>();
        }

        public ImmutableEnumSet<LocationActivityCode> parse(final String text) {
            return cache.computeIfAbsent(text, z -> LocationActivityCode.parse(text));
        }

    }

}
