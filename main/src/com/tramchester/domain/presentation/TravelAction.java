package com.tramchester.domain.presentation;

import com.tramchester.domain.reference.TransportMode;

public enum TravelAction {
    Leave, Change, Board, WalkTo, WalkFrom,ConnectTo, None;

    public static String decideActionText(final TravelAction travelAction, final TransportMode transportMode, TravelAction previousAction) {
        final String modeName = TransportMode.isReplacementBus(transportMode) ? "Replacement Bus" : transportMode.name();
        return switch (travelAction) {
            case Leave, Board -> travelAction.name() + " " + modeName;
            case Change -> boardAfter(previousAction) ? "Board " + modeName : "Change " + modeName;
            case WalkTo -> "Walk to";
            case WalkFrom -> "Walk from";
            case ConnectTo -> "Connect with";
            case None -> throw new RuntimeException("Undefined");
        };
    }

    private static boolean boardAfter(TravelAction previousAction) {
        return previousAction==WalkTo || previousAction==ConnectTo;
    }
}
