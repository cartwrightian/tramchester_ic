package com.tramchester.integration.repository;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static org.junit.jupiter.api.Assertions.*;

@DataUpdateTest
public class UpcomingDatesTest {

    @Test
    void expectDaysOfWeek() {
        assertEquals(DayOfWeek.MONDAY, UpcomingDates.nextMonday().getDayOfWeek());
        assertEquals(DayOfWeek.SATURDAY, UpcomingDates.nextSaturday().getDayOfWeek());
        assertEquals(DayOfWeek.SUNDAY, UpcomingDates.nextSunday().getDayOfWeek());
    }

    @Test
    void shouldHaveValidDates() {
        TramDate testDay = TestEnv.testDay();
        assertFalse(testDay.isChristmasPeriod());
        assertEquals(DayOfWeek.THURSDAY, testDay.getDayOfWeek());
        assertTrue(UpcomingDates.validTestDate(testDay));
    }

}
