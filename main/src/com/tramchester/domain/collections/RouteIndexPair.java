package com.tramchester.domain.collections;


import java.util.Objects;

public class RouteIndexPair {
    private final short first;
    private final short second;
    private final int hashCode;

    private RouteIndexPair(final short first, final short second) {
        this.first = first;
        this.second = second;
        hashCode = Objects.hash(first, second);
    }

    static RouteIndexPair of(final short first, final short second) {
        return new RouteIndexPair(first, second);
    }

    public short first() {
        return first;
    }

    public short second() {
        return second;
    }

    public boolean isSame() {
        return first == second;
    }

    public PairOfRouteIndexPair expandWith(final RouteIndexPairFactory pairFactory, final int linkAsInt) {
        final short link = (short) linkAsInt;
        return PairOfRouteIndexPair.of(pairFactory.get(first, link), pairFactory.get(link, second));
    }

    @Override
    public String toString() {
        return "RouteIndexPair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RouteIndexPair routePair = (RouteIndexPair) o;
        return first == routePair.first && second == routePair.second;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

}
