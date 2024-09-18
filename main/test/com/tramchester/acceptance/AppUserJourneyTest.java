package com.tramchester.acceptance;

import com.tramchester.App;
import com.tramchester.acceptance.infra.AcceptanceAppExtenstion;
import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.App.Stage;
import com.tramchester.acceptance.pages.App.TestResultSummaryRow;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.resources.DataVersionResourceTest;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.SmokeTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.Cookie;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
public class AppUserJourneyTest extends UserJourneyTest {
    // Needs correct locale settings, see .circleci/config.yml setupLocale target
    // NOTE: This controls localAcceptance only, for CI acceptance tests run against the deployed dev instance

    // NOTE: to disable headless set env var export DISABLE_HEADLESS=true
    // NOTE: to run against local server (but not start one) then set export SERVER_URL=http://localhost:8080

    public static final String configPath = "config/localAcceptance.yml";

    private static final AcceptanceAppExtenstion appExtenstion = new AcceptanceAppExtenstion(App.class, configPath);

    private final TramStations bury = Bury;
    private final TramStations altrincham = Altrincham;
    private final TramStations deansgate = Deansgate;

    // useful consts, keep around as can swap when timetable changes
    //public static final String altyToBuryLineName = "Altrincham - Manchester - Bury";
    //public static final String altyToPicLineName = "Altrincham - Piccadilly";

    private LocalDate when;
    private String url;

    @BeforeAll
    static void beforeAnyTestsRun() {
        createFactory(LatLong.Invalid);
    }

    @SuppressWarnings("unused")
    private static Stream<ProvidesDriver> getProvider() {
        return getProviderCommon();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        url = appExtenstion.getUrl()+"/app/index.html";
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

    @Test
    void shouldHaveAPIAvailable() throws MalformedURLException {
        final URL apiUrl = new URL(appExtenstion.getUrl()+"/api/datainfo");

        // helps diagnosis of when the other tests are failing
        try {
            final URLConnection connection = apiUrl.openConnection();
            connection.setConnectTimeout(20*1000); // ms
            connection.connect();
            String contentType = connection.getContentType();
            assertEquals("application/json", contentType, "Wrong content type for " + apiUrl);
        } catch (IOException e) {
            fail("did not connect to " + apiUrl + " Exception " +e);
        }
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldShowInitialCookieConsentAndThenDismiss(ProvidesDriver providesDriver) throws IOException {
        providesDriver.init();
        providesDriver.clearCookies();

        AppPage appPage = providesDriver.getAppPage();
        appPage.load(url);

        assertTrue(appPage.waitForCookieAgreementVisible());
        appPage.agreeToCookies();
        assertTrue(appPage.waitForCookieAgreementInvisible());
        assertTrue(appPage.waitForReady());
        assertTrue(appPage.waitForLocationSelectionsAvailable());
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    @SmokeTest
    void shouldHaveInitialValuesAndSetInputsSetCorrectly(ProvidesDriver providesDriver) throws IOException {
        AppPage appPage = prepare(providesDriver, url);

        validateCurrentTimeIsSelected(appPage);

        assertEquals(TestEnv.LocalNow().toLocalDate(), appPage.getDate());

        appPage.waitForLocationSelectionsAvailable();

        List<String> allFromStops = appPage.getAllStopsFromStops();
        assertFalse(allFromStops.isEmpty(), "no from stops");
        checkSorted(allFromStops);

        List<String> allToStops = appPage.getAllStopsToStops();
        assertFalse(allToStops.isEmpty(), "no to stops");
        checkSorted(allToStops);

        desiredJourney(appPage, altrincham, bury, when, TramTime.of(10,15), false);
        assertJourney(appPage, altrincham, bury, "10:15", when, false);
        desiredJourney(appPage, altrincham, bury, when.plusMonths(1), TramTime.of(3,15), false);
        assertJourney(appPage, altrincham, bury, "03:15", when.plusMonths(1), false);

        appPage.selectNow();
        validateCurrentTimeIsSelected(appPage);

        appPage.selectToday();
        assertEquals(TestEnv.LocalNow().toLocalDate(), appPage.getDate());
    }

    private void checkSorted(List<String> allStops) {
        List<String> sortedAllStops = new LinkedList<>(allStops);
        sortedAllStops.sort(Comparator.comparing(String::toLowerCase));
        for (int i = 0; i < allStops.size(); i++) {
            assertEquals(sortedAllStops.get(i), allStops.get(i), "sorted?");
        }
    }

    private void validateCurrentTimeIsSelected(AppPage appPage) {
        LocalTime timeOnPage = timeSelectedOnPage(appPage);
        LocalTime now = TestEnv.LocalNow().toLocalTime();
        int diff = Math.abs(now.toSecondOfDay() - timeOnPage.toSecondOfDay());
        // allow for page render and webdriver overheads
        assertTrue(diff <= 110, String.format("now:%s timeOnPage: %s diff: %s", now, timeOnPage, diff));
    }

    private LocalTime timeSelectedOnPage(AppPage appPage) {
        String timeOnPageRaw = appPage.getTime();
        return LocalTime.parse(timeOnPageRaw);
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldTravelAltyToBuryAndSetRecents(ProvidesDriver providesDriver) throws IOException {
        FetchAllStationsFromAPI fetchAllStationsFromAPI = new FetchAllStationsFromAPI(appExtenstion);

        List<LocationRefDTO> allStations = fetchAllStationsFromAPI.getStations();
        assertFalse(allStations.isEmpty());
        final int allStationsSize = allStations.size();

        AppPage appPage = prepare(providesDriver, url);

        desiredJourney(appPage, Altrincham, Deansgate, when, TramTime.of(10,15), false);
        appPage.planAJourney();

        assertTrue(appPage.resultsClickable(), "results clickable");
        assertTrue(appPage.searchEnabled());

        // change start, so previous start is now in recents
        appPage.setStart(ExchangeSquare);
        // change dest, so previous dest is now in recents
        appPage.setDest(TramStations.PiccadillyGardens); // so 'to' is available in the recents list

        // check 'from' recents are set
        List<String> fromRecent = appPage.getRecentFromStops();
        assertThat(fromRecent, hasItems(Altrincham.getName(), Deansgate.getName()));

        List<String> remainingFromStops = appPage.getAllStopsFromStops();
        assertThat(remainingFromStops, not(contains(fromRecent)));

        // still displaying all stations
        assertEquals(allStationsSize -1,
                remainingFromStops.size()+fromRecent.size()); // less one as 'to' stop is excluded

        // check 'to' recents are set
        List<String> toRecent = appPage.getRecentToStops();
        assertThat(toRecent, hasItems(Altrincham.getName(), Deansgate.getName()));
        List<String> remainingToStops = appPage.getAllStopsToStops();
        assertThat(remainingToStops, not(contains(toRecent)));
        assertEquals(allStationsSize -1,
                remainingToStops.size()+toRecent.size()); // less one as 'from' stop is excluded

        assertJourney(appPage, ExchangeSquare, PiccadillyGardens, "10:15", when, false);
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldCheckAltrinchamToDeansgate(ProvidesDriver providesDriver) throws IOException {
        AppPage appPage = prepare(providesDriver, url);
        TramTime queryTime = TramTime.of(10,0);
        desiredJourney(appPage, altrincham, deansgate, when, queryTime, false);
        appPage.planAJourney();

        assertTrue(appPage.resultsClickable(), "results clickable");

        List<TestResultSummaryRow> results = appPage.getResults();
        // TODO Lockdown 3->2
        assertTrue(results.size()>=2, "at least 2 results");

        TramTime previous = queryTime;
        for (TestResultSummaryRow result : results) {
            TramTime currentArrivalTime = result.getArriveTime();
            assertTrue(currentArrivalTime.isAfter(previous) || currentArrivalTime.equals(previous),
                    "arrival time order for " + result + " previous: " + previous);
            assertTrue(currentArrivalTime.isValid());
            assertTrue(result.getDepartTime().isValid());
            assertTrue(currentArrivalTime.isAfter(result.getDepartTime()), "arrived before depart");
            assertEquals("Direct", result.getChanges());
            previous = currentArrivalTime;
        }

        // select first journey
        TestResultSummaryRow firstResult = results.get(0);
        firstResult.moveTo(providesDriver);
        appPage.waitForClickable(firstResult.getElement());
        firstResult.click(providesDriver);

        List<Stage> stages = firstResult.getStages();
        assertEquals(1, stages.size());
        Stage stage = stages.get(0);

        Set<KnownTramRoute> expectedRoutes = EnumSet.of(BuryManchesterAltrincham, EtihadPiccadillyAltrincham);
        Set<String> headSigns = new HashSet<>(Arrays.asList(Bury.getName(), Piccadilly.getName(), "Bury via Market Street & Victoria"));
        Set<TramTime> departTimes = Collections.singleton(firstResult.getDepartTime());
        validateAStage(stage, departTimes, "Board Tram", Altrincham.getName(),
                Collections.singletonList(1),
                expectedRoutes, headSigns, 9);
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldCheckLateNightJourney(ProvidesDriver providesDriver) throws IOException {
        AppPage appPage = prepare(providesDriver, url);
        TramTime queryTime = TramTime.of(23,42);
        desiredJourney(appPage, TraffordCentre, ImperialWarMuseum, when, queryTime, false);
        appPage.planAJourney();

        assertTrue(appPage.resultsClickable(), "results displayed");

        List<TestResultSummaryRow> results = appPage.getResults();

        for (TestResultSummaryRow result : results) {
            TramTime arrivalTime = result.getArriveTime();
            assertTrue(arrivalTime.isValid(), "Invalid arrival time " + arrivalTime + " from " + result);
            assertTrue(arrivalTime.isAfter(TramTime.of(0,0)));
            assertTrue(arrivalTime.isBefore(TramTime.nextDay(1,0)));
        }
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldHideStationInToListWhenSelectedInFromList(ProvidesDriver providesDriver) throws IOException {
        AppPage appPage = prepare(providesDriver, url);
        desiredJourney(appPage, altrincham, bury, when, TramTime.of(10,15), false);

        appPage.waitForStops(AppPage.FROM_STOP);
        List<String> destStops = appPage.getToStops();
        assertFalse(destStops.contains(Altrincham.getName()), "should not contain alty");
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldShowNoRoutesMessage(ProvidesDriver providesDriver) throws IOException {
        AppPage appPage = prepare(providesDriver, url);
        desiredJourney(appPage, altrincham, bury, when, TramTime.of(3,15), false);
        appPage.planAJourney();

        assertTrue(appPage.noResults());
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldUpdateWhenNewJourneyIsEntered(ProvidesDriver providesDriver) throws IOException {
        TramTime tenFifteen = TramTime.of(10,15);
        TramTime eightFifteen = TramTime.of(9,15);

        AppPage appPage = prepare(providesDriver, url);
        desiredJourney(appPage, altrincham, deansgate, when, tenFifteen, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable(), "results clickable");
        assertTrue(appPage.searchEnabled());

        List<TestResultSummaryRow> results = appPage.getResults();
        final TramTime departTime = results.get(0).getDepartTime();
        assertTrue(departTime.isValid());
        assertTrue(departTime.isAfter(tenFifteen), "depart not after " + tenFifteen);

        desiredJourney(appPage, altrincham, deansgate, when, eightFifteen, false);
        appPage.planAJourney();
        // need way to delay response for this test to be useful
        //assertFalse(appPage.searchEnabled());
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<TestResultSummaryRow> updatedResults = appPage.getResults();
        final TramTime updatedDepartTime = updatedResults.get(0).getDepartTime();
        assertTrue(updatedDepartTime.isValid());
        assertTrue(updatedDepartTime.isBefore(tenFifteen));
        assertTrue(updatedDepartTime.isAfter(eightFifteen));
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldUpdateWhenEarlierClicked(ProvidesDriver providesDriver) throws IOException {
        TramTime tenFifteen = TramTime.of(10,15);

        AppPage appPage = prepare(providesDriver, url);
        desiredJourney(appPage, altrincham, deansgate, when, tenFifteen, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable(), "results clickable");
        assertTrue(appPage.searchEnabled());

        List<TestResultSummaryRow> results = appPage.getResults();
        TramTime firstDepartureTime = results.get(0).getDepartTime();
        assertTrue(firstDepartureTime.isValid());
        assertTrue(firstDepartureTime.isAfter(tenFifteen));

        appPage.earlier();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<TestResultSummaryRow> updatedResults = appPage.getResults();
        TramTime updatedDepartTime = updatedResults.get(0).getDepartTime();
        assertTrue(updatedDepartTime.isValid());
        assertTrue(updatedDepartTime.isBefore(firstDepartureTime), "should be before current first departure time");
        Duration difference = TramTime.difference(firstDepartureTime, updatedDepartTime);
        assertTrue(Durations.lessThan(difference, Duration.ofMinutes(60)), // difference.compareTo(Duration.ofMinutes(60)) < 0,
                "Too much gap between " + firstDepartureTime + " and update: " + updatedDepartTime);
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldUpdateWhenLaterClicked(ProvidesDriver providesDriver) throws IOException {
        TramTime tenFifteen = TramTime.of(10,15);

        AppPage appPage = prepare(providesDriver, url);
        desiredJourney(appPage, altrincham, deansgate, when, tenFifteen, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable(), "results clickable");
        assertTrue(appPage.searchEnabled());

        List<TestResultSummaryRow> results = appPage.getResults();
        final TramTime departTime = results.get(0).getDepartTime();
        assertTrue(departTime.isValid());
        assertTrue(departTime.isAfter(tenFifteen));
        TramTime lastDepartureTime = results.get(results.size() - 1).getDepartTime();
        assertTrue(lastDepartureTime.isValid());

        appPage.later();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<TestResultSummaryRow> updatedResults = appPage.getResults();
        TramTime updatedDepartTime = updatedResults.get(0).getDepartTime();
        assertTrue(updatedDepartTime.isValid());
        assertTrue(updatedDepartTime.isAfter(lastDepartureTime), "should be after current departure time");
        Duration difference = TramTime.difference(lastDepartureTime, updatedDepartTime);
        assertTrue(difference.compareTo(Duration.ofMinutes(60)) < 0,
                "Too much gap between " + lastDepartureTime + " and update: " + updatedDepartTime);
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldHaveMultistageJourney(ProvidesDriver providesDriver) throws IOException {
        AppPage appPage = prepare(providesDriver, url);
        TramTime planTime = TramTime.of(10,0);
        desiredJourney(appPage, altrincham, TramStations.ManAirport, when, planTime, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());

        List<TestResultSummaryRow> results = appPage.getResults();
        // TODO pre-lockdown timetable was 3
        assertTrue(results.size()>=2, "at least 2 journeys, was "+results.size());
        TramTime previousArrivalTime = planTime; // sorted by arrival time, so we may seen
        for (TestResultSummaryRow result : results) {
            final TramTime arriveTime = result.getArriveTime();
            final TramTime departTime = result.getDepartTime();

            assertTrue(arriveTime.isValid());
            assertTrue(departTime.isValid());

            assertTrue(arriveTime.isAfter(departTime));
            assertTrue(arriveTime.isAfter(previousArrivalTime) || arriveTime.equals(previousArrivalTime));
            assertEquals(result.getChanges(), TraffordBar.getName());
            previousArrivalTime = arriveTime;
        }

        // need to sort, otherwise unpredictable
        List<TestResultSummaryRow> sorted = results.stream().sorted(Comparator.comparing(TestResultSummaryRow::getDepartTime)).toList();

        // select first journey
        TestResultSummaryRow firstResult = sorted.get(0);
        firstResult.moveTo(providesDriver);
        appPage.waitForClickable(firstResult.getElement());
        firstResult.click(providesDriver);

        List<Stage> stages = firstResult.getStages();
        assertEquals(2, stages.size());

        Stage firstStage = stages.get(0);
        Stage secondStage = stages.get(1);

        Set<KnownTramRoute> routeNames = new HashSet<>(Arrays.asList(EtihadPiccadillyAltrincham, BuryManchesterAltrincham));
        Set<String> firstStageHeadsigns = new HashSet<>(Arrays.asList(Piccadilly.getName(), Bury.getName(), "Bury via Market Street & Victoria"));

        TramTime firstDepartTime = firstResult.getDepartTime();
        validateAStage(firstStage, Collections.singleton(firstDepartTime), "Board Tram", Altrincham.getName(),
                Collections.singletonList(1),
                routeNames, firstStageHeadsigns, 7);

        // Too timetable dependent?
        Set<String> secondStageHeadsigns = new HashSet<>(Arrays.asList(ManAirport.getName(), "Manchester Airport via Market Street & Wythenshawe"));

        Set<TramTime> validTimes = new HashSet<>(Arrays.asList(TramTime.of(10,37),
                TramTime.of(10,29),
                TramTime.of(10,25)));
        validateAStage(secondStage, validTimes, "Change Tram", TraffordBar.getName(),
                Arrays.asList(1,2),
                Collections.singleton(DeansgateCastlefieldManchesterAirport),
                secondStageHeadsigns, 17);

        assertEquals(TraffordBar.getName(), secondStage.getActionStation());
        assertEquals("Change Tram", secondStage.getAction());

    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldDisplayWeekendWorkNoteOnlyOnWeekends(ProvidesDriver providesDriver) throws IOException {
        TramTime time = TramTime.of(10,15);

        AppPage appPage = prepare(providesDriver, url);
        LocalDate aSaturday = TestEnv.nextSaturday().toLocalDate();

        desiredJourney(appPage, altrincham, deansgate, aSaturday, time, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable(), "results clickable");
        assertTrue(appPage.notesPresent());
        assertTrue(appPage.hasWeekendMessage());

        desiredJourney(appPage, altrincham, deansgate, when, time, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.noWeekendMessage());

        desiredJourney(appPage, altrincham, deansgate, aSaturday.plusDays(1), time, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.notesPresent());
        assertTrue(appPage.hasWeekendMessage());
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldHaveBuildAndVersionNumberInFooter(ProvidesDriver providesDriver) throws IOException {
        AppPage appPage = prepare(providesDriver, url);

        String build = appPage.getExpectedBuildNumberFromEnv();

        String result = appPage.getBuild();
        assertEquals("2."+build, result);

        String version = appPage.getVersion();
        assertEquals(DataVersionResourceTest.version, version);


    }

    @Disabled("unreliable, cooke is stored in browser but selenium just ignores it sometimes")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldDisplayCookieAgreementIfNotVisited(ProvidesDriver providesDriver) throws InterruptedException, IOException {
        providesDriver.init();
        providesDriver.clearCookies();

        AppPage appPage = providesDriver.getAppPage();
        appPage.load(url);

        assertFalse(appPage.hasCookieNamed("tramchesterVisited"));

        assertTrue(appPage.waitForCookieAgreementVisible());

        appPage.agreeToCookies();
        assertTrue(appPage.waitForCookieAgreementInvisible(), "wait for cookie agreement to close");

        // really ought not to need this, but seems webdriver does not refresh cookies after it's first attempt
        // so just keeps failing.....sigh
        Thread.sleep(1000);
        // cookie should now be set
        Cookie cookie = appPage.waitForCookie("tramchesterVisited");

        assertNotNull(cookie, "cookie null");
        assertNotNull(cookie.getValue(), "cookie null");

        String cookieContents = URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
        assertEquals("{\"visited\":true}", cookieContents);
        assertTrue(appPage.waitForCookieAgreementInvisible());

        AppPage afterReload = providesDriver.getAppPage();
        assertTrue(afterReload.waitForCookieAgreementInvisible());
        afterReload.waitForLocationSelectionsAvailable();
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldDisplayDisclaimer(ProvidesDriver providesDriver) throws IOException {
        AppPage appPage = prepare(providesDriver, url);

        appPage.displayDisclaimer();
        assertTrue(appPage.waitForDisclaimerVisible());

        appPage.dismissDisclaimer();
        // chrome takes a while to close it, so wait for it to go
        assertTrue(appPage.waitForDisclaimerInvisible());
    }

    public static void desiredJourney(AppPage appPage, TramStations start, TramStations dest, LocalDate date, TramTime time, boolean arriveBy) {
        appPage.setStart(start);
        appPage.setDest(dest);
        appPage.setSpecificDate(date);
        appPage.setTime(time);
        appPage.setArriveBy(arriveBy);
    }

    public static void desiredJourneyFromMyLocation(AppPage appPage, TramStations dest, LocalDate date, TramTime time, boolean arriveBy) {
        appPage.setStartToMyLocation();
        appPage.setDest(dest);
        appPage.setSpecificDate(date);
        appPage.setTime(time);
        appPage.setArriveBy(arriveBy);
    }

    private static void assertJourney(AppPage appPage, TramStations start, TramStations dest, String time, LocalDate date,
                                      boolean arriveBy) {
        assertEquals(start.getName(), appPage.getFromStop());
        assertEquals(dest.getName(), appPage.getToStop());
        assertEquals(time, appPage.getTime());
        assertEquals(date, appPage.getDate());
        assertEquals(arriveBy, appPage.getArriveBy());
    }

    public static void validateAStage(Stage stage, Set<TramTime> departTimes, String action, String actionStation, List<Integer> platforms,
                                      Set<KnownTramRoute> expectedRoutes, Set<String> headsigns, int passedStops) {

        Set<String> expectedRoutesNames = expectedRoutes.stream().map(KnownTramRoute::longName).collect(Collectors.toSet());

        assertTrue(departTimes.stream().allMatch(TramTime::isValid),"departTime not valid");

        TramTime stageDepartTime = stage.getDepartTime();
        assertTrue(departTimes.contains(stageDepartTime), "Wrong departTime got '" + stageDepartTime + "' but needed " + departTimes);

        assertEquals(action, stage.getAction(), "action");
        assertEquals(actionStation, stage.getActionStation(), "actionStation");
        assertTrue(platforms.contains(stage.getPlatform()), "platform");

        String stageRoute = stage.getRouteName();
        assertTrue(expectedRoutesNames.contains(stageRoute), "Wrong linename, got '"+ stageRoute +"' but needed one of " + expectedRoutesNames);

        String stageHeadsign = stage.getHeadsign();
        assertTrue(headsigns.contains(stageHeadsign), "Wrong headsign, got '"+ stageHeadsign +"' but needed one of" + headsigns);

        assertEquals(passedStops, stage.getPassedStops(), "passedStops");
    }

}

