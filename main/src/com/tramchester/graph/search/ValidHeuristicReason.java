package com.tramchester.graph.search;

import com.tramchester.graph.search.diagnostics.HowIGotHere;
import com.tramchester.graph.search.diagnostics.ReasonCode;
import com.tramchester.graph.search.diagnostics.SimpleHeuristicReason;

public class ValidHeuristicReason extends SimpleHeuristicReason {
    public ValidHeuristicReason(ReasonCode code, HowIGotHere howIGotHere) {
        super(code, howIGotHere);
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
