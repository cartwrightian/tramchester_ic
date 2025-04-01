package com.tramchester.geo;

import tech.units.indriya.ComparableQuantity;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.quantity.Length;
import java.util.Objects;

public class MarginInMeters {
    private final int meters;

    private MarginInMeters(int meters) {
        this.meters = meters;
    }

    public static MarginInMeters ofMeters(final int meters) {
        return new MarginInMeters(meters);
    }

    public static MarginInMeters ofKM(final Double kilometers) {
        final double meters = kilometers * 1000D;
        return new MarginInMeters(Math.toIntExact(Math.round(meters)));
    }

    public static MarginInMeters invalid() {
        return new MarginInMeters(Integer.MIN_VALUE);
    }

    /***
     * @return margin in meters
     */
    public int get() {
        return meters;
    }

    public ComparableQuantity<Length> getDistance() {
        return Quantities.getQuantity(meters, Units.METRE);
    }

    @Override
    public String toString() {
        return "MarginInMeters{" +
                "meters=" + meters +
                '}';
    }

    public boolean within(final ComparableQuantity<Length> amount) {
        return amount.isLessThanOrEqualTo(getDistance());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarginInMeters that = (MarginInMeters) o;
        return meters == that.meters;
    }

    @Override
    public int hashCode() {
        return Objects.hash(meters);
    }
}
