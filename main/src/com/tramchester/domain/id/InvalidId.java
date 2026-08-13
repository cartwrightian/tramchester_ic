package com.tramchester.domain.id;

import com.tramchester.domain.CoreDomain;

import java.util.Objects;

public class InvalidId<T extends CoreDomain> implements IdFor<T> {

    private final Class<T> domainType;

    // TODO into static method, use a singleton for each Domain Type
    public InvalidId(Class<T> domainType) {
        this.domainType = domainType;
    }

    @Override
    public String getGraphId() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public Class<T> getDomainType() {
        return domainType;
    }

    @Override
    public String toString() {
        return "InvalidId";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        InvalidId<?> invalidId = (InvalidId<?>) o;
        return Objects.equals(domainType, invalidId.domainType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(domainType);
    }
}
