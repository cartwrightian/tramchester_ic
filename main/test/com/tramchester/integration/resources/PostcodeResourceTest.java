package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.id.PostcodeLocationId;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
import com.tramchester.repository.postcodes.PostcodeRepository;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.testTags.PostcodeTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.tramchester.testSupport.reference.KnownLocations.nearWythenshaweHosp;
import static org.junit.jupiter.api.Assertions.*;

@PostcodeTest
@ExtendWith(DropwizardExtensionsSupport.class)
class PostcodeResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new PostcodesOnlyEnabledResourceConfig());

    private final String endPoint = "postcodes";

    @Test
    void shouldGetLoadedPostcodes() {
        App app = appExtension.getApplication();
        GuiceContainerDependencies dependencies = app.getDependencies();
        PostcodeRepository postcodeRepository = dependencies.get(PostcodeRepository.class);

        PostcodeLocation expectedPostcode = postcodeRepository.getPostcode(PostcodeLocationId.create(TestPostcodes.postcodeForWythenshaweHosp()));

        Response response = APIClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, response.getStatus(), response.toString());

        List<LocationDTO> results = response.readEntity(new GenericType<>(){});

        assertFalse(results.isEmpty());

        IdForDTO expectedId = IdForDTO.createFor(expectedPostcode);
        Optional<LocationDTO> found = results.stream().
                filter(postcodeDTO -> postcodeDTO.getId().equals(expectedId)).findFirst();
        assertTrue(found.isPresent());

        LocationDTO result = found.get();

        assertEquals(LocationType.Postcode, result.getLocationType());
        assertEquals(TestPostcodes.postcodeForWythenshaweHosp(), result.getName());

        LatLong expected = nearWythenshaweHosp.latLong();
        LatLong position = result.getLatLong();
        assertEquals(expected.getLat(), position.getLat(), 0.01);
        assertEquals(expected.getLon(), position.getLon(), 0.01);
    }

    @Test
    void shouldGetTramStation304response() {
        Response resultA = APIClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, resultA.getStatus());

        Date lastMod = resultA.getLastModified();

        Response resultB = APIClient.getApiResponse(appExtension, endPoint, lastMod);
        assertEquals(304, resultB.getStatus());
    }

    private static class PostcodesOnlyEnabledResourceConfig extends TramWithPostcodesEnabled {

        public PostcodesOnlyEnabledResourceConfig() {
            super(LiveData.Disabled, Caching.Disabled);
        }

        @Override
        public boolean getPlanningEnabled() {
            return false;
        }


    }
}
