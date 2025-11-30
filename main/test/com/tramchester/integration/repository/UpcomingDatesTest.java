package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("WIP")
@DataUpdateTest
public class UpcomingDatesTest {
    private static GuiceContainerDependencies componentContainer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

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

//    @Disabled("WIP")
//    @Test
//    void shouldHaveExpectedClosures() {
//        List<IdFor<Station>> expectedClosed = stopCallRepository.getClosedBetween(Eccles.getId(), Broadway.getId());
//
//        DateRange range = UpcomingDates.MediaCityEcclesWorks2025;
//
//        range.stream().forEach(date -> {
//            IdSet<Station> missing = expectedClosed.stream().filter(closed -> !UpcomingDates.hasClosure(closed, date)).
//                    collect(IdSet.idCollector());
//            assertTrue(missing.isEmpty(), "On " + date + " still open " + missing);
//        });
//
//    }
}
