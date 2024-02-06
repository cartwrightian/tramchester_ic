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

    protected StringIdFor(String theId, Class<T> domainType) {
        this.theId = theId.intern();
        this.domainType = domainType;
        this.hashcode = Objects.hash(theId, domainType);
    }

    // for invalid ids
    // TODO Need better way to handle this, push into i/f?
    private StringIdFor(Class<T> domainType) {
        this("", domainType);
    }

    // todo package private?
    public static <C extends CoreDomain> IdFor<C> createId(String text, Class<C> domainType) {
        if (text==null) {
            return invalid(domainType);
        }
        if (text.isBlank()) {
            return invalid(domainType);
        }
        return new StringIdFor<>(text, domainType);
    }

    public static IdFor<Station> createId(IdForDTO idForDTO, Class<Station> klass) {
        return createId(idForDTO.getActualId(), klass);
    }

    public static <T extends CoreDomain> IdSet<T> createIds(Set<String> items, Class<T> domainClass) {
        return items.stream().map(item -> StringIdFor.createId(item, domainClass)).collect(IdSet.idCollector());
    }

    public static <DEST extends CoreDomain, SOURCE extends CoreDomain> StringIdFor<DEST> concat(IdFor<SOURCE> originalId, String text, Class<DEST> domainType) {
        StringIdFor<SOURCE> originalStringId = (StringIdFor<SOURCE>) originalId;
        String newId = originalStringId.theId + text;
        return new StringIdFor<>(newId, domainType);
    }

    public static String removeIdFrom(final String text, final IdFor<?> id) {
        StringIdFor<?> originalStringId = (StringIdFor<?>) id;
        return text.replace(originalStringId.theId, "");
    }

    String getContainedId() {
        return theId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o==null) {
            return false;
        }
        if (o instanceof StringIdFor) {
            StringIdFor<?> that = (StringIdFor<?>) o;
            return theId.equals(that.theId) && domainType.equals(that.domainType);
        }
        if (o instanceof ContainsId) {
            ContainsId<?> that = (ContainsId<?>) o;
            StringIdFor<?> thatContainedId = that.getContainedId();
            return theId.equals(thatContainedId.theId) && domainType.equals(thatContainedId.domainType);
        }
        return false;

    }

    @Override
    public String toString() {
        String domainName = domainType.getSimpleName();
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

    public static <CLASS extends CoreDomain> StringIdFor<CLASS> invalid(Class<CLASS> domainType) {
        return new StringIdFor<>(domainType);
    }

    public static <FROM extends CoreDomain,TO extends CoreDomain> IdFor<TO> convert(IdFor<FROM> original, Class<TO> domainType) {
        guardForType(original);
        StringIdFor<FROM> other = (StringIdFor<FROM>) original;
        return createId(other.theId, domainType);
    }

    public static <T extends CoreDomain, S extends CoreDomain> IdFor<T> withPrefix(String prefix, IdFor<S> original, Class<T> domainType) {
        guardForType(original);
        StringIdFor<S> other = (StringIdFor<S>) original;
        return createId(prefix+other.theId, domainType);
    }

    private static <FROM extends CoreDomain> void guardForType(IdFor<FROM> original) {
        if (!(original instanceof StringIdFor)) {
            throw new RuntimeException(original + " is not a StringIdFor");
        }
    }

}
