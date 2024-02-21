package com.tramchester.domain.presentation.DTO.diagnostics;

import com.tramchester.graph.search.diagnostics.HeuristicsReason;
import com.tramchester.graph.search.diagnostics.ReasonCode;

import java.util.Objects;

public class DiagnosticReasonDTO {
    private final ReasonCode code;
    private final String text;
    private final boolean isValid;

    public DiagnosticReasonDTO(HeuristicsReason heuristicsReason) {
        this(heuristicsReason.getReasonCode(), heuristicsReason.textForGraph(), heuristicsReason.isValid());
    }

    public DiagnosticReasonDTO(ReasonCode code, String text, boolean isValid) {
        this.code = code;
        this.text = text;
        this.isValid = isValid;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiagnosticReasonDTO that = (DiagnosticReasonDTO) o;
        return isValid == that.isValid && code == that.code && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, text, isValid);
    }
}
