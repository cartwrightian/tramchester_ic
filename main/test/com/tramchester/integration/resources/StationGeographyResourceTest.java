package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.AreaBoundaryDTO;
import com.tramchester.domain.presentation.DTO.BoxDTO;
import com.tramchester.domain.presentation.DTO.StationToStationConnectionDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.naptan.ResourceTramTestConfigWithNaptan;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.nptg.NPTGRepository;
import com.tramchester.resources.StationGeographyResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class StationGeographyResourceTest {
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfigWithNaptan<>(StationGeographyResource.class));

    private static GuiceContainerDependencies dependencies;
    private static APIClientFactory factory;
    private DTOFactory DTOFactory;
    private NPTGRepository nptgRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        App app = appExtension.getTestSupport().getApplication();
        dependencies = app.getDependencies();
        factory = new APIClientFactory(appExtension);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        DTOFactory = dependencies.get(DTOFactory.class);
        nptgRepository = dependencies.get(NPTGRepository.class);
    }

    // todo neighbours

    @Test
    void shouldGetStationLinks() {
        String endPoint = "geo/links";

        Response response = APIClient.getApiResponse(factory, endPoint);
        assertEquals(200, response.getStatus(), "status");

        List<StationToStationConnectionDTO> results = response.readEntity(new GenericType<>() {});

        assertEquals(TestEnv.NumberOfStationLinks, results.size(), "count incorrect " + results.size());

        StationToStationConnection.LinkType linkType = StationToStationConnection.LinkType.Linked;

        // parial closure Picc gardens
        assertTrue(results.contains(createLink(StPetersSquare, PiccadillyGardens, linkType)));
        assertFalse(results.contains(createLink(StPetersSquare, MarketStreet, linkType)));
        assertTrue(results.contains(createLink(PiccadillyGardens, StPetersSquare, linkType)));

        assertFalse(results.contains(createLink(MarketStreet, StPetersSquare, linkType)));

        assertTrue(results.contains(createLink(StPetersSquare, Deansgate, linkType)));

        assertTrue(results.contains(createLink(Deansgate, StPetersSquare, linkType)));
    }

    @Test
    void shouldGetQuadrants() {
        StationLocations stationLocations = dependencies.get(StationLocations.class);
        Set<BoundingBox> quadrants = stationLocations.getQuadrants();

        String endPoint = "geo/quadrants";
        Response response = APIClient.getApiResponse(factory, endPoint);
        assertEquals(200, response.getStatus(), "status");

        List<BoxDTO> results = response.readEntity(new GenericType<>() {});
        assertEquals(quadrants.size(), results.size());

        Set<BoundingBox> received = results.stream().
                map(dto -> new BoundingBox(CoordinateTransforms.getGridPosition(dto.getBottomLeft()),
                        CoordinateTransforms.getGridPosition(dto.getTopRight()))).collect(Collectors.toSet());

        assertTrue(quadrants.containsAll(received));
    }

    @Test
    void shouldGetBounds() {
        TramchesterConfig config = dependencies.get(TramchesterConfig.class);

        String endPoint = "geo/bounds";
        Response response = APIClient.getApiResponse(factory, endPoint);
        assertEquals(200, response.getStatus(), "status");

        BoxDTO result = response.readEntity(BoxDTO.class);

        BoundingBox expected = config.getBounds();

        assertEquals(expected.getTopRight(), CoordinateTransforms.getGridPosition(result.getTopRight()));
        assertEquals(expected.getBottomLeft(), CoordinateTransforms.getGridPosition(result.getBottomLeft()));
    }

    @Test
    void shouldGetLocality() {
        StationRepository stationRepository = dependencies.get(StationRepository.class);

        String endPoint = "geo/areas";

        Response response = APIClient.getApiResponse(factory, endPoint);
        assertEquals(200, response.getStatus(), "status");

        List<AreaBoundaryDTO> areas = response.readEntity(new GenericType<>() {});

        assertFalse(areas.isEmpty());

        Set<IdForDTO> areaIds = areas.stream().map(AreaBoundaryDTO::getAreaId).collect(Collectors.toSet());

        Station bury = Bury.from(stationRepository);
        IdFor<NPTGLocality> buryLocalityId = bury.getLocalityId();
        NPTGLocality record = nptgRepository.get(buryLocalityId);

        boolean found = areaIds.stream().anyMatch(areaId -> areaId.equals(IdForDTO.createFor(record)));

        assertTrue(found, buryLocalityId + " not found in " + areaIds);
    }

    private StationToStationConnectionDTO createLink(TramStations begin, TramStations end, StationToStationConnection.LinkType linkType) {
        Double distance = 42D; // not used in the equality for the DTO
        return new StationToStationConnectionDTO(DTOFactory.createLocationRefWithPosition(begin.fake()),
                DTOFactory.createLocationRefWithPosition(end.fake()),
                Collections.singleton(Tram), distance, linkType);
    }
}
