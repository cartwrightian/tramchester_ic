package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.time.TramTime;

import java.util.Objects;

import static java.lang.String.format;

class DoesNotOperateAtHour extends SimpleHeuristicReason {
    protected final int hour;

    protected DoesNotOperateAtHour(final ReasonCode reasonCode, final TramTime elapsedTime, final HowIGotHere path) {
        super(reasonCode, path);
        if (elapsedTime == null) {
            throw new RuntimeException("Must provide time");
        }
        this.hour = elapsedTime.getHourOfDay();
    }

    @Override
    public String textForGraph() {
        return format("%s%s%s", getReasonCode().name(), System.lineSeparator(), hour);
    }

    @Override
    public String toString() {
        return super.toString() + " hour:" + hour;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DoesNotOperateAtHour that = (DoesNotOperateAtHour) o;
        return Objects.equals(hour, that.hour);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), hour);
    }
}
