package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.StationGroupDTO;
import com.tramchester.domain.presentation.DTO.StationToStationConnectionDTO;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.config.IntegrationTramBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.BusTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
@ExtendWith(DropwizardExtensionsSupport.class)
class StationLinksNeighboursAndCompositeResourceTest {

    private static final AppConfiguration configuration = new IntegrationTramBusTestConfig(true);

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, configuration);
    private static GuiceContainerDependencies dependencies;

    private StationGroup shudehillCentralBusStops;
    private IdForDTO shudehillTramId;
    private StationGroupsRepository stationGroupsRepository;

    @BeforeAll
    public static void beforeAnyTestsRun() {
        App app = appExtension.getTestSupport().getApplication();
        dependencies = app.getDependencies();
    }

    @BeforeEach
    public void onceBeforeEachTest() {
        StationRepository stationRepository = dependencies.get(StationRepository.class);
        stationGroupsRepository = dependencies.get(StationGroupsRepository.class);

        shudehillCentralBusStops = KnownLocality.Shudehill.from(stationGroupsRepository);
        Station shudehillTram = stationRepository.getStationById(Shudehill.getId());

        shudehillTramId = IdForDTO.createFor(shudehillTram);

    }

    @Disabled("needs trams and buses enabled")
    @Test
    void shouldGetStationNeighboursFromTram() {
        Set<IdForDTO> busStopIds = shudehillCentralBusStops.getAllContained().stream().
                map(Station::getId).
                map(IdForDTO::new).
                collect(Collectors.toSet());

        List<StationToStationConnectionDTO> results = getLinks();

        Set<IdForDTO> beginAtTramStop = results.stream().
                filter(link -> link.getBegin().getId().equals(shudehillTramId)).
                map(link -> link.getEnd().getId()).
                collect(Collectors.toSet());

        assertFalse(beginAtTramStop.isEmpty());

        Set<IdForDTO> endAtBusStop = beginAtTramStop.stream().filter(busStopIds::contains).collect(Collectors.toSet());

        assertFalse(endAtBusStop.isEmpty(), beginAtTramStop.toString());
    }

    @Test
    void shouldGetStationNeighboursFromBus() {

        List<StationToStationConnectionDTO> results = getLinks();

        Set<IdForDTO> busStopIds = shudehillCentralBusStops.getAllContained().
                stream().
                map(IdForDTO::createFor).
                collect(Collectors.toSet());

        Set<IdForDTO> fromShudehillBusToTram = results.stream().
                filter(link -> busStopIds.contains(link.getBegin().getId())).
                map(link -> link.getBegin().getId()).
                collect(Collectors.toSet());

        assertFalse(fromShudehillBusToTram.isEmpty());
    }

    @Test
    void expectedNumberOfLinks() {
        List<StationToStationConnectionDTO> results = getLinks();
        assertEquals(20300, results.size(), "count of links");
    }

    @Disabled("needs trams and buses enabled")
    @Test
    void shouldGetCompositeStations() {

        StationGroup actualComposite = KnownLocality.Altrincham.from(stationGroupsRepository);
        Set<IdForDTO> expectedIds = actualComposite.getAllContained().stream().
                map(IdForDTO::createFor).
                collect(Collectors.toSet());

        Response response = APIClient.getApiResponse(appExtension, "links/composites");
        assertEquals(200, response.getStatus(), "status");

        List<StationGroupDTO> groups = response.readEntity(new GenericType<>() {});
        assertFalse(groups.isEmpty());

        IdFor<NPTGLocality> expectedAreaId = actualComposite.getLocalityId();
        Optional<StationGroupDTO> found = groups.stream().
                filter(item -> item.getAreaId().equals(new IdForDTO(expectedAreaId))).findFirst();
        assertTrue(found.isPresent());

        StationGroupDTO group = found.get();
        assertEquals(expectedIds.size(), group.getContained().size());

        Set<IdForDTO> receivedIds = group.getContained().stream().
                map(LocationRefDTO::getId).
                collect(Collectors.toSet());

        assertTrue(expectedIds.containsAll(receivedIds));
    }

    @NotNull
    private List<StationToStationConnectionDTO> getLinks() {

        Response response = APIClient.getApiResponse(appExtension, "geo/links");
        assertEquals(200, response.getStatus(), "status");

       return response.readEntity(new GenericType<>() {});

    }

}
