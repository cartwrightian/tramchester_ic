package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.PostcodeDTO;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
import com.tramchester.testSupport.*;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyPlannerPostcodeTramResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new TramWithPostcodesEnabled());

    private LocalDate day;
    private LocalTime time;

    @BeforeEach
    void beforeEachTestRuns() {
        day = TestEnv.testDay();
        time = LocalTime.of(9,35);
    }

    private String prefix(PostcodeLocation postcode) {
        return "POSTCODE_"+postcode.forDTO();
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcode() {
        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExtension,
                prefix(TestPostcodes.CentralBury), prefix(TestPostcodes.NearPiccadillyGardens), time, day,
                null, false, 5);

        Assertions.assertEquals(200, response.getStatus());
        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        // TODO WIP
        journeys.forEach(journeyDTO -> Assertions.assertEquals(3,journeyDTO.getStages().size()));
    }

    @Test
    void shouldPlanJourneyFromPostcodeToStation() {
        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExtension,
                prefix(TestPostcodes.CentralBury), TramStations.Piccadilly.forDTO(), time, day,
                null, false, 5);

        Assertions.assertEquals(200, response.getStatus());
        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        // TODO WIP
        journeys.forEach(journeyDTO -> Assertions.assertEquals(2,journeyDTO.getStages().size()));
    }

    @Test
    void shouldPlanJourneyFromStationToPostcode() {
        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExtension,
                TramStations.Piccadilly.forDTO(), prefix(TestPostcodes.CentralBury), time, day,
                null, false, 5);

        Assertions.assertEquals(200, response.getStatus());
        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> Assertions.assertEquals(2, journeyDTO.getStages().size()));
    }

}
