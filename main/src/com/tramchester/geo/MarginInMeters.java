package com.tramchester.geo;

import tech.units.indriya.ComparableQuantity;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.quantity.Length;

public class MarginInMeters {
    private final ComparableQuantity<Length> distance;
    private final long meters;

    public static MarginInMeters of(long meters) {
        return new MarginInMeters(meters);
    }

    private MarginInMeters(long meters) {
        this.distance =  Quantities.getQuantity(meters, Units.METRE);
        this.meters = meters;
    }

    public static MarginInMeters of(Double kilometers) {
        double meters = kilometers * 1000D;
        return new MarginInMeters(Math.round(meters));
    }

    public static MarginInMeters invalid() {
        return new MarginInMeters(Long.MIN_VALUE);
    }

    /***
     * @return margin in meters
     */
    public long get() {
        return meters;
    }

    @Override
    public String toString() {
        return "MarginInMeters{" +
                "meters=" + distance +
                '}';
    }

    public boolean within(final ComparableQuantity<Length> amount) {

        return amount.isLessThanOrEqualTo(distance);

//        final Quantity<Length> amountInMeters = amount.to(Units.METRE);
//        final Number value = amountInMeters.getValue();
//
//        return amount <= meters;
    }
}
