package com.tramchester.domain;

public class NumberOfChanges {
    private final int min;
    private int max;

    public NumberOfChanges(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public static NumberOfChanges None() {
        return new NumberOfChanges(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    @Override
    public String toString() {
        return "NumberOfChanges{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }

    @Deprecated
    public void overrideMax(int maxChanges) {
        max = maxChanges;
    }

    public void overrideMax(JourneyRequest.MaxNumberOfChanges maxChanges) {
        max = maxChanges.get();
    }

    public boolean isNone() {
        return min==Integer.MAX_VALUE;
    }


}
