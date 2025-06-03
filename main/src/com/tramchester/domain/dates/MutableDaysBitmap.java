package com.tramchester.domain.dates;

import java.time.DayOfWeek;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.IntStream;

import static java.lang.String.format;

public class MutableDaysBitmap implements DaysBitmap {
    private final long beginningDay;
    private final BitSet days;
    private final int size;

    public MutableDaysBitmap(long beginningEpochDay, int size) {
        this.beginningDay = beginningEpochDay;
        this.days = new BitSet(size);
        this.size = size;
    }

    public void setDaysOfWeek(final EnumSet<DayOfWeek> operatingDays) {
        for (int i = 0; i < size; i++) {
            TramDate date = TramDate.of(beginningDay + i);
            if (operatingDays.contains(date.getDayOfWeek())) {
                days.set(i);
            }
        }
    }

    public void clearAll() {
        days.clear();
    }

    public long numberSet() {
        return days.cardinality();
    }

    private int offsetFor(final TramDate date) {
        final long day = date.toEpochDay();
        if ((day< beginningDay) || (day>(beginningDay+size))) {
            throw new RuntimeException(format("Date %s (day %s) is out of range for %s", date, day, beginningDay));
        }
        final long diff = Math.subtractExact(day, beginningDay);
        return Math.toIntExact(diff);
    }

    public void set(final TramDate date) {
        final int offset = offsetFor(date);
        if (offset>=size) {
            throw new RuntimeException(format("Attempt to set date out of range %s for %s", date, this));
        }
        days.set(offset);
    }

    public void clear(final TramDate date) {
        final int offset = offsetFor(date);
        days.clear(offset);
    }

    @Override
    public boolean isSet(final TramDate date) {
        final long epochDay = date.toEpochDay();
        if (epochDay<beginningDay || epochDay>(beginningDay+size)) {
            return false;
        }
        final int offset = offsetFor(date);
        return days.get(offset);
    }

    public boolean noneSet() {
        return days.cardinality()==0;
    }

    public boolean anyOverlap(final DaysBitmap other) {

        if ( ! (this.contains(other) || other.contains(this)) ) {
            return false;
        }

        final BitSet firstOverlap = other.createOverlapWith(this);
        final BitSet secondOverlap = this.createOverlapWith(other);

        final boolean result = firstOverlap.intersects(secondOverlap);

        firstOverlap.clear();
        secondOverlap.clear();

        return result;
    }

    @Override
    public boolean contains(final DaysBitmap other) {
        if (dayWithin(other.getBeginningEpochDay())) {
            return true;
        }

        long otherEndDay = other.getBeginningEpochDay() + other.size();
        return dayWithin(otherEndDay);
    }

    private boolean dayWithin(final long epochDay) {
        final long endDay = beginningDay + size;
        return epochDay>=beginningDay && epochDay<=endDay;
    }

    @Override
    public BitSet createOverlapWith(final DaysBitmap other) {
        final long otherBeginningDay = other.getBeginningEpochDay();
        final long startOfOverlap = otherBeginningDay < this.beginningDay ? 0 : otherBeginningDay-this.beginningDay;

        final long endOfThis = this.beginningDay + size;
        final long endOfOther = otherBeginningDay + other.size();

        final long size = endOfOther > endOfThis ? this.size : this.size - Math.subtractExact(endOfThis, endOfOther);

        final int start = Math.toIntExact(startOfOverlap);
        final int end = Math.toIntExact(startOfOverlap+size);

        return days.get(start, end);
    }

    @Override
    public long getBeginningEpochDay() {
        return beginningDay;
    }

    @Override
    public IntStream streamDays() {
        return days.stream();
    }

    @Override
    public int size() {
        return size;
    }

    public void insert(final DaysBitmap other) {
        final int offset = Math.toIntExact(Math.subtractExact(other.getBeginningEpochDay(), beginningDay));

        // todo not safe if runs past end, bit will silently extend the length
        other.streamDays().map(setBit -> setBit+offset).forEach(days::set);
    }

    @Override
    public String toString() {
        return "DaysBitmap{" +
                "beginningDay=" + beginningDay +
                ", size=" + size +
                ", days=" + days +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableDaysBitmap that = (MutableDaysBitmap) o;
        return beginningDay == that.beginningDay && size == that.size && days.equals(that.days);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beginningDay, days, size);
    }
}
