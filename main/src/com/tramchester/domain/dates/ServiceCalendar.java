package com.tramchester.domain.dates;

import java.io.PrintStream;

public interface ServiceCalendar {

    boolean operatesOn(TramDate queryDate);

    void summariseDates(PrintStream printStream);

    /***
     * Range of dates (from data source) given for this service. NOTE: service might not actually operate on
     * any of these dates depending on removed, additional and operating days
     * @return range for calendar
     */
    DateRange getDateRange();

    /***
     * True iff does not operate on any days whatsoever, takes account of additional days
     * @return true if service NEVER operates
     */
    boolean operatesNoDays();

    boolean isCancelled();

    boolean anyDateOverlaps(ServiceCalendar other);

    long numberDaysOperating();

    DaysBitmap getDaysBitmap();

    boolean hasAddition();
}
