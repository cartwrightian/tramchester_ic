package com.tramchester.graph.search.diagnostics;

import java.util.Objects;
import java.util.function.Function;

public class HeuristicReasonWithAttributes<A,B> extends HeuristicReasonWithValidity {

    private final A attributeA;
    private final B attributeB;
    private final Function<A, String> displayA;
    private final Function<B, String> displayB;

    protected HeuristicReasonWithAttributes(ReasonCode code, HowIGotHere howIGotHere,
                                            A attributeA, B attributeB, boolean validity, Function<A, String> displayA, Function<B, String> displayB) {
        super(code, howIGotHere, validity);
        this.attributeA = attributeA;
        this.attributeB = attributeB;
        this.displayA = displayA;
        this.displayB = displayB;
    }

    @Override
    public String textForGraph() {
        return super.textForGraph() + " " + displayA.apply(attributeA) + " " + displayB.apply(attributeB);
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

    public String textForAttributeA() {
        return displayA.apply(attributeA);
    }

    public String textForAttributeB() {
        return displayB.apply(attributeB);
    }
}
