package com.tramchester.domain.dates;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Stream;

public class TramDate implements Comparable<TramDate> {
    private final long epochDays;
    private final DayOfWeek dayOfWeek;

    private TramDate(final long epochDays) {
        this.epochDays = epochDays;
        this.dayOfWeek = calcDayOfWeek(epochDays);
    }

    public static TramDate of(final long epochDay) {
        return new TramDate(epochDay);
    }

    public static TramDate of(final LocalDate date) {
        return new TramDate(date.toEpochDay());
    }

    public static TramDate of(int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day);
        return new TramDate(date.toEpochDay());
    }

    public static TramDate from(final LocalDateTime localDateTime) {
        return of(localDateTime.toLocalDate());
    }

    public static TramDate min(final TramDate dateA, final TramDate dateB) {
        if (dateA.isBefore(dateB)) {
            return dateA;
        } else {
            return dateB;
        }
    }

    public static TramDate max(final TramDate dateA, final TramDate dateB) {
        if (dateA.isAfter(dateB)) {
            return dateA;
        } else {
            return dateB;
        }
    }

    // replicate LocalDate approach
    public DayOfWeek calcDayOfWeek(final long epochDays) {
        final int enumAsInt = Math.floorMod(epochDays + 3, 7);
        return DayOfWeek.of(enumAsInt + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TramDate tramDate = (TramDate) o;
        return epochDays == tramDate.epochDays;
    }

//    public boolean isEquals(TramDate other) {
//        return other.epochDays == epochDays;
//    }

    @Override
    public int hashCode() {
        return Objects.hash(epochDays);
    }

    public boolean isAfter(final TramDate other) {
        return this.epochDays>other.epochDays;
    }

    public boolean isBefore(final TramDate other) {
        return this.epochDays<other.epochDays;
    }

    public TramDate plusDays(final int days) {
        final long newDay = days + epochDays;
        return new TramDate(newDay);
    }

    public LocalDate toLocalDate() {
        return LocalDate.ofEpochDay(epochDays);
    }

    public TramDate minusDays(final int days) {
        final long newDay = epochDays - days;
        return new TramDate(newDay);
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public long toEpochDay() {
        return epochDays;
    }

    public String format(final DateTimeFormatter dateFormatter) {
        return LocalDate.ofEpochDay(epochDays).format(dateFormatter);
    }

    /***
     * format YYYYMMDD
     * @param text date text
     * @param offset offset to start of text
     * @return TramDate
     */
    public static TramDate parseSimple(final String text, final int offset) {
        final int year = parseFullYear(text, offset);
        final int month = parseTens(text, offset+4);
        final int day = parseTens(text, offset+6);
        return TramDate.of(year, month, day);
    }

    /***
     *
     * @param text text to parse in form YYMMDD
     * @param century century to add to the year
     * @param offset offset to start of text to parse
     * @return the TramDate
     */
    public static TramDate parseSimple(final String text, final int century, final int offset) {
        final int year = parseTens(text, offset);
        final int month = parseTens(text, offset+2);
        final int day = parseTens(text, offset+4);
        return TramDate.of((century*100) + year, month, day);
    }

    private static int parseTens(final String text, final int offset) {
        final char digit1 = text.charAt(offset);
        final char digit2 = text.charAt(offset+1);

        final int tens = Character.digit(digit1, 10);
        final int unit = Character.digit(digit2, 10);

        return (tens*10) + unit;
    }

    private static int parseFullYear(final String text, final int offset) {
        final char digit1 = text.charAt(offset);
        final char digit2 = text.charAt(offset+1);
        final char digit3 = text.charAt(offset+2);
        final char digit4 = text.charAt(offset+3);

        final int millenium = Character.digit(digit1, 10);
        final int century = Character.digit(digit2, 10);
        final int decade = Character.digit(digit3, 10);
        final int year = Character.digit(digit4, 10);

        return (millenium*1000) + (century*100) + (decade*10) + year;
    }

    // supports deserialization
    public static TramDate parse(final String text) {
        final LocalDate date = LocalDate.parse(text);
        return new TramDate(date.toEpochDay());
    }

    @Override
    public String toString() {
        LocalDate date = LocalDate.ofEpochDay(epochDays);
        return "TramDate{" +
                "epochDays=" + epochDays +
                ", dayOfWeek=" + dayOfWeek +
                ", date=" + date +
                '}';
    }

    public int compareTo(final TramDate other) {
        return Long.compare(this.epochDays, other.epochDays);
    }

    public TramDate minusWeeks(final int weeks) {
        return of(toLocalDate().minusWeeks(weeks));
    }

    public TramDate plusWeeks(final int weeks) {
        return of (toLocalDate().plusWeeks(weeks));
    }

    public Stream<TramDate> datesUntil(final TramDate endDate) {
        return toLocalDate().datesUntil(endDate.toLocalDate()).map(date -> new TramDate(date.toEpochDay()));
    }

    public boolean isEqual(final TramDate other) {
        return this.epochDays == other.epochDays;
    }

    public int getYear() {
        return toLocalDate().getYear();
    }

    public boolean isChristmasPeriod() {
        final LocalDate date = toLocalDate();
        final Month month = date.getMonth();
        final int day = date.getDayOfMonth();

        if (month==Month.DECEMBER && day>23) {
            return true;
        }
        if (month==Month.JANUARY && day<2) {
            return true;
        }
        return false;
    }

    public boolean isWeekend() {
        return (dayOfWeek==DayOfWeek.SATURDAY) || (dayOfWeek==DayOfWeek.SUNDAY);
    }

}
