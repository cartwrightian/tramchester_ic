package com.tramchester.domain.dates;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.stream.IntStream;

public class MutableExceptionsOnlyServiceCalendar implements MutableServiceCalendar {

    private final TramDateSet additional;
    private final TramDateSet removed;

    private boolean cancelled;

    public MutableExceptionsOnlyServiceCalendar() {
        additional = new TramDateSet();
        removed = new TramDateSet();
    }

    @Override
    public void cancel() {
        cancelled = true;
    }

    // TODO not clear what takes precedence here? Include or Exclude, or should this not happen??

    @Override
    public void includeExtraDate(TramDate date) {
        additional.add(date);
    }

    @Override
    public void excludeDate(TramDate date) {
        removed.add(date);
    }

    @Override
    public boolean operatesOn(TramDate queryDate) {
        if (removed.contains(queryDate)) {
            return false;
        }
        return additional.contains(queryDate);
    }

    @Override
    public void summariseDates(PrintStream printStream) {
        if (cancelled) {
            printStream.print("CANCELLED: ");
        }
        printStream.printf("%s days %s%n", getDateRange(), additional.getDays());
        if (!additional.isEmpty()) {
            printStream.println("Additional on: " + additional);
        }
        if (!removed.isEmpty()) {
            printStream.println("Not running on: " + removed);
        }
    }

    @Override
    public DateRange getDateRange() {
        if (removed.isEmpty() && additional.isEmpty()) {
            return DateRange.Empty();
        } else if (removed.isEmpty()) {
            return DateRange.of(additional.first(), additional.last());
        } else if (additional.isEmpty()) {
            return DateRange.of(removed.first(), removed.last());
        } else {
            TramDate earliest = TramDate.min(removed.first(), additional.first());
            TramDate latest = TramDate.max(removed.last(), additional.last());
            return DateRange.of(earliest, latest);
        }
    }

    @Override
    public boolean operatesNoDays() {
        if (cancelled) {
            return true;
        }
        return additional.isEmpty();
    }

    @Override
    public EnumSet<DayOfWeek> getOperatingDays() {
        if (cancelled) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        return additional.getDays();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean anyDateOverlaps(ServiceCalendar other) {
       return this.getDateRange().overlapsWith(other.getDateRange());
    }

    @Override
    public long numberDaysOperating() {
        return additional.size();
    }

    @Override
    public DaysBitmap getDaysBitmap() {
        if (additional.isEmpty()) {
            if (removed.isEmpty()) {
                throw new RuntimeException("Fully empty, cannot create bitmap");
            }
            return new NoneSetDaysBitmap(removed.first());
        }

        long start = additional.first().toEpochDay();
        long end = additional.last().toEpochDay();
        long diff = (end-start) + 1;
        MutableDaysBitmap bitmap = new MutableDaysBitmap(start, Math.toIntExact(diff));
        additional.forEach(bitmap::set);
        return bitmap;
    }

    @Override
    public String toString() {
        return "MutableExceptionsOnlyServiceCalendar{" +
                "additional=" + additional +
                ", removed=" + removed +
                ", cancelled=" + cancelled +
                '}';
    }

    private static class NoneSetDaysBitmap implements DaysBitmap {
        private final long start;

        public NoneSetDaysBitmap(TramDate start) {
            this.start = start.toEpochDay();
        }

        @Override
        public boolean isSet(TramDate date) {
            return false;
        }

        @Override
        public boolean contains(DaysBitmap other) {
            return false;
        }

        @Override
        public BitSet createOverlapWith(DaysBitmap other) {
            return new BitSet(0);
        }

        @Override
        public long getBeginningEpochDay() {
            return start;
        }

        @Override
        public IntStream streamDays() {
            return IntStream.empty();
        }

        @Override
        public int size() {
            return 0;
        }
    }
}
