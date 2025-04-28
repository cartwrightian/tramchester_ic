package com.tramchester.domain.dates;

import java.io.PrintStream;
import java.util.BitSet;
import java.util.stream.IntStream;

public class EmptyServiceCalendar implements ServiceCalendar {

    private static final DaysBitmap emptyBitmap = new EmptyDaysBitmap();

    @Override
    public boolean operatesOn(TramDate queryDate) {
        return false;
    }

    @Override
    public void summariseDates(PrintStream printStream) {
        printStream.println("EMPTY");
    }

    @Override
    public DateRange getDateRange() {
        return DateRange.Empty();
    }

    @Override
    public boolean operatesNoDays() {
        return true;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean anyDateOverlaps(ServiceCalendar other) {
        return false;
    }

    @Override
    public long numberDaysOperating() {
        return 0;
    }

    @Override
    public DaysBitmap getDaysBitmap() {
        return emptyBitmap;
    }

    @Override
    public boolean hasAddition() {
        return false;
    }

    private static class EmptyDaysBitmap implements DaysBitmap {
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
            return BitSet.valueOf(new byte[]{});
        }

        @Override
        public long getBeginningEpochDay() {
            return 0;
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
