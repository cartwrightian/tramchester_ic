package com.tramchester.geo;

public class GridPositions {

    public static boolean withinDistEasting(final GridPosition gridPositionA, final GridPosition gridPositionB, final MarginInMeters rangeInMeters) {
        return getDistEasting(gridPositionA, gridPositionB) <= rangeInMeters.get();
    }

    public static boolean withinDistNorthing(final GridPosition gridPositionA, final GridPosition gridPositionB, final MarginInMeters rangeInMeters) {
        return getDistNorthing(gridPositionA, gridPositionB) <= rangeInMeters.get();
    }

    private static int getDistNorthing(final GridPosition gridPositionA, final GridPosition gridPositionB) {
        return Math.abs(gridPositionA.getNorthings() - gridPositionB.getNorthings());
    }

    private static int getDistEasting(final GridPosition gridPositionA, final GridPosition gridPositionB) {
        return Math.abs(gridPositionA.getEastings() - gridPositionB.getEastings());
    }

    private static long getSumSquaresDistance(final GridPosition gridPositionA, final GridPosition gridPositionB) {
        final long distHorz = getDistEasting(gridPositionA, gridPositionB);
        final long distVert = getDistNorthing(gridPositionA, gridPositionB);
        return (distHorz * distHorz) + (distVert * distVert);
    }

    public static boolean withinDist(final GridPosition gridPositionA, final GridPosition gridPositionB, final MarginInMeters rangeInMeters) {
        final long meters = rangeInMeters.get();
        final long hypSquared = meters*meters;
        final long sum = getSumSquaresDistance(gridPositionA, gridPositionB);
        return sum<=hypSquared;
    }

    public static long distanceTo(final GridPosition gridPositionA, final GridPosition gridPositionB) {
        final long sum = getSumSquaresDistance(gridPositionA, gridPositionB);
        return Math.round(Math.sqrt(sum));
    }

}
