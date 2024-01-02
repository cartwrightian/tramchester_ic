package com.tramchester.unit.domain;

import com.tramchester.domain.dates.*;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MutableExceptionsOnlyServiceCalendarTest {

    // TODO CalendarDates seen but no calendar??

    @Test
    void shouldSetIncludeDates() {

        TramDate startDate = TramDate.of(2014, 10, 5);
        TramDate endDate = TramDate.of(2014, 12, 25);

        MutableExceptionsOnlyServiceCalendar serviceCalendar = new MutableExceptionsOnlyServiceCalendar();
        serviceCalendar.includeExtraDate(startDate);
        serviceCalendar.includeExtraDate(endDate);

        assertTrue(serviceCalendar.operatesOn(startDate));
        assertTrue(serviceCalendar.operatesOn(endDate), "not operating " + serviceCalendar);
        assertFalse(serviceCalendar.operatesOn(TramDate.of(2014, 11, 30)));

        assertFalse(serviceCalendar.operatesOn(TramDate.of(2016, 11, 30)));
        assertFalse(serviceCalendar.operatesOn(startDate.minusDays(1)));
        assertFalse(serviceCalendar.operatesOn(endDate.plusDays(1)));

        DaysBitmap daysBitmap = serviceCalendar.getDaysBitmap();
        assertEquals(startDate.toEpochDay(), daysBitmap.getBeginningEpochDay());

        long diff = endDate.toEpochDay() - startDate.toEpochDay();
        assertEquals(diff+1, daysBitmap.size());

        assertTrue(daysBitmap.isSet(startDate));
        assertTrue(daysBitmap.isSet(endDate));

        assertFalse(daysBitmap.isSet(startDate.plusDays(1)));
        assertFalse(daysBitmap.isSet(endDate.minusDays(1)));

        Set<Long> inBitmap = daysBitmap.streamDays().mapToLong(anInt -> anInt).boxed().collect(Collectors.toSet());
        assertTrue(inBitmap.contains(0L), inBitmap.toString());
        assertTrue(inBitmap.contains(diff), inBitmap.toString());
        assertEquals(2, inBitmap.size());

    }

    @Test
    void shouldCancel() {

        MutableServiceCalendar serviceCalendar = new MutableExceptionsOnlyServiceCalendar();

        TramDate date = TramDate.of(2014, 11, 30);

        serviceCalendar.includeExtraDate(date);

        assertTrue(serviceCalendar.operatesOn(date));

        serviceCalendar.cancel();

        assertTrue(serviceCalendar.operatesNoDays());
    }


    @Test
    void shouldCheckIfServiceHasExceptionDatesRemoved() {
        MutableServiceCalendar serviceCalendar = new MutableExceptionsOnlyServiceCalendar();

        TramDate queryDate = TramDate.of(2020, 12, 1);
        assertFalse(serviceCalendar.operatesOn(queryDate));
        serviceCalendar.excludeDate(queryDate);
        assertFalse(serviceCalendar.operatesOn(queryDate));
    }

    @Test
    void shouldHaveExpectedDateRangeWhenOnlyIncludedPresent() {
        MutableServiceCalendar serviceCalendar = new MutableExceptionsOnlyServiceCalendar();

        TramDate dateA = TramDate.of(2023, 1, 15);
        serviceCalendar.includeExtraDate(dateA);

        DateRange rangeA = serviceCalendar.getDateRange();
        assertEquals(dateA, rangeA.getStartDate());
        assertEquals(dateA, rangeA.getEndDate());

        TramDate dateB = TramDate.of(2023, 2, 15);
        serviceCalendar.includeExtraDate(dateB);
        DateRange rangeB = serviceCalendar.getDateRange();
        assertEquals(dateA, rangeB.getStartDate());
        assertEquals(dateB, rangeB.getEndDate());
    }

    @Test
    void shouldHaveExpectedDateRangeWhenOnlyExcludedPresent() {
        MutableServiceCalendar serviceCalendar = new MutableExceptionsOnlyServiceCalendar();

        TramDate dateA = TramDate.of(2023, 1, 15);
        serviceCalendar.excludeDate(dateA);

        DateRange rangeA = serviceCalendar.getDateRange();
        assertEquals(dateA, rangeA.getStartDate());
        assertEquals(dateA, rangeA.getEndDate());

        TramDate dateB = TramDate.of(2023, 2, 15);
        serviceCalendar.excludeDate(dateB);
        DateRange rangeB = serviceCalendar.getDateRange();
        assertEquals(dateA, rangeB.getStartDate());
        assertEquals(dateB, rangeB.getEndDate());

        DaysBitmap bitmap = serviceCalendar.getDaysBitmap();

        assertEquals(dateA.toEpochDay(),bitmap.getBeginningEpochDay());
        assertEquals(0, bitmap.size());
        assertTrue(bitmap.streamDays().boxed().collect(Collectors.toSet()).isEmpty());
    }

    @Test
    void shouldHaveExpectedDateRangeWhenNoDatesPresent() {
        MutableServiceCalendar serviceCalendar = new MutableExceptionsOnlyServiceCalendar();

        assertTrue(serviceCalendar.getDateRange().isEmpty());
    }

    @Test
    void shouldCheckForOverlaps() {
        MutableServiceCalendar serviceCalendarA = new MutableExceptionsOnlyServiceCalendar();
        MutableServiceCalendar serviceCalendarB = new MutableExceptionsOnlyServiceCalendar();

        assertFalse(serviceCalendarA.anyDateOverlaps(serviceCalendarB));

        serviceCalendarA.includeExtraDate(TramDate.of(2023,1, 5));
        serviceCalendarA.includeExtraDate(TramDate.of(2023,1,30));
        serviceCalendarB.includeExtraDate(TramDate.of(2023,2, 2));

        assertFalse(serviceCalendarA.anyDateOverlaps(serviceCalendarB));

        serviceCalendarB.includeExtraDate(TramDate.of(2023, 1, 15));

        assertTrue(serviceCalendarA.anyDateOverlaps(serviceCalendarB));

    }

    @Test
    void shouldCheckIfServiceHasExceptionDatesAdded() {

        TramDate testDay = TestEnv.testDay();

        TramDate startDate = testDay.minusWeeks(1);

        DayOfWeek dayOfWeek = testDay.getDayOfWeek();

        MutableServiceCalendar serviceCalendar = new MutableExceptionsOnlyServiceCalendar();

        TramDate additional = startDate.plusDays(1);
        assertFalse(serviceCalendar.operatesOn(additional));

        // same day
        serviceCalendar.includeExtraDate(additional);
        assertTrue(serviceCalendar.operatesOn(additional));

        // different day - TODO GTFS spec really not so clean on this, but assume we should allow as specifically included
        TramDate outsidePeriodDiffDayOfWeek = additional.plusDays(1);
        assertNotEquals(dayOfWeek, outsidePeriodDiffDayOfWeek.getDayOfWeek());

        serviceCalendar.includeExtraDate(outsidePeriodDiffDayOfWeek);
        assertTrue(serviceCalendar.operatesOn(outsidePeriodDiffDayOfWeek));
    }

    @Test
    void shouldHaveNumberOfDaysOperatingAdditional() {
        TramDate startDate = TramDate.of(2022, 8, 1); // a monday

        MutableServiceCalendar calendar = new MutableExceptionsOnlyServiceCalendar();
        assertEquals(0, calendar.numberDaysOperating());

        calendar.includeExtraDate(startDate);
        assertEquals(1, calendar.numberDaysOperating());

        calendar.excludeDate(startDate.plusDays(1));
        assertEquals(1, calendar.numberDaysOperating());

    }

    @Test
    void shouldHaveNumberOfDaysExcludeAndAdditional() {
        TramDate startDate = TramDate.of(2022, 8, 1); // a monday
        TramDate endDate = TramDate.of(2022, 8, 14);

        MutableServiceCalendar rangeForMondays = new MutableExceptionsOnlyServiceCalendar();

        rangeForMondays.excludeDate(startDate);
        assertEquals(0, rangeForMondays.numberDaysOperating());

        rangeForMondays.includeExtraDate(startDate.plusDays(1));
        assertEquals(1, rangeForMondays.numberDaysOperating());


    }


}
