package com.tramchester.acceptance;

import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import io.dropwizard.testing.junit5.DropwizardAppExtension;

import java.util.List;

public class FetchAllStationsFromAPI {

    private final DropwizardAppExtension<AppConfiguration> appExtension;

    public FetchAllStationsFromAPI(DropwizardAppExtension<AppConfiguration> appExtension) {
        this.appExtension = appExtension;
    }

    public List<LocationRefDTO> getStations() {
        throw new RuntimeException("TODO");
//        Response result = APIClient.getApiResponse(appExtension, "stations/mode/Tram");
//        List<LocationRefDTO> results = result.readEntity(new GenericType<>() {});
//        return results;
    }
}
