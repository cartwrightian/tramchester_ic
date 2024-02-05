package com.tramchester.geo;

// Note: UK national grid
// https://en.wikipedia.org/wiki/Ordnance_Survey_National_Grid#All-numeric_grid_references

public class GridPosition {
    private final int eastings;
    private final int northings;

    public GridPosition(int eastings, int northings) {
        this.eastings = eastings;
        this.northings = northings;
    }

    // National grid positions as always +ve
    public static final GridPosition Invalid =  new GridPosition(Integer.MIN_VALUE, Integer.MIN_VALUE);

    public int getEastings() {
        return eastings;
    }

    public int getNorthings() {
        return northings;
    }

    public boolean isValid() {
        return eastings>=0 && northings>=0;
    }

    @Override
    public String toString() {
        if (isValid()) {
            return "GridPosition{" +
                    "easting=" + eastings +
                    ", northing=" + northings +
                    '}';
        }
        else {
            return "GridPosition{INVALID}";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GridPosition that = (GridPosition) o;

        if (getEastings() != that.getEastings()) return false;
        return getNorthings() == that.getNorthings();
    }

    @Override
    public int hashCode() {
        int result = (int) (getEastings() ^ (getEastings() >>> 32));
        result = 31 * result + (int) (getNorthings() ^ (getNorthings() >>> 32));
        return result;
    }

}
