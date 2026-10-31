package com.tramchester.acceptance.geolocation;

import com.tramchester.App;
import com.tramchester.acceptance.AppUserJourneyTest;
import com.tramchester.acceptance.FetchAllStationsFromAPI;
import com.tramchester.acceptance.UserJourneyTest;
import com.tramchester.acceptance.infra.AcceptanceAppExtenstion;
import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.App.Stage;
import com.tramchester.acceptance.pages.App.TestResultSummaryRow;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.acceptance.AppUserJourneyTest.desiredJourney;
import static com.tramchester.acceptance.AppUserJourneyTest.desiredJourneyFromMyLocation;
import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.NavigationRoad;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.*;


// NOTE: disabled on ci for now, geolocation is not working on circle ci for same firefox and gecko driver version

@DisabledIfEnvironmentVariable(named = "CIRCLECI", matches = "true")
@ExtendWith(DropwizardExtensionsSupport.class)
public class AppUserJourneyLocationsTest extends UserJourneyTest {

    private static final String configPath = AppUserJourneyTest.configPath;

    public static final AcceptanceAppExtenstion appExtension = new AcceptanceAppExtenstion(App.class, configPath);

    private final String bury = TramStations.Bury.getName();
    private final String altrincham = Altrincham.getName();

    private LocalDate when;
    private String url;

    @BeforeAll
    static void beforeAnyTestsRun() {
        createFactory(nearAltrincham.latLong());
    }

    @SuppressWarnings("unused")
    private static Stream<ProvidesDriver> getProvider() {
        return getProviderCommon();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        url = appExtension.getUrl()+"/app/index.html";
        when = TestEnv.testDay().toLocalDate();
    }

    @AfterEach
    void afterEachTestRuns(TestInfo testInfo) {
        takeScreenshotsFor(testInfo);
    }

    @AfterAll
    static void afterAllTestsRun() {
        closeFactory();
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldHaveCorrectNearbyStops(ProvidesDriver providesDriver) throws IOException {
        AppPage appPage = prepare(providesDriver);

        FetchAllStationsFromAPI fetchAllStationsFromAPI = new FetchAllStationsFromAPI(appExtension);

        Set<String> allStations = fetchAllStationsFromAPI.getStations().stream().
                map(LocationRefDTO::getName).
                collect(Collectors.toSet());

        //final int closed = fetchAllStationsFromAPI.getClosedStations().size();
        //final int expectedNumber = allStations-closed;

        assertTrue(appPage.hasLocation(), "geo enabled");
        assertTrue(appPage.searchEnabled());

        // from
        List<String> myLocationStops = appPage.getNearbyFromStops();
        assertEquals(1, myLocationStops.size(),"unexpected 1 got " + myLocationStops);

        List<String> nearestFromStops = appPage.getNearestFromStops();
        assertThat("Have nearest stops", nearestFromStops, hasItems(altrincham, NavigationRoad.getName()));

        List<String> fromStops = appPage.getAllStopsFromStops();
        assertFalse(fromStops.contains(altrincham));
        assertFalse(fromStops.contains(NavigationRoad.getName()));

        List<String> recentFromStops = appPage.getRecentFromStops();
        assertThat(fromStops, not(contains(recentFromStops)));

        Set<String> allFromStops = new HashSet<>(nearestFromStops);
        allFromStops.addAll(fromStops);
        allFromStops.addAll(recentFromStops);

        Set<String> mismatchFrom = SetUtils.disjunction(allStations, allFromStops);

        assertTrue(mismatchFrom.isEmpty(), "Got mismatch for from stops " + mismatchFrom);

        // to
        List<String> myLocationToStops = appPage.getNearbyToStops();
        assertEquals(1, myLocationToStops.size());

        List<String> nearestToStops = appPage.getNearestFromStops();
        assertThat(nearestToStops, hasItems(altrincham, NavigationRoad.getName()));

        List<String> toStops = appPage.getAllStopsToStops();
        assertThat(toStops, not(contains(nearestToStops)));

        List<String> recentToStops = appPage.getRecentToStops();

        Set<String> allToStops = new HashSet<>(nearestToStops);
        allToStops.addAll(toStops);
        allToStops.addAll(recentToStops);

        Set<String> mismatchTo = SetUtils.disjunction(allStations, allToStops);

        assertTrue(mismatchTo.isEmpty(), "Got mismatch for to stops " + mismatchTo);
        //assertEquals(allStations.size(), nearestToStops.size()+toStops.size()+recentToCount);

        // check recents works as expected
        desiredJourney(appPage, Altrincham, TramStations.Bury, when, TramTime.of(10,15), false);
        appPage.planAJourney();
        appPage.waitForReady();

        // set start/dest to some other stations
        appPage.setStart(TramStations.Piccadilly);
        appPage.setDest(TramStations.ManAirport);

        List<String> fromRecent = appPage.getRecentFromStops();
        assertThat(fromRecent, hasItems(altrincham, bury));
        nearestFromStops = appPage.getNearestFromStops();
        assertThat(nearestFromStops, hasItems(NavigationRoad.getName()));
        // TODO to recent just bury, not alty
    }

    // TODO Missing a test from a station to a location

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldCheckNearAltrinchamToDeansgate(ProvidesDriver providesDriver) throws IOException {
        AppPage appPage = prepare(providesDriver);

        assertTrue(appPage.hasLocation(), "geo enabled");

        TramTime planTime = TramTime.of(10,15);
        desiredJourneyFromMyLocation(appPage, TramStations.Deansgate, when, planTime, false);
        appPage.planAJourney();

        List<TestResultSummaryRow> results = appPage.getResults();
        // lockdown timetable: 3 -> 2
        assertTrue(results.size()>=2, "at least some results");

        // check timings are sane
        for (TestResultSummaryRow result : results) {
            TramTime departTime = result.getDepartTime();
            assertTrue(departTime.isValid());
            assertTrue(departTime.isAfter(planTime), departTime.toString());

            TramTime arriveTime = result.getArriveTime();
            assertTrue(arriveTime.isValid());
            assertTrue(arriveTime.isAfter(departTime), arriveTime.toString());
            String changes = result.getChanges();
            assertTrue(changes.equals(NavigationRoad.getName()) || changes.equals(Altrincham.getName()),
                    "Changes not as expected " + changes);
        }

        // select first journey - this seems to be inconsistent, not returning first displayed journey
        TestResultSummaryRow firstResult = results.getFirst();
        firstResult.moveTo(providesDriver);
        appPage.waitForClickable(firstResult.getElement());
        firstResult.click(providesDriver);

        List<Stage> stages = firstResult.getStages();
        assertEquals(2, stages.size());

        Stage firstStage = stages.get(0);
        Stage secondStage = stages.get(1);

        // walking stage
        final TramTime firstStageDepartTime = firstStage.getDepartTime();
        assertTrue(firstStageDepartTime.isValid());
        assertEquals("Walk to", firstStage.getAction(), "action wrong for " + stages);
        assertEquals(-1, firstStage.getPlatform(), "platform wrong for " + stages);
        //assertEquals("", firstStage.getRouteName(), "lineName");
        String station = firstStage.getActionStation();

        final TramTime secondStageDepartTime = secondStage.getDepartTime();
        assertTrue(secondStageDepartTime.isAfter(firstStageDepartTime));

        String expectedAction = "Board Tram";
//        String expectedAction = "Board Bus";

        assertEquals(expectedAction, secondStage.getAction(), "action wrong for " + stages);
        assertEquals(station, secondStage.getActionStation(), "action stations wrong");
    }

    private AppPage prepare(ProvidesDriver providesDriver) throws IOException {
        return prepare(providesDriver, url);
    }

}

