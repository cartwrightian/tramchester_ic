package com.tramchester.acceptance;

import com.tramchester.acceptance.infra.AcceptanceAppExtenstion;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import java.util.List;

public class FetchAllStationsFromAPI {

    private final APIClientFactory apiClientFactory;

    public FetchAllStationsFromAPI(AcceptanceAppExtenstion appExtension) {
        apiClientFactory = new APIClientFactory(appExtension);
    }

    public List<LocationRefDTO> getStations() {
        Response result = APIClient.getApiResponse(apiClientFactory, "stations/mode/Tram");

        return result.readEntity(new GenericType<>() {});
    }

}
