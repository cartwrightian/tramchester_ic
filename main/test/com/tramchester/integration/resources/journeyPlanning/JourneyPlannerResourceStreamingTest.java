package com.tramchester.integration.resources.journeyPlanning;

import com.tramchester.App;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.resources.JourneyPlannerResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;

@ExtendWith(DropwizardExtensionsSupport.class)
public class JourneyPlannerResourceStreamingTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceTramTestConfig<>(JourneyPlannerResource.class));

    private TramDate when;
    private JourneyResourceTestFacade journeyPlanner;

//    @AfterAll
//    static void afterAll() {
//        appExtension.after();
//    }

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        journeyPlanner = new JourneyResourceTestFacade(appExtension);
    }

    @Test
    void shouldGetResultsAsStream() throws IOException {
        final int maxChanges = 3;
        final boolean arriveBy = false;

        List<JourneyDTO> journeyDTOS = journeyPlanner.getJourneyPlanStreamed(when, TramTime.of(11,45),
                TramStations.StPetersSquare, TramStations.ManAirport, arriveBy, maxChanges);

        Assertions.assertFalse(journeyDTOS.isEmpty());
        journeyDTOS.forEach(journeyDTO -> Assertions.assertFalse(journeyDTO.getStages().isEmpty()));
    }

}
