package com.tramchester.graph.search.diagnostics;

import java.util.Objects;

public class InvalidHeutisticReasonWithAttribute<T> extends InvalidHeuristicReason {

    private final T attribue;

    protected InvalidHeutisticReasonWithAttribute(ReasonCode code, HowIGotHere howIGotHere, T attribue) {
        super(code, howIGotHere);
        this.attribue = attribue;
    }

    @Override
    public String textForGraph() {
        return super.textForGraph() + " " + attribue.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InvalidHeutisticReasonWithAttribute<?> that = (InvalidHeutisticReasonWithAttribute<?>) o;
        return Objects.equals(attribue, that.attribue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attribue);
    }
}
