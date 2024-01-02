package com.tramchester.domain.dates;

import com.tramchester.dataimport.data.CalendarData;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.util.*;

public class MutableNormalServiceCalendar implements MutableServiceCalendar {
    private final DateRange dateRange;
    private final EnumSet<DayOfWeek> operatingDays;
    private final MutableDaysBitmap days;

    // for diagnosis only
    private final TramDateSet additional;
    private final TramDateSet removed;

    private boolean cancelled;

    public MutableNormalServiceCalendar(CalendarData calendarData) {
        this(calendarData.getDateRange(),
                daysOfWeekFrom(calendarData.isMonday(),
                calendarData.isTuesday(),
                calendarData.isWednesday(),
                calendarData.isThursday(),
                calendarData.isFriday(),
                calendarData.isSaturday(),
                calendarData.isSunday()));
    }

    public MutableNormalServiceCalendar(TramDate startDate, TramDate endDate, DayOfWeek... operatingDays) {
        this(new DateRange(startDate, endDate), enumFrom(operatingDays));
    }

    public MutableNormalServiceCalendar(DateRange dateRange, EnumSet<DayOfWeek> operatingDays) {
        this.dateRange = dateRange;
        this.operatingDays = operatingDays;
        additional = new TramDateSet();
        removed = new TramDateSet();
        cancelled = false;

        long firstEpochDay = dateRange.getStartDate().toEpochDay();

        int size = Math.toIntExact(dateRange.numberOfDays()); // will throw if overflow
        days = new MutableDaysBitmap(firstEpochDay,size);
        days.setDaysOfWeek(operatingDays);

    }

    private static EnumSet<DayOfWeek> enumFrom(DayOfWeek[] operatingDays) {
        return EnumSet.copyOf(Arrays.asList(operatingDays));
    }

    public void cancel() {
        cancelled = true;
        days.clearAll();
    }

    @Override
    public long numberDaysOperating() {
        return days.numberSet();
    }

    public void includeExtraDate(TramDate date) {
        additional.add(date);
        days.set(date);
    }

    public void excludeDate(TramDate date) {
        removed.add(date);
        days.clear(date);
    }

    @Override
    public boolean operatesOn(final TramDate date) {
        if (!dateRange.contains(date)) {
            return false;
        }
        return days.isSet(date);
    }

    @Override
    public void summariseDates(PrintStream printStream) {
        if (cancelled) {
            printStream.print("CANCELLED: ");
        }
        printStream.printf("%s days %s%n", dateRange, reportDays());
        if (!additional.isEmpty()) {
            printStream.println("Additional on: " + additional);
        }
        if (!removed.isEmpty()) {
            printStream.println("Not running on: " + removed);
        }
    }

    @Override
    public DateRange getDateRange() {
        return dateRange;
    }

    private String reportDays() {
        if (operatingDays.isEmpty()) {
            return "SPECIAL/NONE";
        }
        if (cancelled) {
            return "CANCELLED";
        }

        StringBuilder found = new StringBuilder();
        operatingDays.forEach(dayOfWeek -> {
            if (!found.isEmpty()) {
                found.append(",");
            }
            found.append(dayOfWeek.name());
        });
        return found.toString();
    }


    @Override
    public boolean operatesNoDays() {
        return cancelled || days.noneSet();
    }

    @Override
    public EnumSet<DayOfWeek> getOperatingDays() {
        return operatingDays;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public DaysBitmap getDaysBitmap() {
        return days;
    }

    @Override
    public boolean anyDateOverlaps(ServiceCalendar otherCalendar) {
        if (otherCalendar==null) {
            throw new RuntimeException("otherCalendar was null");
        }
//        HasDaysBitmap other = (HasDaysBitmap) otherCalendar;
//        //noinspection ConstantConditions
//        if (other==null) {
//            throw new RuntimeException("Cannot compute overlap as Not DaysAsBitmap " + otherCalendar.toString());
//        }

        return this.days.anyOverlap(otherCalendar.getDaysBitmap());
    }

    @Override
    public String toString() {
        return "MutableServiceCalendar{" +
                "dateRange=" + dateRange +
                ", operatingDays=" + operatingDays +
                ", additional=" + additional +
                ", removed=" + removed +
                ", cancelled=" + cancelled +
                ", days=" + days +
                '}';
    }

    private static EnumSet<DayOfWeek> daysOfWeekFrom(boolean monday, boolean tuesday,
                                                     boolean wednesday, boolean thursday, boolean friday, boolean saturday, boolean sunday)
    {
        HashSet<DayOfWeek> result = new HashSet<>();
        addIf(monday, DayOfWeek.MONDAY, result);
        addIf(tuesday, DayOfWeek.TUESDAY, result);
        addIf(wednesday, DayOfWeek.WEDNESDAY, result);
        addIf(thursday, DayOfWeek.THURSDAY, result);
        addIf(friday, DayOfWeek.FRIDAY, result);
        addIf(saturday, DayOfWeek.SATURDAY, result);
        addIf(sunday, DayOfWeek.SUNDAY, result);
        if (result.isEmpty()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        return EnumSet.copyOf(result);
    }

    private static void addIf(boolean flag, DayOfWeek dayOfWeek, HashSet<DayOfWeek> accumulator) {
        if (flag) {
            accumulator.add(dayOfWeek);
        }
    }


}
