package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.IdFor;

import java.util.Objects;

public class HeuristicsReasonWithID<T extends CoreDomain> extends SimpleHeuristicReason {
    private final IdFor<T> id;

    protected HeuristicsReasonWithID(final ReasonCode code, final HowIGotHere path, final IdFor<T> id) {
        super(code, path);
        this.id = id;
    }

    @Override
    public String textForGraph() {
        return super.textForGraph() + ":" +id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        HeuristicsReasonWithID<?> that = (HeuristicsReasonWithID<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
