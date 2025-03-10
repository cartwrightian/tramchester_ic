package com.tramchester.domain.id;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.collections.DomainPair;
import com.tramchester.domain.presentation.DTO.HasIdForDTO;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface HasId<DOMAINTYPE extends CoreDomain> {

    IdFor<DOMAINTYPE> getId();

    static <S extends HasId<?>> String asIds(final Collection<S> items) {
        return collectionToIdStringList(items, item -> item.getId().toString());
    }

    static <P extends DomainPair<?>> String asIds(final P pair) {
        return "(" + pair.first().getId() + ", " + pair.second().getId() + ")";
    }

    static <P extends DomainPair<?>> String asIds(final Set<P> pairs) {
        return collectionToIdStringList(pairs, pair -> pair.getIds().toString());
    }

    static String asIds(final IdMap<?> idMap) {
        return idMap.getIds().toString();
    }

    static String asIds(final LocationSet<?> locationSet) {
        return locationSet.asIds();
    }

    static String asIds(List<? extends HasIdForDTO> items) {
        return collectionToIdStringList(items, item -> item.getId().getActualId());
    }

    @NotNull
    static <T> String collectionToIdStringList(final Collection<T> items, final GetsId<T> getsId) {
        if (items==null) {
            return "[null]";
        }

        final StringBuilder ids = new StringBuilder();
        ids.append("[");
        items.forEach(item -> ids.append(" '").append(getsId.asString(item)).append("'"));
        ids.append("]");
        return ids.toString();
    }

    static <T extends HasId<T> & CoreDomain> IdFor<T> asId(final T item) {
        return item.getId();
    }

    interface GetsId<T> {
        String asString(T item);
    }
}
