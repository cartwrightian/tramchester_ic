package com.tramchester.domain.collections;


import java.util.Objects;

public class RouteIndexPair {
    private final short first;
    private final short second;
    private final int hashCode;

    /***
     * Always via RouteIndexPairFactory
     * @param first route index
     * @param second route index
     */
    private RouteIndexPair(final short first, final short second) {
        this.first = first;
        this.second = second;
        hashCode = Objects.hash(first, second);
    }

    /***
     * Always via RouteIndexPairFactory
     * @param first route index
     * @param second route index
     * @return the pair
     */
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
        // since always via RouteIndexPairFactory
        return this == o;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

}
