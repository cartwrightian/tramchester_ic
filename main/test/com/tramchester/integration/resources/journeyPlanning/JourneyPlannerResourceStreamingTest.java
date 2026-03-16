package com.tramchester.integration.resources.journeyPlanning;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.resources.JourneyPlannerResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramAppTestExtension;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.TramApp;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;

@ExtendWith(TramAppTestExtension.class)
public class JourneyPlannerResourceStreamingTest {

    @TramApp
    private static IntegrationAppExtension appExtension =
            new IntegrationAppExtension(new ResourceTramTestConfig<>(JourneyPlannerResource.class));

    private TramDate when;
    private JourneyResourceTestFacade journeyPlanner;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        journeyPlanner = new JourneyResourceTestFacade(appExtension);
    }

    @AfterAll
    public static void onceAfterAllTestsRun() {
        appExtension.after();
        appExtension = null;
    }

    @Test
    void shouldGetResultsAsStream() throws IOException {
        TramchesterConfig config = appExtension.getConfiguration();
        final int maxChanges = config.getMaxNumberChanges();

        final boolean arriveBy = false;

        List<JourneyDTO> journeyDTOS = journeyPlanner.getJourneyPlanStreamed(when, TramTime.of(11,45),
                TramStations.StPetersSquare, TramStations.ManAirport, arriveBy, maxChanges);

        Assertions.assertFalse(journeyDTOS.isEmpty());
        journeyDTOS.forEach(journeyDTO -> Assertions.assertFalse(journeyDTO.getStages().isEmpty()));
    }

}
