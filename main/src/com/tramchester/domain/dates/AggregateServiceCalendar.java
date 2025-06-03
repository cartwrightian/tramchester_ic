package com.tramchester.domain.dates;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AggregateServiceCalendar implements ServiceCalendar {

    private final MutableDaysBitmap days;

    // for diagnostics only
    private final Collection<ServiceCalendar> sources;

    private boolean hasAdditional;
    private final boolean cancelled;
    private final DateRange aggregatedRange;

    public AggregateServiceCalendar(final Collection<ServiceCalendar> calendars) {

        if (calendars.isEmpty()) {
            throw new RuntimeException("Cannot create aggregate for no calendars");
        }

        aggregatedRange = calculateDateRange(calendars);
        cancelled = calendars.stream().allMatch(ServiceCalendar::isCancelled);

        sources = calendars;

        days = createDaysBitset(aggregatedRange);

        hasAdditional = false;

        calendars.forEach(calendar -> {
            setDaysFor(calendar);
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
        return days.isSet(date);
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
    }

    private String reportDays() {

        final Set<DayOfWeek> aggregatedDays = getDateRange().stream().
                filter(days::isSet).
                map(TramDate::getDayOfWeek).collect(Collectors.toSet());

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
                ", cancelled=" + cancelled +
                ", aggregatedRange=" + aggregatedRange +
                ", days=" + days +
                '}';
    }
}
