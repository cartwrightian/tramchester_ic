package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.query.DeparturesQueryDTO;
import com.tramchester.domain.presentation.DTO.query.JourneyQueryDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.DTO.DepartureListDTO;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.PlatformMessageSource;
import com.tramchester.livedata.repository.UpcomingDeparturesSource;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.testTags.LiveDataMessagesCategory;
import com.tramchester.testSupport.testTags.LiveDataTestCategory;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class DeparturesAfterJourneyResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(IntegrationTramTestConfig.LiveData.Enabled, true));
    private static APIClientFactory factory;

    private Station stationWithNotes;
    private Station stationWithDepartures;
    private PlatformMessageSource platformMessageSource;
    private Station destinationForDueTram;
    private JourneyResourceTestFacade journeyPlanner;
    private TramDate queryDate;
    private TramTime time;


    @BeforeAll
    public static void onceBeforeAll() {
        factory = new APIClientFactory(appExtension);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        final App app =  appExtension.getApplication();
        final GuiceContainerDependencies dependencies = app.getDependencies();

        platformMessageSource = dependencies.get(PlatformMessageSource.class);
        StationRepository stationRepository = dependencies.get(StationRepository.class);
        ProvidesLocalNow providesLocalNow = dependencies.get(ProvidesLocalNow.class);

        queryDate = providesLocalNow.getTramDate();
        time = providesLocalNow.getNowHourMins();

        journeyPlanner = new JourneyResourceTestFacade(appExtension);


        // find locations with valid due trains and messages, needed for tests
        // not ideal but it works

        Optional<PlatformMessage> searchForMessage = stationRepository.getAllStationStream().
                map(station -> platformMessageSource.messagesFor(station, queryDate, time)).
                flatMap(Collection::stream).
                findAny();
        searchForMessage.ifPresent(platformMessage -> stationWithNotes = platformMessage.getStation());

        UpcomingDeparturesSource dueTramsSource = dependencies.get(TramDepartureRepository.class);

        Optional<UpcomingDeparture> searchForDueTrams = stationRepository.getAllStationStream().
                flatMap(station -> dueTramsSource.forStation(station).stream()).
                findAny();
        searchForDueTrams.ifPresent(dueTram -> {
            stationWithDepartures = dueTram.getDisplayLocation();
            destinationForDueTram = dueTram.getDestination();
        });
    }

    @Test
    @LiveDataMessagesCategory
    void shouldHaveAStationWithAMessage() {
        assertNotNull(stationWithNotes, "No station with notes");
    }

    @Test
    @LiveDataTestCategory
    void shouldHaveAStationWithDepartures() {
        assertNotNull(stationWithDepartures);
    }

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsWithCorrectDueTramsHighlighted() {
        Station station = stationWithDepartures;

        SortedSet<DepartureDTO> departures = getDeparturesForStationAfterJourney(station, destinationForDueTram);
        assertFalse(departures.isEmpty(), "no due trams at " + station);

        departures.forEach(depart -> assertEquals(new LocationRefDTO(station), depart.getFrom()));

        IdForDTO departureID = IdForDTO.createFor(destinationForDueTram.getId());
        boolean haveTowardsDest = departures.stream().anyMatch(departureDTO -> departureDTO.getDestination().getId().equals(departureID));

        assertTrue(haveTowardsDest, "Did not find " + departureID + " in " + departures);

        Optional<DepartureDTO> towardsDestination = departures.stream().filter(DepartureDTO::getMatchesJourney).findAny();
        assertTrue(towardsDestination.isPresent(), "no tram flagged as matching journey to " + destinationForDueTram.getId() + " from " + station.getId()
            + " in " + departures);
    }

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForLocationWithinQueryTimeNowAndDestinationMatching() {
        LatLong latLong = stationWithDepartures.getLatLong();

        MyLocation myLocation = new MyLocation(latLong);

        SortedSet<DepartureDTO> departures = getDeparturesForStationAfterJourney(myLocation, destinationForDueTram);
        assertFalse(departures.isEmpty(), "no due trams at " + latLong);

        Optional<DepartureDTO> towardsDestination = departures.stream().filter(DepartureDTO::getMatchesJourney).findAny();
        assertTrue(towardsDestination.isPresent(), "no tram towards " + destinationForDueTram.getId() + " from close to " + stationWithDepartures.getId());
    }


    private SortedSet<DepartureDTO> getDeparturesForStationAfterJourney(Location<?> displayLocation, Station destinationForDueTram) {
        DeparturesQueryDTO queryDTO = new DeparturesQueryDTO(displayLocation.getLocationType(), IdForDTO.createFor(displayLocation));

        JourneyQueryDTO journeyQueryDto = journeyPlanner.getQueryDTO(queryDate, time, displayLocation,
                destinationForDueTram, false, 3);
        JourneyPlanRepresentation plan = journeyPlanner.getJourneyPlan(journeyQueryDto);

        assertFalse(plan.getJourneys().isEmpty(), "could find journey from " + displayLocation.getId() + " to " + destinationForDueTram.getId());

        queryDTO.setJourneys(plan.getJourneys());

        return getDepartureDTOS(queryDTO);
    }


    private SortedSet<DepartureDTO> getDepartureDTOS(DeparturesQueryDTO queryDTO) {
        //queryDTO.setTime(time);

        Response response = getPostResponse(queryDTO);

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        return departureList.getDepartures();
    }

    @NotNull
    private Response getPostResponse(DeparturesQueryDTO queryDTO) {
        Response response = APIClient.postAPIRequest(factory, "departures/location", queryDTO);
        assertEquals(200, response.getStatus());
        return response;
    }


}