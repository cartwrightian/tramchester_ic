package com.tramchester.domain.id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.places.Station;

import java.util.Objects;
import java.util.Set;

@JsonDeserialize(using = StringIdForDeserializer.class)
@JsonSerialize(using = StringIdForSerializer.class)
public class StringIdFor<T extends CoreDomain> implements IdFor<T> {
    private final String theId;
    private final int hashcode;
    private final Class<T> domainType;

    protected StringIdFor(final String theId, final Class<T> domainType) {
        this.theId = theId.intern();
        this.domainType = domainType;
        this.hashcode = Objects.hash(theId, domainType);
    }

    // for invalid ids
    // TODO Need better way to handle this, push into i/f?
    private StringIdFor(final Class<T> domainType) {
        this("", domainType);
    }

    // todo package private?
    public static <C extends CoreDomain> StringIdFor<C> createId(final String text, final Class<C> domainType) {
        if (text==null) {
            return invalid(domainType);
        }
        if (text.isBlank()) {
            return invalid(domainType);
        }
        return new StringIdFor<>(text, domainType);
    }

    public static IdFor<Station> createId(final IdForDTO idForDTO, final Class<Station> klass) {
        return createId(idForDTO.getActualId(), klass);
    }

    public static <T extends CoreDomain> IdSet<T> createIds(final Set<String> items, final Class<T> domainClass) {
        return items.stream().map(item -> StringIdFor.createId(item, domainClass)).collect(IdSet.idCollector());
    }

    public static <DEST extends CoreDomain, SOURCE extends CoreDomain> StringIdFor<DEST> concat(final IdFor<SOURCE> originalId, final String text, final Class<DEST> domainType) {
        final StringIdFor<SOURCE> originalStringId = (StringIdFor<SOURCE>) originalId;
        final String newId = originalStringId.theId + text;
        return new StringIdFor<>(newId, domainType);
    }

    public static String removeIdFrom(final String text, final IdFor<?> id) {
        final StringIdFor<?> originalStringId = (StringIdFor<?>) id;
        return text.replace(originalStringId.theId, "");
    }

    String getContainedId() {
        return theId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj==null) {
            return false;
        }
        if (obj instanceof StringIdFor<?> other) {
            return theId.equals(other.theId) && domainType.equals(other.domainType);
        }
        if (obj instanceof ContainsId<?> other) {
            final StringIdFor<?> thatContainedId = other.getContainedId();
            return theId.equals(thatContainedId.theId) && domainType.equals(thatContainedId.domainType);
        }
        return false;

    }

    @Override
    public String toString() {
        final String domainName = domainType.getSimpleName();
        if (isValid()) {
            return "Id{'" + domainName+ ":" + theId + "'}";
        } else {
            return "Id{"+domainName+":NOT_VALID}";
        }
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public String getGraphId() {
        return theId;
    }

    @Override
    public boolean isValid() {
        return !theId.isEmpty();
    }

    @Override
    public Class<T> getDomainType() {
        return domainType;
    }

    public static <CLASS extends CoreDomain> StringIdFor<CLASS> invalid(final Class<CLASS> domainType) {
        return new StringIdFor<>(domainType);
    }

    public static <FROM extends CoreDomain,TO extends CoreDomain> IdFor<TO> convert(final IdFor<FROM> original, final Class<TO> domainType) {
        guardForType(original);
        final StringIdFor<FROM> other = (StringIdFor<FROM>) original;
        return createId(other.theId, domainType);
    }

    public static <T extends CoreDomain, S extends CoreDomain> IdFor<T> withPrefix(final String prefix, final IdFor<S> original, final Class<T> domainType) {
        guardForType(original);
        final StringIdFor<S> other = (StringIdFor<S>) original;
        return createId(prefix+other.theId, domainType);
    }

    private static <FROM extends CoreDomain> void guardForType(final IdFor<FROM> original) {
        if (!(original instanceof StringIdFor)) {
            throw new RuntimeException(original + " is not a StringIdFor");
        }
    }

}
