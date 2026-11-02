package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.dataimport.loader.TransportDataReader;
import com.tramchester.dataimport.loader.TransportDataReaderFactory;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.ServiceCalendar;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.dates.TramDateSet;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.testTags.DataExpiryTest;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.MultiMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.repository.Interchanges.WEEKS_AHEAD_FOR_LINKS;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@MultiMode
@DataUpdateTest
public class ServiceRepositoryTest {

    private static GuiceContainerDependencies componentContainer;
    private static TramchesterConfig config;

    private ServiceRepository serviceRepository;
    private TramDate when;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        config = tramchesterConfig;
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        serviceRepository = componentContainer.get(ServiceRepository.class);
        when = TestEnv.testDay();
    }

    @Test
    void shouldGetServicesByDate() {
        TramDate nextSaturday = UpcomingDates.nextSaturday();
        Set<Service> results = serviceRepository.getServicesOnDate(nextSaturday, TransportMode.TramsOnly);

        assertFalse(results.isEmpty(), "no services next saturday");
        long onCorrectDate = results.stream().
                filter(svc -> svc.getCalendar().operatesOn(nextSaturday)).count();

        assertEquals(results.size(), onCorrectDate, "should all be on the specified date");

        TramDate noTrams = nextSaturday.plusWeeks(11*52);
        results = serviceRepository.getServicesOnDate(noTrams, TransportMode.TramsOnly);
        assertTrue(results.isEmpty(), "not empty, got " + results);
    }


    @DataExpiryTest
    @Test
    void shouldHaveServiceEndDatesBeyondNextNDays() {

//        TramDate startDate = TramDate.from(TestEnv.LocalNow());
//        TramDate endDate = startDate.plusDays(DAYS_AHEAD);

        DateRange dateRange = DateRange.from(UpcomingDates.daysAhead());

        Set<Service> services = serviceRepository.getServices();
        Set<Service> expiringServices = services.stream().
                filter(service -> !service.getCalendar().getDateRange().overlapsWith(dateRange)).
                collect(Collectors.toSet());

        assertNotEquals(services, expiringServices, "all services are expiring");
    }

    @DataExpiryTest
    @Test
    void shouldHaveTramServicesAvailableNDaysAhead() {

        Set<TramDate> noServices = UpcomingDates.getUpcomingDates().
                filter(date -> serviceRepository.getServicesOnDate(date, TransportMode.TramsOnly).isEmpty()).
                collect(Collectors.toSet());

        assertTrue(noServices.isEmpty(), "no services on " + noServices);

    }

    @Test
    void shouldHaveServicesThatIncludeDateInRange() {

        Set<Service> allServices = serviceRepository.getServices();

        Set<Service> includeDate = allServices.stream().filter(service -> service.getCalendar().getDateRange().contains(when)).collect(Collectors.toSet());

        assertFalse(includeDate.isEmpty());

        Set<Service> onActualDate = includeDate.stream().filter(service -> service.getCalendar().operatesOn(when)).collect(Collectors.toSet());

        assertFalse(onActualDate.isEmpty());
    }

    @Test
    void shouldBeApplyingExceptionalDatesCorrectly() {

        TransportDataReaderFactory dataReaderFactory = componentContainer.get(TransportDataReaderFactory.class);
        List<TransportDataReader> transportDataReaders = dataReaderFactory.getReaders();
        TransportDataReader transportDataReader = transportDataReaders.getFirst(); // yuk
        Set<CalendarDateData> calendarsDates = transportDataReader.getCalendarDates().collect(Collectors.toSet());

        Set<Service> calendarsForTrams = serviceRepository.getServices();

        Set<CalendarDateData> applyToCurrentServices = calendarsDates.stream().
                filter(calendarDateData -> serviceRepository.hasServiceId(calendarDateData.getServiceId())).
                collect(Collectors.toSet());

        // are not any in the data at the moment 15/11/2022
        assertFalse(applyToCurrentServices.isEmpty(), "did not alter any of " + HasId.asIds(calendarsForTrams));

        assertEquals(1,  config.getGtfsSourceConfig().size(), "expected only one data source");
        GTFSSourceConfig sourceConfig = config.getGtfsSourceConfig().getFirst();
        TramDateSet excludedByConfig = TramDateSet.of(sourceConfig.getNoServices());

        applyToCurrentServices.forEach(exception -> {
            Service service = serviceRepository.getServiceById(exception.getServiceId());
            ServiceCalendar calendar = service.getCalendar();

            TramDate exceptionDate = exception.getDate();
            int exceptionType = exception.getExceptionType();
            if (exceptionType == CalendarDateData.ADDED) {
                if (excludedByConfig.contains(exceptionDate)) {
                    assertFalse(calendar.operatesOn(exceptionDate));
                } else {
                    assertTrue(calendar.operatesOn(exceptionDate));
                }
            } else if (exceptionType == CalendarDateData.REMOVED) {
                assertFalse(calendar.operatesOn(exceptionDate));
            }
        });
    }

    @Test
    void shouldServicesOverlappingWithUpcomingPeriod() {

        TramDate today = TramDate.of(LocalDate.now());
        DateRange dateRange = DateRange.of(today, today.plusWeeks(WEEKS_AHEAD_FOR_LINKS));

        IdSet<Service> allSvcs = serviceRepository.getServices().stream().
                filter(service -> service.getCalendar().getDateRange().getEndDate().isAfter(today)).
                collect(IdSet.collector());

        allSvcs.forEach(id -> {
            boolean expect = !serviceRepository.getServiceById(id).getCalendar().operatesNoDays();
            assertEquals(expect, serviceRepository.anyOverlaps(IdSet.singleton(id), dateRange), "No overlap for " + id);
        });

        assertTrue(serviceRepository.anyOverlaps(allSvcs, dateRange));
    }

    @Test
    void shouldNotHaveOverlapInThePast() {
        DateRange inThePast = DateRange.of(TramDate.of(1999, 1, 25), TramDate.of(1999 ,1 ,26));
        IdSet<Service> allSvcs = serviceRepository.getServices().stream().collect(IdSet.collector());
        assertFalse(serviceRepository.anyOverlaps(allSvcs, inThePast));
    }
}
