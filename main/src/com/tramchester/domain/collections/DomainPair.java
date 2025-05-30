package com.tramchester.domain.collections;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.IdPair;
import com.tramchester.domain.id.HasId;

import java.util.Objects;

public class DomainPair<T extends CoreDomain & HasId<T>> {
    private final T first;
    private final T second;

    public DomainPair(final T first, final T second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return "first=" + first.getId() + ", second=" + second.getId();
    }

    public T first() {
        return first;
    }

    public T second() {
        return second;
    }

    public boolean areSame() {
        return first.equals(second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainPair<?> that = (DomainPair<?>) o;
        return first.equals(that.first) && second.equals(that.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    public IdPair<T> getIds() {
        return new IdPair<>(first.getId(), second.getId());
    }


}
