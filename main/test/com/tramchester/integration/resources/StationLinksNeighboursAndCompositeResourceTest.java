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
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.BusTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

    private static final AppConfiguration configuration = new NeighboursTestConfig();
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, configuration);
    private static GuiceContainerDependencies dependencies;

    private StationGroup shudehillCompositeBus;
    private Station shudehillTram;
    private BusStations.CentralStops centralStops;

    @BeforeAll
    public static void beforeAnyTestsRun() {
        App app = appExtension.getTestSupport().getApplication();
        dependencies = app.getDependencies();
    }

    @BeforeEach
    public void onceBeforeEachTest() {
        StationRepository stationRepository = dependencies.get(StationRepository.class);

        centralStops = new BusStations.CentralStops(dependencies);

//        String shudehill_interchange = "Shudehill Interchange";
        shudehillCompositeBus = centralStops.Shudehill(); //stationGroupsRepository.findByName(shudehill_interchange);
        shudehillTram = stationRepository.getStationById(Shudehill.getId());
    }

    @Test
    void shouldGetStationNeighboursFromTram() {

        List<StationLinkDTO> results = getLinks();

        Set<IdForDTO> fromShudehillTram = results.stream().
                filter(link -> link.getBegin().getId().equals(IdForDTO.createFor(shudehillTram))).
                map(link -> link.getEnd().getId()).
                collect(Collectors.toSet());

        assertFalse(fromShudehillTram.isEmpty());

        shudehillCompositeBus.getContained().forEach(busStop ->
                assertTrue(fromShudehillTram.contains(IdForDTO.createFor(busStop)), "missing " + busStop.getId()));
    }

    @Test
    void shouldGetStationNeighboursFromBus() {

        List<StationLinkDTO> results = getLinks();

        Set<IdForDTO> busStopIds = shudehillCompositeBus.getContained().
                stream().
                map(IdForDTO::createFor).
                collect(Collectors.toSet());

        final Set<StationLinkDTO> fromShudehillBusStops = results.stream().
                filter(link -> busStopIds.contains(link.getBegin().getId())).
                collect(Collectors.toSet());

        assertFalse(fromShudehillBusStops.isEmpty());

        Set<IdForDTO> fromShudehillBusToTram = fromShudehillBusStops.stream().
                filter(link -> IdForDTO.createFor(shudehillTram).equals(link.getEnd().getId())).
                map(link -> link.getBegin().getId()).
                collect(Collectors.toSet());

        assertFalse(fromShudehillBusToTram.isEmpty());
    }

    @Test
    void expectedNumbers() {
        List<StationLinkDTO> results = getLinks();
        assertEquals(2476, results.size(), "count of links");
    }

    @Test
    void shouldGetCompositeStations() {

//        final String altrinchamInterchangeName = composites.AltrinchamInterchange();
        StationGroup actualComposite = centralStops.Altrincham();
        Set<IdForDTO> expectedIds = actualComposite.getContained().stream().
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
    private List<StationLinkDTO> getLinks() {

        Response response = APIClient.getApiResponse(appExtension, "links/neighbours");
        assertEquals(200, response.getStatus(), "status");

       return response.readEntity(new GenericType<>() {});

    }

}
