package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.time.TramTime;

import java.util.Objects;

import static java.lang.String.format;

class DoesNotOperateOnTime extends SimpleHeuristicReason {
    protected final TramTime elapsedTime;

    protected DoesNotOperateOnTime(final ReasonCode reasonCode, final TramTime elapsedTime, final HowIGotHere path) {
        super(reasonCode, path);
        if (elapsedTime == null) {
            throw new RuntimeException("Must provide time");
        }
        this.elapsedTime = elapsedTime;
    }

    @Override
    public String textForGraph() {
        return format("%s%s%s", getReasonCode().name(), System.lineSeparator(), elapsedTime.toPattern());
    }

    @Override
    public String toString() {
        return super.toString() + " time:" + elapsedTime.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DoesNotOperateOnTime that = (DoesNotOperateOnTime) o;
        return Objects.equals(elapsedTime, that.elapsedTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), elapsedTime);
    }
}
