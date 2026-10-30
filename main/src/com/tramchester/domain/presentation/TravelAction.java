package com.tramchester.domain.presentation;

import com.tramchester.domain.reference.TransportMode;

public enum TravelAction {
    Leave, Change, Board, WalkTo, WalkFrom,ConnectTo, None;

    public static String decideActionText(final TravelAction action, final TransportMode mode,
                                          final TravelAction previousAction, final TransportMode previousMode) {
        final String modeName = TransportMode.isReplacementBus(mode) ? "Replacement Bus" : mode.name();
        return switch (action) {
            case Leave, Board -> action.name() + " " + modeName;
            case Change -> boardAfter(mode, previousAction, previousMode) ? "Board " + modeName : "Change " + modeName;
            case WalkTo -> "Walk to";
            case WalkFrom -> "Walk from";
            case ConnectTo -> "Connect with";
            case None -> throw new RuntimeException("Undefined");
        };
    }

    private static boolean boardAfter(TransportMode currentMode, TravelAction previousAction, TransportMode previousMode) {
        if (previousAction==WalkTo || previousAction==ConnectTo) {
            return true;
        }
        return (previousMode!=currentMode);
    }
}
