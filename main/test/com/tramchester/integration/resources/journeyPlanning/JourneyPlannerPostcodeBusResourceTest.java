package com.tramchester.integration.resources.journeyPlanning;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.SimpleStageDTO;
import com.tramchester.domain.presentation.DTO.query.JourneyQueryDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramAppTestExtension;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.testTags.BusTest;
import com.tramchester.testSupport.testTags.TramApp;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("wip for now")
@BusTest
@ExtendWith(TramAppTestExtension.class)
class JourneyPlannerPostcodeBusResourceTest {

    @TramApp
    private static IntegrationAppExtension appExtension = new IntegrationAppExtension(
            new IntegrationBusTestConfig());

    private TramDate day;
    private TramTime time;
    private JourneyResourceTestFacade journeyPlanner;

    @BeforeEach
    void beforeEachTestRuns() {
        day = TestEnv.testDay();
        time = TramTime.of(9,35);
        journeyPlanner = new JourneyResourceTestFacade(appExtension);
    }

    @AfterAll
    public static void onceAfterAllTestsRun() {
        appExtension.after();
        appExtension = null;
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcodeViaBus() {

        JourneyQueryDTO query = JourneyQueryDTO.create(day, time, TestPostcodes.CentralBury, TestPostcodes.NearPiccadillyGardens,
                false, 0);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();

        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> {
                assertEquals(3,journeyDTO.getStages().size(), journeyDTO.toString());
                assertEquals(TransportMode.Walk, journeyDTO.getStages().get(0).getMode());
                assertEquals(TransportMode.Walk, journeyDTO.getStages().get(2).getMode());
        });
    }

    @Test
    void shouldWalkFromPostcodeToNearbyStation() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(day, time, TestPostcodes.CentralBury, BusStations.BuryInterchange,
                false, 1);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> {
            assertEquals(1, journeyDTO.getStages().size());
            assertEquals(TransportMode.Walk, journeyDTO.getStages().get(0).getMode());
        });
    }

    @Test
    void shouldWalkFromStationToNearbyPostcode() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(day, time, BusStations.BuryInterchange, TestPostcodes.CentralBury,
                false, 1);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        Set<JourneyDTO> oneStage = journeys.stream().filter(journeyDTO -> journeyDTO.getStages().size() == 1).collect(Collectors.toSet());
        assertFalse(oneStage.isEmpty(), "no one stage in " + journeys);

        oneStage.forEach(journeyDTO -> {
            final List<SimpleStageDTO> stages = journeyDTO.getStages();
            assertEquals(TransportMode.Walk, stages.get(0).getMode(), stages.get(0).toString());
            assertEquals(BusStations.BuryInterchange.getIdForDTO(), journeyDTO.getBegin().getId());
        });
    }

    @Test
    void shouldPlanJourneyFromPostcodeToBusStation() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(day, time, TestPostcodes.CentralBury, BusStations.StopAtShudehillInterchange,
                false, 1);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            final List<SimpleStageDTO> stages = journey.getStages();
            assertTrue(stages.size()>=2, journey.toString());
            assertEquals(stages.get(0).getMode(), TransportMode.Walk, journey.toString());
            assertEquals(stages.get(stages.size()-1).getMode(), TransportMode.Bus);
        });
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcode() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(day, time, BusStations.StopAtShudehillInterchange, TestPostcodes.CentralBury,
                false, 1);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> {
            final List<SimpleStageDTO> stages = journeyDTO.getStages();
            assertEquals(stages.get(0).getMode(), TransportMode.Bus);
            assertEquals(stages.get(stages.size()-1).getMode(), TransportMode.Walk);
        });
    }

}
