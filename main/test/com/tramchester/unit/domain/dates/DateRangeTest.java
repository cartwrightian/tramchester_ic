package com.tramchester.unit.domain.dates;

import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class DateRangeTest {

    @Test
    void shouldCreateCorrectly() {
        TramDate startDate = TramDate.of(2014, 10, 5);
        TramDate endDate = TramDate.of(2014, 12, 25);

        DateRange range = DateRange.of(startDate, endDate);
        assertEquals(startDate, range.getStartDate());
        assertEquals(endDate, range.getEndDate());
    }

    @Test
    void shouldCheckContainedCorrectly() {
        TramDate startDate = TramDate.of(2014, 10, 5);
        TramDate endDate = TramDate.of(2014, 12, 25);

        DateRange range = DateRange.of(startDate, endDate);

        assertTrue(range.contains(startDate));
        assertTrue(range.contains(endDate));

        TramDate date = startDate;
        while (date.isBefore(endDate)) {
            assertTrue(range.contains(date), "contains " + date);
            date = date.plusDays(1);
        }

        assertFalse(range.contains(startDate.minusDays(1)));
        assertFalse(range.contains(endDate.plusDays(1)));

    }

    @Test
    void shouldHaveOverlapAllDays() {
        TramDate startDate = TramDate.of(2014, 10, 5);
        TramDate endDate = TramDate.of(2014, 12, 25);

        DateRange dateRange = DateRange.of(startDate, endDate);
        assertTrue(dateRange.overlapsWith(DateRange.of(startDate, endDate)));

        assertTrue(dateRange.overlapsWith(DateRange.of(startDate.minusDays(1), startDate.plusDays(1))));
        assertTrue(dateRange.overlapsWith(DateRange.of(endDate.minusDays(1), endDate.plusDays(1))));
        assertTrue(dateRange.overlapsWith(DateRange.of(startDate.minusDays(1), endDate.plusDays(1))));
        assertTrue(dateRange.overlapsWith(DateRange.of(startDate.minusDays(1), endDate.plusDays(1))));
        assertTrue(dateRange.overlapsWith(DateRange.of(startDate.plusDays(1), endDate.minusDays(1))));

        assertFalse(dateRange.overlapsWith(DateRange.of(endDate.plusDays(2), endDate.plusDays(3))));
        assertFalse(dateRange.overlapsWith(DateRange.of(startDate.minusDays(3), startDate.minusDays(2))));
    }

    @Test
    void shouldHaveNumberOfDaysInclusive() {
        TramDate startDate = TramDate.of(2022, 9, 1);
        TramDate endDate = TramDate.of(2022, 9, 30);

        DateRange dateRange = DateRange.of(startDate, endDate);

        assertEquals(30, dateRange.numberOfDays());
    }

    @Test
    void shouldHaveNumberOfDaysInclusiveOneDay() {
        TramDate startDate = TramDate.of(2022, 11, 29);
        TramDate endDate = TramDate.of(2022, 11, 29);

        DateRange dateRange = DateRange.of(startDate, endDate);

        assertTrue(dateRange.contains(startDate));
        assertTrue(dateRange.contains(endDate));

        assertEquals(1, dateRange.numberOfDays());
    }

    @Test
    void shouldHaveStreamOfRangeOneDays() {
        TramDate startDate = TramDate.of(2022, 1,1);

        Set<TramDate> fromStream = DateRange.of(startDate, startDate).stream().collect(Collectors.toSet());

        assertEquals(1, fromStream.size());

        assertTrue(fromStream.contains(startDate));
    }

    @Test
    void shouldHaveStreamOfRangeOfDays() {
        TramDate startDate = TramDate.of(2022, 1,1);
        TramDate endDate = TramDate.of(2022, 1, 31);

        Set<TramDate> fromStream = DateRange.of(startDate, endDate).stream().collect(Collectors.toSet());

        int expectedSize = 31;
        assertEquals(expectedSize, fromStream.size());

        for (int i = 0; i < expectedSize; i++) {
            TramDate expected = startDate.plusDays(i);
            assertTrue(fromStream.contains(expected), "missing " +  expected + " for " + i);
        }
    }

    @Disabled("Performance testing only")
    @RepeatedTest(100)
    void performanceOfIncludes() {
        final int days = 400;
        TramDate startDate = TestEnv.testDay();
        TramDate endDate = startDate.plusDays(days);

        DateRange range = DateRange.of(startDate, endDate);

        for (int i = 0; i < 100000000; i++) {
            int day = ThreadLocalRandom.current().nextInt(0, days);
            assertTrue(range.contains(startDate.plusDays(day)));
        }
    }


}
