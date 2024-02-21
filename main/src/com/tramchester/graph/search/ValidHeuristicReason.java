package com.tramchester.graph.search;

import com.tramchester.graph.search.diagnostics.HowIGotHere;
import com.tramchester.graph.search.diagnostics.ReasonCode;
import com.tramchester.graph.search.diagnostics.InvalidHeuristicReason;

public class ValidHeuristicReason extends InvalidHeuristicReason {
    public ValidHeuristicReason(ReasonCode code, HowIGotHere howIGotHere) {
        super(code, howIGotHere);
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
