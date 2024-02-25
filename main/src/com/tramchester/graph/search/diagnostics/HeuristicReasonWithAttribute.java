package com.tramchester.graph.search.diagnostics;

import java.util.Objects;

public class HeuristicReasonWithAttribute<T> extends HeuristicReasonWithValidity {

    private final T attribute;

    protected HeuristicReasonWithAttribute(ReasonCode code, HowIGotHere howIGotHere, T attribute, boolean validity) {
        super(code, howIGotHere, validity);
        this.attribute = attribute;
    }

    @Override
    public String textForGraph() {
        return super.textForGraph() + " " + attribute.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        HeuristicReasonWithAttribute<?> that = (HeuristicReasonWithAttribute<?>) o;
        return Objects.equals(attribute, that.attribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attribute);
    }

    public T getAttribute() {
        return attribute;
    }
}
