package com.tramchester.integration.resources.journeyPlanning;

import com.tramchester.App;
import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.ChangeStationRefWithPosition;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.SimpleStageDTO;
import com.tramchester.domain.presentation.DTO.query.JourneyQueryDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.resources.JourneyPlannerResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.FakeStation;
import com.tramchester.testSupport.reference.KnownLocations;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static com.tramchester.testSupport.reference.KnownLocations.nearPiccGardens;
import static com.tramchester.testSupport.reference.TramStations.Deansgate;
import static com.tramchester.testSupport.reference.TramStations.NavigationRoad;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyPlannerLocationResourceTest {

    private static final AppConfiguration config = new ResourceTramTestConfig<>(JourneyPlannerResource.class);
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, config);

    private TramDate when;
    private JourneyResourceTestFacade journeyPlanner;

    @BeforeEach
    void beforeEachTestRuns() {
        journeyPlanner = new JourneyResourceTestFacade(appExtension);
        when = TestEnv.testDay();
    }

    @Test
    void shouldFindStationsNearPiccGardensToExchangeSquare() {
        validateJourneyFromLocation(nearPiccGardens, TramStations.ExchangeSquare, TramTime.of(9,0),
                false, when);
    }

    @Disabled("Unreliable")
    @Test
    void planRouteAllowingForWalkingTime() {

        // TODO Sort out the issues with this test to make it reliable

        // TODO remove plus 2 weeks
        TramDate whichDay = this.when;
        Set<JourneyDTO> journeys = validateJourneyFromLocation(nearAltrincham, Deansgate,
                TramTime.of(19,1), false, whichDay);
        assertFalse(journeys.isEmpty());

        JourneyDTO firstJourney = getEarliestArrivingJourney(journeys);

        List<SimpleStageDTO> stages = firstJourney.getStages();
        assertEquals(2, stages.size());
        SimpleStageDTO walkingStage = stages.get(0);
        LocalDateTime departureTime = walkingStage.getFirstDepartureTime();

        // todo new lockdown timetable
        List<LocalDateTime> possibleTimes = Arrays.asList(
                getDateTimeFor(whichDay, 19, 19),
                getDateTimeFor(whichDay, 19, 8),
                getDateTimeFor(whichDay, 19, 25));

        assertTrue(possibleTimes.contains(departureTime), departureTime + " not one of "+possibleTimes);

        // todo new lockdown timetable
        // 19:36 -> 20:00
        assertEquals(getDateTimeFor(whichDay, 20, 0), firstJourney.getExpectedArrivalTime(), firstJourney.toString());
    }

    private JourneyDTO getEarliestArrivingJourney(Set<JourneyDTO> journeys) {
        return journeys.stream().
                sorted(Comparator.comparing(JourneyDTO::getExpectedArrivalTime)).
                toList().get(0);
    }

    @Test
    void reproCacheStalenessIssueWithNearAltyToDeansgate() {
        final int count = 20; // 2000
        for (int i = 0; i < count; i++) {
            TramTime localTime = TramTime.of(10, 15);
            Set<JourneyDTO> journeys = validateJourneyFromLocation(nearAltrincham, Deansgate, localTime, false, when);
            assertFalse(journeys.isEmpty());

            LocalDateTime planTime = localTime.toDate(when);
            for (JourneyDTO result : journeys) {
                LocalDateTime departTime = result.getFirstDepartureTime();
                assertTrue(departTime.isAfter(planTime), result.toString());

                LocalDateTime arriveTime = result.getExpectedArrivalTime();
                assertTrue(arriveTime.isAfter(departTime), result.toString());
            }
        }
    }

    @Test
    void planRouteAllowingForWalkingTimeArriveBy() {
        TramTime queryTime = TramTime.of(20, 9);
        Set<JourneyDTO> journeys = validateJourneyFromLocation(nearAltrincham, Deansgate, queryTime, true, when);
        assertFalse(journeys.isEmpty());

        List<JourneyDTO> sorted = journeys.stream().
                sorted(Comparator.comparing(JourneyDTO::getExpectedArrivalTime)).
                toList();

        JourneyDTO earliest = sorted.get(0);

        final LocalDateTime query = queryTime.toDate(when);
        final LocalDateTime firstDepartureTime = earliest.getFirstDepartureTime();

        assertTrue(firstDepartureTime.isBefore(query));

        List<SimpleStageDTO> stages = earliest.getStages();

        assertEquals(2, stages.size());
    }

    @Test
    void shouldPlanRouteStartingInAWalk() {
        final TramTime queryTime = TramTime.of(20, 9);
        Set<JourneyDTO> journeys = validateJourneyFromLocation(nearAltrincham, Deansgate, queryTime, false, when);

        journeys.forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isAfter(queryTime.toDate(when)));

            List<SimpleStageDTO> stages = journeyDTO.getStages();

            assertEquals(2, stages.size(), stages.toString());
            assertEquals(TransportMode.Walk, stages.get(0).getMode());
            assertEquals(TransportMode.Tram, stages.get(1).getMode());

            List<ChangeStationRefWithPosition> changeStations = journeyDTO.getChangeStations();
            assertEquals(1, changeStations.size());

            ChangeStationRefWithPosition changeStation = changeStations.get(0);
            assertEquals(TransportMode.Walk, changeStation.getFromMode());

        });
    }

    @Test
    void shouldPlanRouteEndingInAWalk() {
        final TramTime queryTime = TramTime.of(20, 9);
        Set<JourneyDTO> journeys = validateJourneyToLocation(Deansgate, nearAltrincham, queryTime, false);

        journeys.forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isAfter(queryTime.toDate(when)));

            List<SimpleStageDTO> stages = journeyDTO.getStages();

            assertEquals(2, stages.size(), stages.toString());
            assertEquals(TransportMode.Tram, stages.get(0).getMode());

            assertEquals(TransportMode.Walk, stages.get(1).getMode());
            SimpleStageDTO walkingStage = stages.get(1);
            assertEquals(nearAltrincham.latLong(), walkingStage.getLastStation().getLatLong());

            List<ChangeStationRefWithPosition> changeStations = journeyDTO.getChangeStations();
            assertEquals(1, changeStations.size());

            ChangeStationRefWithPosition changeStation = changeStations.get(0);
            assertEquals(TransportMode.Tram, changeStation.getFromMode());
        });
    }

    @Test
    void shouldPlanRouteEndingInAWalkArriveBy() {
        TramTime queryTime = TramTime.of(19, 9);
        Set<JourneyDTO> results = validateJourneyToLocation(Deansgate, nearAltrincham, queryTime, true);

        int numberOfStages = 2;

        List<JourneyDTO> journeys = results.stream().
                filter(journeyDTO -> journeyDTO.getStages().size() == numberOfStages).toList();
        assertFalse(journeys.isEmpty());

        JourneyDTO firstJourney = journeys.get(0);
        assertTrue(firstJourney.getFirstDepartureTime().isBefore(queryTime.toDate(when)));

        List<SimpleStageDTO> stages = firstJourney.getStages();
        assertEquals(TransportMode.Tram, stages.get(0).getMode());
        int lastStageIndex = numberOfStages - 1;
        assertEquals(TransportMode.Walk, stages.get(lastStageIndex).getMode());
   }

    @Test
    void shouldGiveWalkingRouteFromMyLocationToNearbyStop() {
        Set<JourneyDTO> journeys = validateJourneyFromLocation(nearAltrincham, TramStations.Altrincham,
                TramTime.of(22, 9), false, when);
        assertFalse(journeys.isEmpty());
        JourneyDTO first = getEarliestArrivingJourney(journeys);

        List<SimpleStageDTO> stages = first.getStages();
        assertEquals(1, stages.size());
        SimpleStageDTO walkingStage = stages.get(0);
        assertEquals(getDateTimeFor(when, 22, 9), walkingStage.getFirstDepartureTime());
    }

    @Test
    void shouldGiveWalkingRouteFromStationToMyLocation() {
        Set<JourneyDTO> journeys = validateJourneyToLocation(TramStations.Altrincham, nearAltrincham,
                TramTime.of(22, 9), false);
        assertFalse(journeys.isEmpty());
        JourneyDTO first = getEarliestArrivingJourney(journeys);

        List<SimpleStageDTO> stages = first.getStages();
        assertEquals(1, stages.size());
        SimpleStageDTO walkingStage = stages.get(0);
        assertEquals(getDateTimeFor(when, 22, 9), walkingStage.getFirstDepartureTime());
    }

    @Test
    void shouldFindStationsNearPiccGardensWalkingOnly() {
        Set<JourneyDTO> journeys = validateJourneyFromLocation(nearPiccGardens, TramStations.PiccadillyGardens,
                TramTime.of(9,0), false, when);

        assertFalse(journeys.isEmpty());
        JourneyDTO first = getEarliestArrivingJourney(journeys);

        assertEquals(getDateTimeFor(when, 9, 0), first.getFirstDepartureTime(), "wrong departure time for " + first);
        assertEquals(getDateTimeFor(when, 9, 2), first.getExpectedArrivalTime(), "wrong arrival time for " + first);

        List<SimpleStageDTO> stages = first.getStages();
        assertEquals(1, stages.size());
        SimpleStageDTO stage = stages.get(0);
        assertEquals(getDateTimeFor(when, 9, 0), stage.getFirstDepartureTime());
        assertEquals(getDateTimeFor(when, 9, 2), stage.getExpectedArrivalTime());
    }

    @Test
    void shouldFindStationsNearPiccGardensWalkingOnlyArriveBy() {
        TramTime queryTime = TramTime.of(9, 0);
        Set<JourneyDTO> journeys = validateJourneyFromLocation(nearPiccGardens, TramStations.PiccadillyGardens,
                queryTime, true, when);

        journeys.forEach(journeyDTO -> {
            LocalDateTime queryTimeDate = journeyDTO.getQueryTime().toDate(journeyDTO.getQueryDate());

            assertFalse(queryTimeDate.isAfter(journeyDTO.getFirstDepartureTime()),
                    queryTimeDate + " is After dep time " + journeyDTO.getFirstDepartureTime() +
                            " for journey " + journeyDTO);
        });
    }


    @NotNull
    private LocalDateTime getDateTimeFor(TramDate when, int hour, int minute) {
        return LocalDateTime.of(when.toLocalDate(), LocalTime.of(hour, minute));
    }

    @Test
    void reproduceIssueNearAltyToAshton()  {
        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, TramTime.of(19,47), nearAltrincham.location(), 
            TramStations.Ashton, false, 3);
        
        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);
        Set<JourneyDTO> journeys = plan.getJourneys();

        assertFalse(journeys.isEmpty(), "no journeys for " + query);

        Optional<JourneyDTO> find3Stage = journeys.stream().filter(journeyDTO -> journeyDTO.getStages().size() == 3).findFirst();

        assertTrue(find3Stage.isPresent(), "No 3 stage journeys in " + journeys);
    }

    @Test
    void shouldFindRouteNearEndOfServiceTimes() {
        TramStations destination = Deansgate;

        TramTime queryTime = TramTime.of(23,0);
        int walkingTime = 13;

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, queryTime.plusMinutes(walkingTime), NavigationRoad, destination, false, 3);

        JourneyPlanRepresentation directFromStationNoWalking = journeyPlanner.getJourneyPlan(query);

        assertFalse(directFromStationNoWalking.getJourneys().isEmpty());
        // now check walking
        validateJourneyFromLocation(nearAltrincham, destination, queryTime, false, when);
        validateJourneyFromLocation(nearAltrincham, destination, queryTime, true, when);
    }

    private Set<JourneyDTO> validateJourneyFromLocation(KnownLocations start, FakeStation destination, TramTime queryTime,
                                                        boolean arriveBy, TramDate when) {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, queryTime, start.location(), destination, arriveBy, 3);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);
        return validateJourneyPresent(plan, query);
    }

    private Set<JourneyDTO> validateJourneyToLocation(FakeStation start, KnownLocations destination, TramTime queryTime, boolean arriveBy) {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(when, queryTime, start, destination.location(), arriveBy, 3);

        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(query);
        return validateJourneyPresent(plan, query);
    }

    @NotNull
    private Set<JourneyDTO> validateJourneyPresent(JourneyPlanRepresentation plan, JourneyQueryDTO query) {
        Set<JourneyDTO> journeys = plan.getJourneys();
        assertFalse(journeys.isEmpty(), "no journeys for " + query);

        journeys.forEach(journeyDTO -> {
            List<SimpleStageDTO> stages = journeyDTO.getStages();
            assertFalse(stages.isEmpty(), "no stages");
            stages.forEach(stage -> assertTrue(stage.getDuration()>0, stage.toString()));
        });

        return journeys;
    }

}
