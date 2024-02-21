package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.ValidHeuristicReason;

import java.util.Objects;

public class HeuristicReasonsOK {

    public static HeuristicsReason IsValid(final ReasonCode code, final HowIGotHere path) {
        return new ValidHeuristicReason( code, path);
    }

    public static HeuristicsReason Continue(final HowIGotHere path) {
        return new ValidHeuristicReason(ReasonCode.Continue, path);
    }

    public static HeuristicsReason Arrived(final HowIGotHere path) {
        return new ValidHeuristicReason(ReasonCode.Arrived, path);
    }

    public static HeuristicsReason NumChangesOK(ReasonCode reasonCode, HowIGotHere howIGotHere, int currentNumChanges) {
        return new ValidWithAttribute<>(reasonCode, howIGotHere, currentNumChanges);
    }

    public static HeuristicsReason TimeOK(ReasonCode reasonCode, HowIGotHere howIGotHere, TramTime tramTime) {
        return new ValidWithAttribute<>(reasonCode, howIGotHere, tramTime);
    }

    public static HeuristicsReason HourOk(ReasonCode reasonCode, HowIGotHere howIGotHere, TramTime tramTime) {
        return new ValidWithAttribute<>(reasonCode, howIGotHere, tramTime);
    }

    public static HeuristicsReason SeenGroup(ReasonCode reasonCode, HowIGotHere howIGotHere, IdFor<NPTGLocality> areaId) {
        return new ValidWithAttribute<>(reasonCode, howIGotHere, areaId);
    }

    private static class ValidWithAttribute<T> extends ValidHeuristicReason {

        private final T attribute;

        public ValidWithAttribute(ReasonCode code, HowIGotHere howIGotHere, T attribute) {
            super(code, howIGotHere);
            this.attribute = attribute;
        }

        @Override
        public String textForGraph() {
            return super.textForGraph()+":"+attribute.toString();
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            ValidWithAttribute<?> that = (ValidWithAttribute<?>) o;
            return Objects.equals(attribute, that.attribute);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), attribute);
        }
    }


}
