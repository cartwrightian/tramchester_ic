package com.tramchester.domain.dates;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;

public class AggregateServiceCalendar implements ServiceCalendar {

    private final EnumSet<DayOfWeek> aggregatedDays;
    private final MutableDaysBitmap days;

    // for diagnostics only
    private final Collection<ServiceCalendar> sources;

    private boolean hasAdditional;
    private final boolean cancelled;
    private final DateRange aggregatedRange;

    public AggregateServiceCalendar(Collection<ServiceCalendar> calendars) {

        if (calendars.isEmpty()) {
            throw new RuntimeException("Cannot create aggregate for no calendars");
        }

        aggregatedRange = calculateDateRange(calendars);
        cancelled = calendars.stream().allMatch(ServiceCalendar::isCancelled);

        sources = calendars;
        aggregatedDays = EnumSet.noneOf(DayOfWeek.class);

        days = createDaysBitset(aggregatedRange);

        hasAdditional = false;

        calendars.forEach(calendar -> {
            setDaysFor(calendar);
            aggregatedDays.addAll(calendar.getOperatingDays());
            hasAdditional = hasAdditional || calendar.hasAddition();
        });
    }

    private MutableDaysBitmap createDaysBitset(DateRange dateRange) {
        long earliest = dateRange.getStartDate().toEpochDay();
        long latest = dateRange.getEndDate().toEpochDay();

        int size = Math.toIntExact(Math.subtractExact(latest, earliest));

        return new MutableDaysBitmap(earliest, size);
    }

    private void setDaysFor(ServiceCalendar calendar) {
        days.insert(calendar.getDaysBitmap());
    }

    private static DateRange calculateDateRange(final Collection<ServiceCalendar> calendars) {
        if (calendars.isEmpty()) {
            throw new RuntimeException("No calendars supplied");
        }
        final Optional<TramDate> begin = calendars.stream().map(calendar -> calendar.getDateRange().getStartDate()).
                reduce(AggregateServiceCalendar::earliest);

        final Optional<TramDate> end = calendars.stream().map(calendar -> calendar.getDateRange().getEndDate()).
                reduce(AggregateServiceCalendar::latest);

        if (begin.isPresent() && end.isPresent()) {
            return DateRange.of(begin.get(), end.get());
        } else {
            throw new RuntimeException("Unable to derive a valid date range from supplier calendars " + calendars);
        }
    }

    private static TramDate earliest(final TramDate a, final TramDate b) {
        if (a.isBefore(b)) {
            return a;
        } else {
            return b;
        }
    }

    private static TramDate latest(final TramDate a, final TramDate b) {
        if (a.isAfter(b)) {
            return a;
        } else {
            return b;
        }
    }

    @Override
    public boolean anyDateOverlaps(ServiceCalendar other) {
        return this.days.anyOverlap(other.getDaysBitmap());
    }

    @Override
    public long numberDaysOperating() {
        return days.numberSet();
    }

    @Override
    public boolean operatesOn(final TramDate date) {
        if (aggregatedRange.contains(date)) {
            return days.isSet(date);
        }
        return false;
    }

    @Override
    public DateRange getDateRange() {
        return aggregatedRange;
    }

    @Override
    public boolean operatesNoDays() {
       return days.numberSet()==0;
    }

    @Override
    public EnumSet<DayOfWeek> getOperatingDays() {
        return aggregatedDays;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void summariseDates(PrintStream printStream) {
        if (cancelled) {
            printStream.print("CANCELLED: ");
        }
        printStream.printf("%s days %s%n", getDateRange(), reportDays());
        printStream.println("source calendars: " + sources.toString());
//        if (!additional.isEmpty()) {
//            printStream.println("Additional on: " + additional);
//        }
//        if (!removed.isEmpty()) {
//            printStream.println("Not running on: " + removed);
//        }
    }

    private String reportDays() {
        if (aggregatedDays.isEmpty()) {
            return "SPECIAL/NONE";
        }

        if (cancelled) {
            return "CANCELLED";
        }

        StringBuilder found = new StringBuilder();
        aggregatedDays.forEach(dayOfWeek -> {
            if (!found.isEmpty()) {
                found.append(",");
            }
            found.append(dayOfWeek.name());
        });
        return found.toString();
    }

    @Override
    public DaysBitmap getDaysBitmap() {
        return days;
    }

    @Override
    public boolean hasAddition() {
        return hasAdditional;
    }

    @Override
    public String toString() {
        return "AggregateServiceCalendar{" +
                "aggregatedDays=" + aggregatedDays +
                ", cancelled=" + cancelled +
                ", aggregatedRange=" + aggregatedRange +
                ", days=" + days +
                '}';
    }
}
