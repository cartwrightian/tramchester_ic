package com.tramchester.domain.id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.CoreDomain;

// TODO Interface?
abstract class ContainsId<T extends CoreDomain> implements IdFor<T> {

    private final StringIdFor<T> containedId;

    ContainsId(StringIdFor<T> containedId) {
        this.containedId = containedId;
    }

    @JsonIgnore
    @Override
    final public String getGraphId() {
        return containedId.getGraphId();
    }

    @JsonProperty("diagnostics")
    final protected StringIdFor<T> getContainedId() {
        return containedId;
    }

    @JsonIgnore
    @Override
    public final Class<T> getDomainType() {
        return containedId.getDomainType();
    }

    @Override
    public final int hashCode() {
        return containedId.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (this==obj) {
            return true;
        }
        if (obj==null) {
            return false;
        }
        if (obj instanceof ContainsId<?> other) {
            return containedId.equals(other.containedId);
        }
        if (obj instanceof StringIdFor<?> other) {
            return containedId.equals(other);
        }
        return false;
    }


}
