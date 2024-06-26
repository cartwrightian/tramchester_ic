package com.tramchester.integration.resources.journeyPlanning;

import com.tramchester.App;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.query.JourneyQueryDTO;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Disabled("Postcode planning not currently in use")
@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyPlannerPostcodeTramResourceTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new TramWithPostcodesEnabled(IntegrationTramTestConfig.LiveData.Disabled, IntegrationTramTestConfig.Caching.Enabled));

    private TramDate day;
    private TramTime time;
    private JourneyResourceTestFacade journeyPlanner;

    @BeforeEach
    void beforeEachTestRuns() {
        day = TestEnv.testDay();
        time = TramTime.of(9,35);
        journeyPlanner = new JourneyResourceTestFacade(appExtension);
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcode() {

        JourneyQueryDTO query = JourneyQueryDTO.create(day, time, TestPostcodes.CentralBury, TestPostcodes.NearPiccadillyGardens, false, 5);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        // TODO WIP
        journeys.forEach(journeyDTO -> Assertions.assertEquals(3,journeyDTO.getStages().size()));
    }

    @Test
    void shouldPlanJourneyFromPostcodeToStation() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(day, time, TestPostcodes.CentralBury, TramStations.Piccadilly, false, 5);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        // TODO WIP
        journeys.forEach(journeyDTO -> Assertions.assertEquals(2,journeyDTO.getStages().size()));
    }

    @Test
    void shouldPlanJourneyFromStationToPostcode() {

        JourneyQueryDTO query = journeyPlanner.getQueryDTO(day, time, TramStations.Piccadilly, TestPostcodes.CentralBury, false, 5);

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(query);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> Assertions.assertEquals(2, journeyDTO.getStages().size()));
    }

}
