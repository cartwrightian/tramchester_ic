package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.time.TramTime;

public class HeuristicReasonsOK {

    public static HeuristicsReason IsValid(final ReasonCode code, final HowIGotHere path) {
        return new HeuristicReasonWithValidity( code, path, true);
    }

    public static HeuristicsReason Continue(final HowIGotHere path) {
        return new HeuristicReasonWithValidity(ReasonCode.Continue, path, true);
    }

    public static HeuristicsReason Arrived(final HowIGotHere path) {
        return new HeuristicReasonWithValidity(ReasonCode.Arrived, path, true);
    }

    public static HeuristicsReason NumChangesOK(ReasonCode reasonCode, HowIGotHere howIGotHere, int currentNumChanges) {
        return new HeuristicReasonWithAttribute<>(reasonCode, howIGotHere, currentNumChanges, true);
    }

    public static HeuristicsReason TimeOK(ReasonCode reasonCode, HowIGotHere howIGotHere, TramTime tramTime) {
        return new HeuristicReasonWithAttribute<>(reasonCode, howIGotHere, tramTime, true);
    }

    public static HeuristicsReason HourOk(ReasonCode reasonCode, HowIGotHere howIGotHere, TramTime tramTime, int hour) {
        return new HeuristicReasonWithAttributes<>(reasonCode, howIGotHere, tramTime, hour,true);
    }

    public static HeuristicsReason SeenGroup(ReasonCode reasonCode, HowIGotHere howIGotHere, IdFor<NPTGLocality> areaId) {
        return new HeuristicReasonWithAttribute<>(reasonCode, howIGotHere, areaId, true);
    }

}
