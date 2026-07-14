package com.tramchester.integration.resources.journeyPlanning;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.DTO.query.JourneyQueryDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.JourneyPlannerResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramAppTestExtension;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.Summer2026Closures;
import com.tramchester.testSupport.testTags.TramApp;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(TramAppTestExtension.class)
public class JourneyPlannerResourceTest {

    @TramApp
    private static IntegrationAppExtension appExtension =
            new IntegrationAppExtension(new ResourceTramTestConfig<>(JourneyPlannerResource.class));

    private static GuiceContainerDependencies dependencies;

    private TramDate when;
    private JourneyResourceTestFacade journeyPlanner;
    private StationRepository stationRepository;

    private int maxChanges;
    private Station altrincham;

    @BeforeAll
    static void onceBeforeAnyTest() {
        App app = appExtension.getTestSupport().getApplication();
        dependencies = app.getDependencies();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        journeyPlanner = new JourneyResourceTestFacade(appExtension);

        TramchesterConfig config = dependencies.get(TramchesterConfig.class);
        maxChanges = config.getMaxNumberChanges();

        stationRepository = dependencies.get(StationRepository.class);
        altrincham = stationRepository.getStationById(Altrincham.getId());
    }

    @AfterAll
    public static void onceAfterAllTestsRun() {
        appExtension.after();
        appExtension = null;
    }

    @Test
    void shouldPlanSimpleJourneyFromAltyToCornbrook() {
        checkAltyToCornbrook(TramTime.of(8, 15), false);
    }

    @Test
    void shouldPlanSimpleJourneyFromAltyToCornbrookArriveBy() {
        TramTime arriveByTime = TramTime.of(8, 15);
        checkAltyToCornbrook(arriveByTime, true);
    }

    private void checkAltyToCornbrook(TramTime queryTime, boolean arriveBy) {
        final List<String> possibleHeadsigns = Arrays.asList( Bury.getName(), Piccadilly.getName(),
                "Bury via Market Street & Victoria", Etihad.getName(),
                // summer 2025 closures
                Crumpsal.getName());

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, queryTime, Altrincham, Cornbrook, arriveBy, 0);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertFalse(journeys.isEmpty());

        LocalDateTime queryLocalDateTime = queryTime.toDate(when);

        final Set<JourneyDTO> toCheck;
        if (arriveBy) {
            toCheck = journeys.stream().
                    filter(journeyDTO -> journeyDTO.getExpectedArrivalTime().isBefore(queryLocalDateTime)).
                    collect(Collectors.toSet());

            assertFalse(toCheck.isEmpty(), "none arrive in time " + journeys);
        } else {
            toCheck = journeys;
        }

        toCheck.forEach(journey -> {
            VehicleStageDTO firstStage = (VehicleStageDTO) journey.getStages().getFirst();

            String headSign = firstStage.getHeadSign();
            assertTrue(possibleHeadsigns.contains(headSign), "unexpected headsign " + headSign);

            PlatformDTO platform = firstStage.getPlatform();
            LocalDateTime firstDepartureTime = journey.getFirstDepartureTime();
            if (arriveBy) {
                assertTrue(firstDepartureTime.isBefore(queryLocalDateTime), firstDepartureTime + " not before " + queryLocalDateTime);
            } else {
                assertTrue(firstDepartureTime.isAfter(queryLocalDateTime), firstDepartureTime + " not after " + queryLocalDateTime);
            }
            assertEquals(when.toLocalDate(), journey.getQueryDate());

            List<String> expectedNumbers = Arrays.asList("1", "2");
            assertTrue(expectedNumbers.contains(platform.getPlatformNumber()));
            assertTrue(platform.getName().startsWith("Altrincham platform 1"));
            Set<IdForDTO> ids = altrincham.getPlatforms().stream().map(IdForDTO::createFor).collect(Collectors.toSet());
            assertTrue(ids.contains(platform.getId()));

            journey.getStages().forEach(stage -> assertEquals(when.toLocalDate(), stage.getQueryDate()));
        });

        Set<Integer> indexs = journeys.stream().map(JourneyDTO::getIndex).collect(Collectors.toSet());
        assertEquals(journeys.size(), indexs.size(), "mismatch on indexes " + indexs);

    }

    @Test
    void shouldNotFindAnyResultsIfNoneTramModeIsRequested() {

        TramTime queryTime = TramTime.of(8,15);

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, queryTime, TramStations.Altrincham,
                TramStations.Cornbrook, false, 0);
        query.setModes(Collections.singleton(TransportMode.Train));

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        assertTrue(plan.getJourneys().isEmpty());

    }

    @Test
    void shouldNotFindAnyResultsIfNoneTramModeIsRequestedArriveBy() {

        TramTime queryTime = TramTime.of(8,15);

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, queryTime, TramStations.Altrincham,
                TramStations.Cornbrook, true, 0);
        query.setModes(Collections.singleton(TransportMode.Train));

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        assertTrue(plan.getJourneys().isEmpty());

    }

    @Test
    void shouldPlanSimpleJourneyArriveByHasAtLeastOneDepartByRequiredTime() {
        TramTime queryTime = TramTime.of(11,45);

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, queryTime, TramStations.Altrincham,
                TramStations.Cornbrook, true, 0);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> arriveBefore = plan.getJourneys().stream().
                filter(journeyDTO -> journeyDTO.getExpectedArrivalTime().isBefore(queryTime.toDate(when))).
                collect(Collectors.toSet());

        assertFalse(arriveBefore.isEmpty(), "no joruneys arrives in time " + plan.getJourneys());

        List<JourneyDTO> found = new ArrayList<>();
        arriveBefore.forEach(journeyDTO -> {
            LocalDateTime firstDepartureTime = journeyDTO.getFirstDepartureTime();
            LocalDateTime queryLocalDateTime = queryTime.toDate(when);
            assertTrue(firstDepartureTime.isBefore(queryLocalDateTime), firstDepartureTime + " not before " + queryLocalDateTime);
            // TODO lockdown less frequent services during lockdown mean threshhold here increased to 12
            Duration duration = Duration.between(journeyDTO.getExpectedArrivalTime(), queryLocalDateTime);
            if (duration.getSeconds()<=(12*60)) {
                found.add(journeyDTO);
            }
            assertEquals(when.toLocalDate(), journeyDTO.getQueryDate());
        });
        Assertions.assertFalse(found.isEmpty(), "no journeys found");
    }

    @Test
    void shouldGetNoResultsToAirportWhenLimitOnChanges() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, TramTime.of(11,45), Altrincham, ManAirport,
                true, 0);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);
        assertTrue(plan.getJourneys().isEmpty());
    }

    @Test
    void shouldReproLateNightIssueShudehillToAltrincham() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, TramTime.of(23,11), Shudehill, Altrincham, false, 1);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertFalse(journeys.isEmpty(), "no journeys");
        journeys.forEach(journeyDTO ->
                assertTrue(journeyDTO.getExpectedArrivalTime().isAfter(journeyDTO.getFirstDepartureTime())));
    }

    @Test
    void shouldPlanSimpleJourneyFromAltyToAshton() {

        // note: Cornbrook, StPetersSquare, Deansgate all valid but have same cost

        Station deansgate = Deansgate.from(stationRepository);
        Station cornbrook = Cornbrook.from(stationRepository);
        Station piccadily = Piccadilly.from(stationRepository);
        Station stPetersSquare = StPetersSquare.from(stationRepository);
        // summer 2026
        Station piccGardens = PiccadillyGardens.from(stationRepository);

        Set<Platform> platforms = new HashSet<>();
        platforms.addAll(deansgate.getPlatforms());
        platforms.addAll(cornbrook.getPlatforms());
        platforms.addAll(piccadily.getPlatforms());
        platforms.addAll(stPetersSquare.getPlatforms());
        platforms.addAll(piccGardens.getPlatforms());

        Set<IdForDTO> platformIds = platforms.stream().map(IdForDTO::createFor).collect(Collectors.toSet());

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, TramTime.of(17, 45), Altrincham, Ashton, false, 1);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertFalse(journeys.isEmpty(), "no journeys");

        journeys.forEach(journey -> {
            VehicleStageDTO firstStage = (VehicleStageDTO) journey.getStages().getFirst();
            PlatformDTO stategOnePlatform = firstStage.getPlatform();

            List<String> expected = Arrays.asList("1", "2");
            assertTrue(expected.contains(stategOnePlatform.getPlatformNumber()));
            assertTrue( stategOnePlatform.getName().startsWith("Altrincham platform"));
            Set<IdForDTO> ids = altrincham.getPlatforms().stream().map(IdForDTO::createFor).collect(Collectors.toSet());
            //assertEquals( IdForDTO.createFor(firstPlatformAtAlty), stategOnePlatform.getId());
            assertTrue(ids.contains(stategOnePlatform.getId()));

            SimpleStageDTO secondStageRaw = journey.getStages().get(1);
            assertInstanceOf(VehicleStageDTO.class, secondStageRaw, "Expected vehicle stage but got " + secondStageRaw);
            VehicleStageDTO secondStage = (VehicleStageDTO) secondStageRaw;
            assertNotNull(secondStage);

            PlatformDTO secondStagePlatform = secondStage.getPlatform();

            // seems can be 1 through 4
            String platformNumber = secondStagePlatform.getPlatformNumber();
            assertTrue("1234".contains(platformNumber), "unexpected platform number, got " + platformNumber);

            List<String> expectedSecondStationNames = Arrays.asList(
                    Cornbrook.getName(),
                    Deansgate.getName(),
                    Piccadilly.getName(),
                    PiccadillyGardens.getName(),
                    StPetersSquare.getName());

            // multiple possible places to change depending on timetable etc
            LocationRefDTO secondStagePlatformStation = secondStagePlatform.getStation();
            assertTrue(expectedSecondStationNames.contains(secondStagePlatformStation.getName()),
                    "did not expect " + secondStagePlatformStation.getName());

            assertTrue(platformIds.contains(secondStagePlatform.getId()),
                    secondStagePlatform.getId() +"("+secondStagePlatformStation+") not in "
                            + platformIds + "\n for journey " + journey);

            List<ChangeStationRefWithPosition> changeStations = journey.getChangeStations();

            assertEquals(1, changeStations.size());
            ChangeStationRefWithPosition changeStation = changeStations.getFirst();
            assertTrue(expectedSecondStationNames.contains(changeStation.getName()), "did not expect " + changeStation.getName());
            assertEquals(TransportMode.Tram, changeStation.getFromMode());

        });

    }

    @Test
    void testAltyToManAirportHasRealisticTranferAtCornbrook() {
        // Clousures on alty line during July on Sunday
        TramDate date = TestEnv.testDay(); // TestEnv.nextSunday();

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(date, TramTime.of(11, 0),
                Altrincham, ManAirport, false, maxChanges);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();

        assertFalse(journeys.isEmpty(), "no journeys for " + query);
        checkDepartsAfterPreviousArrival("Altrincham to airport at 11:43 sunday", journeys);
    }

    @Test
    void shouldFindRouteVicToShawAndCrompton() {
        validateAtLeastOneJourney(Victoria, ShawAndCrompton, when, TramTime.of(23,15));
    }

    @Test
    void shouldFindRouteDeansgateToVictoriaMidDay() {
        validateAtLeastOneJourney(Deansgate, Victoria, when, TramTime.of(12,41));
    }

    @Test
    void shouldFindRouteDeansgateToVictoriaEndDay() {
        validateAtLeastOneJourney(Deansgate, Victoria, when, TramTime.of(23,41));
    }

    @Summer2026Closures
    @Test
    void shouldFindEndOfDayTwoStageJourney() {
        validateAtLeastOneJourney(TraffordCentre, TraffordBar, when, TramTime.of(23,30));
    }

    @Summer2026Closures
    @Test
    void shouldFindEndOfDayThreeStageJourney() {
        validateAtLeastOneJourney(Altrincham, ShawAndCrompton, when, TramTime.of(22,45));
    }

    @Summer2026Closures
    @Test
    void shouldOnlyReturnFullJourneysForEndOfDaysJourney() {
        JourneyPlanRepresentation results = validateAtLeastOneJourney(Deansgate,
                ManAirport, when, TramTime.of(23,5));

        assertFalse(results.getJourneys().isEmpty() );
    }

    @Test
    void shouldCorrectlyQueryAfterMidnight() {
        JourneyPlanRepresentation results = validateAtLeastOneJourney(Altrincham,
                OldTrafford, when, TramTime.of(0,15));

        assertFalse(results.getJourneys().isEmpty() );
    }

    @Test
    void shouldFilterOutJourneysWithSameDepartArrivePathButDiffChanges() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(TestEnv.testDay(), TramTime.of(10, 43), Altrincham, Ashton, false, 1);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = plan.getJourneys();
        Set<Triple<LocalDateTime, LocalDateTime, List<LocationRefWithPosition>>> filtered = journeys.stream().
                map(dto -> Triple.of(dto.getFirstDepartureTime(), dto.getExpectedArrivalTime(), dto.getPath())).
                collect(Collectors.toSet());

        assertEquals(filtered.size(), journeys.size(), "Not same " + journeys + " filtered " + filtered);
    }

    @Test
    void shouldHaveFirstResultWithinReasonableTimeOfQuery() {

        TramTime queryTime = TramTime.of(17,45);

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(TestEnv.testDay(), TramTime.ofHourMins(queryTime.asLocalTime()),
                Altrincham, Ashton, false, maxChanges);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Optional<JourneyDTO> findEarliest = results.getJourneys().stream().min(Comparator.comparing(JourneyDTO::getFirstDepartureTime));
        assertTrue(findEarliest.isPresent());

        JourneyDTO earliestJourney = findEarliest.get();

        TramTime firstDepart = TramTime.ofHourMins(earliestJourney.getFirstDepartureTime().toLocalTime());

        TramDuration elapsed = TramTime.difference(queryTime, firstDepart);
        assertTrue(Durations.lessThan(elapsed,TramDuration.ofMinutes(15)), "first result too far in future " + firstDepart + " for " + results.getJourneys());
    }

    private void checkDepartsAfterPreviousArrival(String message, Set<JourneyDTO> journeys) {
        for(JourneyDTO journey: journeys) {
            LocalDateTime previousArrive = null;
            for(SimpleStageDTO stage : journey.getStages()) {
                if (previousArrive!=null) {
                    LocalDateTime firstDepartureTime = stage.getFirstDepartureTime();
                    String prefix  = String.format("Check first departure time %s is after arrival time %s for stage %s and journey %s " ,
                            firstDepartureTime, previousArrive, stage, journey);
                    if (stage.getMode()!= TransportMode.Walk) {
                        assertFalse(firstDepartureTime.isBefore(previousArrive), prefix + message);
                    }
                }
                previousArrive = stage.getExpectedArrivalTime();
            }
        }
    }

    private JourneyPlanRepresentation validateAtLeastOneJourney(TramStations start, TramStations end,
                                                                TramDate date, TramTime queryTime)  {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(date, queryTime, start, end, false, maxChanges);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();

        String message = String.format(" from %s to %s at %s on %s ", start, end, queryTime, date);
        assertFalse(journeys.isEmpty(), "Unable to find journey " + message);
        checkDepartsAfterPreviousArrival(message, journeys);
        journeys.forEach(journey -> assertFalse(journey.getStages().isEmpty(), "Missing stages for journey "+journey));
        return results;
    }

}
