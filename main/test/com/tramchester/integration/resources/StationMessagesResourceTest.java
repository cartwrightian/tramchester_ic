package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StationMessagesDTO;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.repository.PlatformMessageSource;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.StationMessagesResource;
import com.tramchester.testSupport.testTags.LiveDataMessagesCategory;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class StationMessagesResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(StationMessagesResource.class, IntegrationTramTestConfig.LiveData.Enabled));

    private Station stationWithNotes;

    @BeforeEach
    void beforeEachTestRuns() {
        final App app = appExtension.getApplication();
        final GuiceContainerDependencies dependencies = app.getDependencies();

        PlatformMessageSource platformMessageSource = dependencies.get(PlatformMessageSource.class);
        StationRepository stationRepository = dependencies.get(StationRepository.class);
        ProvidesLocalNow providesLocalNow = dependencies.get(ProvidesLocalNow.class);

        TramDate queryDate = providesLocalNow.getTramDate();
        TramTime time = providesLocalNow.getNowHourMins();

        Optional<PlatformMessage> searchForMessage = stationRepository.getAllStationStream().
                map(station -> platformMessageSource.messagesFor(station, queryDate, time)).
                flatMap(Collection::stream).
                findAny();
        searchForMessage.ifPresent(platformMessage -> stationWithNotes = platformMessage.getStation());

    }

    @Test
    @LiveDataMessagesCategory
    void shouldHaveMessagesForStation() {
        assertNotNull(stationWithNotes, "No station with notes");
        Response response = getResponseForStation(stationWithNotes);
        assertEquals(200, response.getStatus());
        StationMessagesDTO departureList = response.readEntity(StationMessagesDTO.class);

        assertFalse(departureList.getNotes().isEmpty(), "no notes found for " + stationWithNotes.getName());
    }

    private Response getResponseForStation(Station station) {
        return APIClient.getApiResponse(appExtension, "stationMessages/" + IdForDTO.createFor(station).getActualId());
    }


}
