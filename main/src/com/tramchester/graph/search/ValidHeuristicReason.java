package com.tramchester.graph.search;

import com.tramchester.graph.search.diagnostics.HowIGotHere;
import com.tramchester.graph.search.diagnostics.ReasonCode;
import com.tramchester.graph.search.diagnostics.HeuristicReasonWithValidity;

public class ValidHeuristicReason extends HeuristicReasonWithValidity {
    public ValidHeuristicReason(ReasonCode code, HowIGotHere howIGotHere) {
        super(code, howIGotHere, true);
    }

}
