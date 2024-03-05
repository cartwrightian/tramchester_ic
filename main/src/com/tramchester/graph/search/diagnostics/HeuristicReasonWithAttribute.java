package com.tramchester.graph.search.diagnostics;

import java.util.Objects;
import java.util.function.Function;

public class HeuristicReasonWithAttribute<T> extends HeuristicReasonWithValidity {

    private final T attribute;
    private final Function<T, String> displayAttribute;

    protected HeuristicReasonWithAttribute(ReasonCode code, HowIGotHere howIGotHere, T attribute, boolean validity,
                                           Function<T, String> displayAttribute) {
        super(code, howIGotHere, validity);
        this.attribute = attribute;
        this.displayAttribute = displayAttribute;
    }

    @Override
    public String textForGraph() {
        return super.textForGraph() + " " + displayAttribute.apply(attribute);
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

    public String textForAttribute() {
        return displayAttribute.apply(attribute);
    }
}
