package com.tramchester.domain.presentation.DTO.diagnostics;

import com.tramchester.graph.search.diagnostics.HeuristicsReason;
import com.tramchester.graph.search.diagnostics.HowIGotHere;
import com.tramchester.graph.search.diagnostics.ReasonCode;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;

import java.util.Objects;

public class DiagnosticReasonDTO {
    private final ReasonCode code;
    private final String text;
    private final boolean isValid;
    private final TraversalStateType stateType;
    private final String beginId;
    private final String endId;

    public DiagnosticReasonDTO(final HeuristicsReason heuristicsReason) {
        this(heuristicsReason.getReasonCode(), heuristicsReason.textForGraph(), heuristicsReason.isValid(),
                heuristicsReason.getHowIGotHere());
    }

    public DiagnosticReasonDTO(ReasonCode code, String text, boolean isValid, HowIGotHere howIGotHere) {
        this.code = code;
        this.text = text;
        this.isValid = isValid;
        this.stateType = howIGotHere.getTraversalStateType();
        this.beginId = beginIdFrom(howIGotHere);
        this.endId = howIGotHere.getEndNodeId().toString();
    }

    private String beginIdFrom(final HowIGotHere howIGotHere) {
        if (howIGotHere.atStart()) {
            return "START_ID";
        } else {
            return howIGotHere.getPreviousId().toString();
        }
    }

    public ReasonCode getCode() {
        return code;
    }

    public String getText() {
        return stateType.name() +  " " + text;
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

    public TraversalStateType getStateType() {
        return stateType;
    }

    public String getBeginId() {
        return beginId;
    }

    public String getEndId() {
        return endId;
    }
}
