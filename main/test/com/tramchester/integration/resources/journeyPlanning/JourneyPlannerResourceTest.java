package com.tramchester.integration.resources.journeyPlanning;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.Platform;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.DTO.query.JourneyQueryDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.JourneyPlannerResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
public class JourneyPlannerResourceTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceTramTestConfig<>(JourneyPlannerResource.class));

    private static GuiceContainerDependencies dependencies;

    private TramDate when;
    private JourneyResourceTestFacade journeyPlanner;
    private Platform firstPlatformAtAlty;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTesst() {
        App app = appExtension.getTestSupport().getApplication();
        dependencies = app.getDependencies();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        journeyPlanner = new JourneyResourceTestFacade(appExtension);

        stationRepository = dependencies.get(StationRepository.class);
        Station altrincham = stationRepository.getStationById(Altrincham.getId());
        List<Platform> platforms = new ArrayList<>(altrincham.getPlatforms());
        firstPlatformAtAlty = platforms.get(0);
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
        final List<String> possibleHeadsigns = Arrays.asList( Bury.getName(), Piccadilly.getName(), "Bury via Market Street & Victoria");

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, queryTime, Altrincham, Cornbrook, arriveBy, 0);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            VehicleStageDTO firstStage = (VehicleStageDTO) journey.getStages().get(0);

            String headSign = firstStage.getHeadSign();
            assertTrue(possibleHeadsigns.contains(headSign), "unexpected headsign " + headSign);

            PlatformDTO platform = firstStage.getPlatform();
            if (arriveBy) {
                assertTrue(journey.getFirstDepartureTime().isBefore(queryTime.toDate(when)));
            } else {
                assertTrue(journey.getFirstDepartureTime().isAfter(queryTime.toDate(when)));
            }
            assertEquals(when.toLocalDate(), journey.getQueryDate());

            assertEquals("1", platform.getPlatformNumber());
            assertEquals("Altrincham platform 1", platform.getName());
            assertEquals(IdForDTO.createFor(firstPlatformAtAlty), platform.getId());

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

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, queryTime, TramStations.Altrincham, TramStations.Cornbrook, true, 0);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime.toDate(when)));
            // TODO lockdown less frequent services during lockdown mean threshhold here increased to 12
            Duration duration = Duration.between(journeyDTO.getExpectedArrivalTime(), queryTime.toDate(when));
            if (duration.getSeconds()<=(12*60)) {
                found.add(journeyDTO);
            }
            assertEquals(when.toLocalDate(), journeyDTO.getQueryDate());
        });
        Assertions.assertFalse(found.isEmpty(), "no journeys found");
    }

    @Test
    void shouldGetNoResultsToAirportWhenLimitOnChanges() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, TramTime.of(11,45), Altrincham, ManAirport, true, 0);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);
        assertTrue(plan.getJourneys().isEmpty());
    }

    @Test
    void shouldReproLateNightIssueShudehillToAltrincham() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, TramTime.of(23,11), Shudehill, Altrincham, false, 3);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertFalse(journeys.isEmpty(), "no journeys");
        journeys.forEach(journeyDTO ->
                assertTrue(journeyDTO.getExpectedArrivalTime().isAfter(journeyDTO.getFirstDepartureTime())));
    }

    @Test
    void shouldPlanSimpleJourneyFromAltyToAshton() {

        // note: Cornbrook, StPetersSquare, Deansgate all valid but have same cost

        Station deansgate = stationRepository.getStationById(Deansgate.getId());
        Station cornbrook = stationRepository.getStationById(Cornbrook.getId());
        Station piccadily = stationRepository.getStationById(Piccadilly.getId());
        Station stPetersSquare = stationRepository.getStationById(StPetersSquare.getId());

        Set<Platform> platforms = new HashSet<>();
        platforms.addAll(deansgate.getPlatforms());
        platforms.addAll(cornbrook.getPlatforms());
        platforms.addAll(piccadily.getPlatforms());
        platforms.addAll(stPetersSquare.getPlatforms());

        Set<IdForDTO> platformIds = platforms.stream().map(IdForDTO::createFor).collect(Collectors.toSet());

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, TramTime.of(17, 45), Altrincham, Ashton, false, 1);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = plan.getJourneys();
        assertFalse(journeys.isEmpty(), "no journeys");

        journeys.forEach(journey -> {
            VehicleStageDTO firstStage = (VehicleStageDTO) journey.getStages().get(0);
            PlatformDTO stategOnePlatform = firstStage.getPlatform();

            assertEquals("1", stategOnePlatform.getPlatformNumber());
            assertEquals( "Altrincham platform 1", stategOnePlatform.getName());
            assertEquals( IdForDTO.createFor(firstPlatformAtAlty), stategOnePlatform.getId());

            VehicleStageDTO secondStage = (VehicleStageDTO) journey.getStages().get(1);
            assertNotNull(secondStage);

            PlatformDTO secondStagePlatform = secondStage.getPlatform();

            // seems can be either 1,2 or 3
            String platformNumber = secondStagePlatform.getPlatformNumber();
            assertTrue("123".contains(platformNumber), "unexpected platform number, got " + platformNumber);

            List<String> expectedSecondStationNames = Arrays.asList(
                    Cornbrook.getName(),
                    Deansgate.getName(),
                    Piccadilly.getName(),
                    StPetersSquare.getName());

            // multiple possible places to change depending on timetable etc
            LocationRefDTO secondStagePlatformStation = secondStagePlatform.getStation();
            assertTrue(expectedSecondStationNames.contains(secondStagePlatformStation.getName()),
                    "did not expect " + secondStagePlatformStation.getName());

            assertTrue(platformIds.contains(secondStagePlatform.getId()), stategOnePlatform.getId() + " not in " + platformIds);

            List<ChangeStationRefWithPosition> changeStations = journey.getChangeStations();
            assertEquals(1, changeStations.size());
            ChangeStationRefWithPosition changeStation = changeStations.get(0);
            assertTrue(expectedSecondStationNames.contains(changeStation.getName()), "did not expect " + changeStation.getName());
            assertEquals(TransportMode.Tram, changeStation.getFromMode());

        });

    }

    @Test
    void testAltyToManAirportHasRealisticTranferAtCornbrook() {
        // Clousures on alty line during July on Sunday
        TramDate date = TestEnv.testDay(); // TestEnv.nextSunday();

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(date, TramTime.of(11, 0),
                Altrincham, ManAirport, false, 3);

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
    void shouldFindRouteDeansgateToVictoria() {
        validateAtLeastOneJourney(Deansgate, Victoria, when, TramTime.of(23,41));
    }

    @Test
    void shouldFindEndOfDayTwoStageJourney() {
        validateAtLeastOneJourney(TraffordCentre, TraffordBar, when, TramTime.of(23,30));
    }

    @Test
    void shouldFindEndOfDayThreeStageJourney() {
        validateAtLeastOneJourney(Altrincham, ShawAndCrompton, when, TramTime.of(22,45));
    }

    @Test
    void shouldOnlyReturnFullJourneysForEndOfDaysJourney() {
        JourneyPlanRepresentation results = validateAtLeastOneJourney(Deansgate,
                ManAirport, when, TramTime.of(23,5));

        assertFalse(results.getJourneys().isEmpty());
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

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(TestEnv.testDay(), TramTime.ofHourMins(queryTime.asLocalTime()), Altrincham, Ashton, false, 3);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Optional<JourneyDTO> findEarliest = results.getJourneys().stream().min(Comparator.comparing(JourneyDTO::getFirstDepartureTime));
        assertTrue(findEarliest.isPresent());

        JourneyDTO earliestJourney = findEarliest.get();

        TramTime firstDepart = TramTime.ofHourMins(earliestJourney.getFirstDepartureTime().toLocalTime());

        Duration elapsed = TramTime.difference(queryTime, firstDepart);
        assertTrue(Durations.lessThan(elapsed,Duration.ofMinutes(15)), "first result too far in future " + firstDepart + " for " + results.getJourneys());
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

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(date, queryTime, start, end, false, 3);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();

        String message = String.format(" from %s to %s at %s on %s ", start, end, queryTime, date);
        assertFalse(journeys.isEmpty(), "Unable to find journey " + message);
        checkDepartsAfterPreviousArrival(message, journeys);
        journeys.forEach(journey -> assertFalse(journey.getStages().isEmpty(), "Missing stages for journey "+journey));
        return results;
    }

}
