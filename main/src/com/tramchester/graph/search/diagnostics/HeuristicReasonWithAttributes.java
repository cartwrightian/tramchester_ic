package com.tramchester.graph.search.diagnostics;

import java.util.Objects;

public class HeuristicReasonWithAttributes<A,B> extends HeuristicReasonWithValidity {

    private final A attributeA;
    private final B attributeB;

    protected HeuristicReasonWithAttributes(ReasonCode code, HowIGotHere howIGotHere,
                                            A attributeA, B attributeB, boolean validity) {
        super(code, howIGotHere, validity);
        this.attributeA = attributeA;
        this.attributeB = attributeB;
    }

    @Override
    public String textForGraph() {
        return super.textForGraph() + " " + attributeA.toString() + " " + attributeB.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        HeuristicReasonWithAttributes<?, ?> that = (HeuristicReasonWithAttributes<?, ?>) o;
        return Objects.equals(attributeA, that.attributeA) && Objects.equals(attributeB, that.attributeB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attributeA, attributeB);
    }

    public A getAttributeA() {
        return attributeA;
    }

    public B getAttributeB() {
        return attributeB;
    }
}
