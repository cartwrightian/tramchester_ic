package com.tramchester.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.LatLong;

import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Valid
public class BoundingBox {

    // whole uk, approx
    //    minEastings: 112685
    //    minNorthings: 015490
    //    maxEasting: 619307
    //    maxNorthings: 118843

    private final GridPosition bottomLeft;
    private final GridPosition topRight;

    public BoundingBox(@JsonProperty(value = "minEastings", required = true) int minEastings,
                       @JsonProperty(value = "minNorthings", required = true) int minNorthings,
                       @JsonProperty(value = "maxEasting", required = true) int maxEasting,
                       @JsonProperty(value = "maxNorthings", required = true) int maxNorthings) {
        this(new GridPosition(minEastings, minNorthings), new GridPosition(maxEasting, maxNorthings));
    }

    public BoundingBox(GridPosition bottomLeft, GridPosition topRight) {
        if (!bottomLeft.isValid()) {
            throw new RuntimeException("bottemLeft is invalid");
        }
        if (!topRight.isValid()) {
            throw new RuntimeException("topRight is invalid");
        }
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
    }

    public BoundingBox(BoundingBox other) {
        this(other.bottomLeft, other.topRight);
    }

    public GridPosition getBottomLeft() {
        return bottomLeft;
    }

    public GridPosition getTopRight() {
        return topRight;
    }

    public int getMinEastings() {
        return bottomLeft.getEastings();
    }

    public int getMinNorthings() {
        return bottomLeft.getNorthings();
    }

    public int getMaxEasting() {
        return topRight.getEastings();
    }

    public int getMaxNorthings() {
        return topRight.getNorthings();
    }

    public boolean within(final MarginInMeters marginInMeters, final GridPosition position) {
        if (!position.isValid()) {
            throw new RuntimeException("Invalid grid position " + position);
        }

        final long margin = marginInMeters.get();
        final int eastings = position.getEastings();
        if ((eastings >= getMinEastings() - margin) && (eastings <= getMaxEasting() + margin)) {
            final int northings = position.getNorthings();
            return (northings >= getMinNorthings() - margin) && (northings <= getMaxNorthings() + margin);
        } else {
            return false;
        }

    }

    public boolean contained(final Location<?> hasPosition) {
        return contained(hasPosition.getGridPosition());
    }

    public boolean contained(final LatLong destination) {
        return contained(CoordinateTransforms.getGridPosition(destination));
    }

    public boolean contained(final GridPosition position) {
        if (!position.isValid()) {
            throw new RuntimeException("Invalid grid position " + position);
        }

        return (position.getEastings() >= getMinEastings()) &&
                (position.getEastings() <= getMaxEasting()) &&
                (position.getNorthings() >= getMinNorthings()) &&
                (position.getNorthings() <= getMaxNorthings());
    }

    public boolean overlapsWith(final BoundingBox other) {
        if (other.bottomLeft.getNorthings()>topRight.getNorthings()) {
            return false;
        }
        if (other.topRight.getNorthings()<bottomLeft.getNorthings()) {
            return false;
        }
        if (other.bottomLeft.getEastings()>topRight.getEastings()) {
            return false;
        }
        if (other.topRight.getEastings()<bottomLeft.getEastings()) {
            return false;
        }
        return true;
    }

//    public GridPosition middle() {
//        final int left = bottomLeft.getEastings();
//        final int top = topRight.getNorthings();
//        final int bottom = bottomLeft.getNorthings();
//        final int right = topRight.getEastings();
//
//        int midEasting = left + ((right - left) / 2);
//        int midNorthing = bottom +  ((top - bottom) / 2);
//
//        return new GridPosition(midEasting, midNorthing);
//    }

    public Set<BoundingBox> quadrants() {
        final Set<BoundingBox> result = new HashSet<>();

        final int left = bottomLeft.getEastings();
        final int top = topRight.getNorthings();
        final int bottom = bottomLeft.getNorthings();
        final int right = topRight.getEastings();

        final int midEasting = left + ((right - left) / 2);
        final int midNorthing = bottom +  ((top - bottom) / 2);

        final GridPosition middle = new GridPosition(midEasting, midNorthing);

        final BoundingBox bottomLeftQuadrant = new BoundingBox(bottomLeft, middle);
        final BoundingBox topRightQuadrant = new BoundingBox(middle, topRight);

        final BoundingBox topLeftQuadrant = new BoundingBox(
                new GridPosition(left, midNorthing), new GridPosition(midEasting, top));
        final BoundingBox bottomRightQuadrant = new BoundingBox(
                new GridPosition(midEasting, bottom), new GridPosition(right, midNorthing));

        result.add(bottomLeftQuadrant);
        result.add(topRightQuadrant);
        result.add(topLeftQuadrant);
        result.add(bottomRightQuadrant);

        return result;
    }

    @Override
    public String toString() {
        return "BoundingBox{" +
                "bottomLeft=" + bottomLeft +
                ", topRight=" + topRight +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoundingBox that = (BoundingBox) o;

        if (!bottomLeft.equals(that.bottomLeft)) return false;
        return topRight.equals(that.topRight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bottomLeft, topRight);
//        int result = bottomLeft.hashCode();
//        result = 31 * result + topRight.hashCode();
//        return result;
    }

    public long width() {
        return Math.abs(bottomLeft.getEastings() - topRight.getEastings());
    }

    public long height() {
        return Math.abs(bottomLeft.getNorthings() - topRight.getNorthings());
    }

    public GridPosition getMidPoint() {
        int midEasting = (bottomLeft.getEastings() + topRight.getEastings()) / 2;
        int midNorthing = (bottomLeft.getNorthings() + topRight.getNorthings()) / 2;
        return new GridPosition(midEasting, midNorthing);
    }
}
