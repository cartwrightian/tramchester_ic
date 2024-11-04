package com.tramchester.unit.domain;

import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;


class GenerateExampleDatesTest {

    @Test
    void shouldGenerateCorrectDaysForTests() {
        Assertions.assertNotEquals(DayOfWeek.SATURDAY, TestEnv.testDay().getDayOfWeek());
        Assertions.assertNotEquals(DayOfWeek.SUNDAY, TestEnv.testDay().getDayOfWeek());
        Assertions.assertNotEquals(DayOfWeek.MONDAY, TestEnv.testDay().getDayOfWeek());

        Assertions.assertEquals(DayOfWeek.MONDAY, UpcomingDates.nextMonday().getDayOfWeek());
        Assertions.assertEquals(DayOfWeek.SATURDAY, UpcomingDates.nextSaturday().getDayOfWeek());
        Assertions.assertEquals(DayOfWeek.SUNDAY, UpcomingDates.nextSunday().getDayOfWeek());
    }
}
