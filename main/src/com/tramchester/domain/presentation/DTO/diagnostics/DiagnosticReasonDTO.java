package com.tramchester.domain.presentation.DTO.diagnostics;

import com.tramchester.graph.search.diagnostics.HeuristicsReason;
import com.tramchester.graph.search.diagnostics.ReasonCode;

public class DiagnosticReasonDTO {
    private final ReasonCode code;
    private final String text;
    private final boolean isValid;

    public DiagnosticReasonDTO(HeuristicsReason heuristicsReason) {
        this.code = heuristicsReason.getReasonCode();
        this.text = heuristicsReason.textForGraph();
        this.isValid = heuristicsReason.isValid();
    }

    public ReasonCode getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public boolean isValid() {
        return isValid;
    }
}
