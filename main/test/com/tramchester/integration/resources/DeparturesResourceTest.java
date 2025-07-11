package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.query.DeparturesQueryDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
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
import com.tramchester.resources.DeparturesResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.conditional.RequiresNetwork;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.LiveDataDueTramsTest;
import com.tramchester.testSupport.testTags.LiveDataMessagesTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.KnownLocations.nearAltrinchamInterchange;
import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static org.junit.jupiter.api.Assertions.*;

@RequiresNetwork
@ExtendWith(DropwizardExtensionsSupport.class)
class DeparturesResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(DeparturesResource.class, IntegrationTramTestConfig.LiveData.Enabled));
    private static APIClientFactory factory;

    private Station stationWithNotes;
    private Station stationWithDepartures;
    private PlatformMessageSource platformMessageSource;

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

        TramDate queryDate = providesLocalNow.getTramDate();
        TramTime time = providesLocalNow.getNowHourMins();

        // find locations with valid due trains and messages, needed for tests
        // not ideal but it works

        Optional<List<PlatformMessage>> searchForMessage = stationRepository.getAllStationStream().
                map(station -> platformMessageSource.messagesFor(station, queryDate, time)).
                filter(platformMessages -> !platformMessages.isEmpty()).
                limit(4).
                max(Comparator.comparingInt(List::size));

        searchForMessage.ifPresent(platformMessages -> stationWithNotes = platformMessages.getFirst().getStation());

        UpcomingDeparturesSource dueTramsSource = dependencies.get(TramDepartureRepository.class);

        Optional<UpcomingDeparture> searchForDueTrams = stationRepository.getAllStationStream().
                flatMap(station -> dueTramsSource.forStation(station).stream()).
                findAny();
        searchForDueTrams.ifPresent(dueTram -> {
            stationWithDepartures = dueTram.getDisplayLocation();
        });
    }

    @Test
    @LiveDataMessagesTest
    void shouldHaveAStationWithAMessage() {
        assertNotNull(stationWithNotes, "No station with notes");
    }

    @Test
    @LiveDataDueTramsTest
    void shouldHaveAStationWithDepartures() {
        assertNotNull(stationWithDepartures);
    }

    @Test
    @LiveDataMessagesTest
    void shouldHaveMessagesForStationWithNotes() {
        assertNotNull(stationWithNotes, "No station with notes");

        Response response = getResponseForStation(stationWithNotes);
        assertEquals(200, response.getStatus());
        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        assertFalse(departureList.getNotes().isEmpty(), "no notes in " + departureList + " for " + stationWithNotes.getName());

        assertFalse(departureList.isForJourney());
    }

    @Test
    @LiveDataMessagesTest
    void shouldHaveMessagesForSpecificStation() {

        DeparturesQueryDTO queryDTO = new DeparturesQueryDTO(LocationType.Station, IdForDTO.createFor(stationWithNotes));
        queryDTO.setNotesFor(Collections.singleton(IdForDTO.createFor(stationWithNotes)));
        Response response = getPostResponse(queryDTO);
        assertEquals(200, response.getStatus());
        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        List<Note> notes = departureList.getNotes();
        List<Note> liveNotes = notes.stream().filter(note -> note.getNoteType()== Note.NoteType.Live).toList();
        assertFalse(liveNotes.isEmpty());
    }

    @Test
    @LiveDataMessagesTest
    void shouldHaveNoMessagesForNoneExistentStation() {
        IdForDTO invalidStation = BusStations.KnutsfordStationStand3.getIdForDTO();

        DeparturesQueryDTO queryDTO = new DeparturesQueryDTO(LocationType.Station, IdForDTO.createFor(stationWithNotes));
        queryDTO.setNotesFor(Collections.singleton(invalidStation));
        Response response = getPostResponse(queryDTO);
        assertEquals(200, response.getStatus());
        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        List<Note> notes = departureList.getNotes();
        List<Note> liveNotes = notes.stream().filter(note -> note.getNoteType()== Note.NoteType.Live).toList();
        assertTrue(liveNotes.isEmpty(), "got unexpectedNotes " + liveNotes);
    }

    @Test
    @LiveDataDueTramsTest
    void shouldGetDueTramsForStationWithinQuerytimeNow() {
        Station station = stationWithDepartures;

        LocalTime now = TestEnv.LocalNow().toLocalTime();
        DepartureListDTO result = getDeparturesForStation(station, now);
        SortedSet<DepartureDTO> departures = result.getDepartures();
        assertFalse(departures.isEmpty(), "no due trams at " + station);
        departures.forEach(depart -> assertEquals(new LocationRefDTO(station), depart.getFrom()));
    }

    @Test
    @LiveDataDueTramsTest
    void shouldGetDueTramsForStationWithQuerytimePast() {
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().minusMinutes(120);

        DepartureListDTO result = getDeparturesForStation(stationWithDepartures, queryTime);
        SortedSet<DepartureDTO> departures = result.getDepartures();
        assertTrue(departures.isEmpty());
        assertTrue(result.getNotes().isEmpty());
    }

    @Test
    @LiveDataDueTramsTest
    void shouldGetDueTramsForStationWithQuerytimeFuture() {
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().plusMinutes(120);

        DepartureListDTO result = getDeparturesForStation(stationWithDepartures, queryTime);
        SortedSet<DepartureDTO> departures = result.getDepartures();

        assertTrue(departures.isEmpty());
        assertTrue(result.getNotes().isEmpty());
    }

    @Test
    @LiveDataDueTramsTest
    void shouldGetNearbyDeparturesQuerytimeNow() {
        LatLong where = nearAltrinchamInterchange.latLong();
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime();

        DepartureListDTO result = getDeparturesForLatlongTime(where, queryTime);
        assertFalse(result.isForJourney());
        SortedSet<DepartureDTO> departures = result.getDepartures();
        assertFalse(departures.isEmpty(), "no departures for " + nearAltrinchamInterchange);

        Set<IdForDTO> departsFrom = departures.stream().
                map(departureDTO -> departureDTO.getFrom().getId()).
                collect(Collectors.toSet());

        assertTrue(departsFrom.contains(Altrincham.getIdForDTO()), "Did see altrincham departure in " + departures);

        List<Note> notes = result.getNotes();
        assertFalse(notes.isEmpty(),"no notes for " + nearAltrinchamInterchange);

        Set<IdForDTO> notesDisplayedAt = notes.stream().
                flatMap(note -> note.getDisplayedAt().stream()).
                map(LocationRefDTO::getId).
                collect(Collectors.toSet());

        assertTrue(notesDisplayedAt.contains(Altrincham.getIdForDTO()), "Did see altrincham departure in " + notes);
    }

    @Test
    @LiveDataDueTramsTest
    void shouldGetNearbyDeparturesQuerytimeFuture() {
        LatLong latLong = new LatLong(53.4804263d, -2.2392436d);
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().plusMinutes(120);
        //SortedSet<DepartureDTO> departures = getDeparturesForLatlongTime(latLong, queryTime);

        DepartureListDTO result = getDeparturesForLatlongTime(latLong, queryTime);
        assertFalse(result.isForJourney());

        SortedSet<DepartureDTO> departures = result.getDepartures();

        assertTrue(departures.isEmpty());
        assertTrue(result.getNotes().isEmpty());
    }

    @Test
    @LiveDataDueTramsTest
    void shouldGetNearbyDeparturesQuerytimePast() {
        LatLong latLong = new LatLong(53.4804263d, -2.2392436d);
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().minusMinutes(120);

        //SortedSet<DepartureDTO> departures = getDeparturesForLatlongTime(latLong, queryTime);
        DepartureListDTO result = getDeparturesForLatlongTime(latLong, queryTime);
        assertFalse(result.isForJourney());

        SortedSet<DepartureDTO> departures = result.getDepartures();

        assertTrue(departures.isEmpty());
        assertTrue(result.getNotes().isEmpty());
    }

    @Test
    @LiveDataDueTramsTest
    void shouldNotGetNearbyIfOutsideOfThreshold() {
        LatLong where = nearAltrinchamInterchange.latLong();

        final Set<IdForDTO> nearAlty = Stream.of(Altrincham, TramStations.NavigationRoad).
                map(TramStations::getIdForDTO).collect(Collectors.toSet());

        Response response = getResponseForLocation(where);
        assertEquals(200, response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        assertFalse(departureList.isForJourney());

        SortedSet<DepartureDTO> departures = departureList.getDepartures();
        assertFalse(departures.isEmpty(), "no departures");

        DepartureDTO departureDTO = departures.first();
        LocalDateTime when = departureDTO.getDueTime();

        TramTime nowMinus5mins = TramTime.ofHourMins(TestEnv.LocalNow().toLocalTime().minusMinutes(6));
        assertTrue(when.toLocalTime().isAfter(nowMinus5mins.asLocalTime()), when.toString());

        LocationRefDTO nextDepart = departureDTO.getFrom();
        assertTrue(nearAlty.contains(nextDepart.getId()), nextDepart.toString());
        assertFalse(departureDTO.getStatus().isEmpty());

    }

    private DepartureListDTO getDeparturesForLatlongTime(LatLong where, LocalTime queryTime) {
        DeparturesQueryDTO queryDTO = new DeparturesQueryDTO(LocationType.MyLocation, IdForDTO.createFor(where));
        return getDepartureDTOS(queryTime, queryDTO);
    }

    private DepartureListDTO getDeparturesForStation(Station station, LocalTime queryTime) {
        DeparturesQueryDTO queryDTO = new DeparturesQueryDTO(LocationType.Station, IdForDTO.createFor(station));
        return getDepartureDTOS(queryTime, queryDTO);
    }

    private DepartureListDTO getDepartureDTOS(LocalTime time, DeparturesQueryDTO queryDTO) {
        queryDTO.setTime(time);
        Response response = getPostResponse(queryDTO);
        return response.readEntity(DepartureListDTO.class);
    }

    private Response getResponseForStation(Station station) {
        DeparturesQueryDTO queryDTO = new DeparturesQueryDTO(LocationType.Station, IdForDTO.createFor(station));
        return getPostResponse(queryDTO);
    }

    private Response getResponseForLocation(LatLong where) {
        DeparturesQueryDTO queryDTO = new DeparturesQueryDTO(LocationType.MyLocation, IdForDTO.createFor(where));
        return getPostResponse(queryDTO);
    }

    @NotNull
    private Response getPostResponse(DeparturesQueryDTO queryDTO) {
        Response response = APIClient.postAPIRequest(factory, "departures/location", queryDTO);
        assertEquals(200, response.getStatus());
        return response;
    }


}
