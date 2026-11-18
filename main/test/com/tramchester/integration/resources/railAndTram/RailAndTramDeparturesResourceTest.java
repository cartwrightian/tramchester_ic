package com.tramchester.integration.resources.railAndTram;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.DestinationAndCallingPoints;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.query.DeparturesQueryDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.ResourceTramTrainTestConfig;
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
import com.tramchester.testSupport.TramAppTestExtension;
import com.tramchester.testSupport.conditional.RequiresNetwork;
import com.tramchester.testSupport.testTags.*;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.KnownLocations.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
@RequiresNetwork
@ExtendWith(TramAppTestExtension.class)
class RailAndTramDeparturesResourceTest {

    @TramApp
    private static IntegrationAppExtension appExtension = new IntegrationAppExtension(
            new ResourceTramTrainTestConfig<>(DeparturesResource.class, IntegrationTramTestConfig.LiveData.Enabled));

    private static APIClientFactory factory;

    private Station stationWithNotes;
    private Station stationWithDepartures;
    private PlatformMessageSource platformMessageSource;
    private LocalDateTime localDateTime;
    private StationRepository stationRepository;

    @BeforeAll
    public static void onceBeforeAll() {
        factory = appExtension.getApiClientFactory();
    }

    @AfterAll
    public static void onceAfterAllTestsRun() {
        appExtension.after();
        appExtension = null;
    }

    @BeforeEach
    void beforeEachTestRuns() {
        final App app =  appExtension.getApplication();
        final GuiceContainerDependencies dependencies = app.getDependencies();

        platformMessageSource = dependencies.get(PlatformMessageSource.class);
        stationRepository = dependencies.get(StationRepository.class);
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
                flatMap(station -> dueTramsSource.forStation(station, DestinationAndCallingPoints.None()).stream()).
                findAny();
        searchForDueTrams.ifPresent(dueTram -> {
            stationWithDepartures = dueTram.getDisplayLocation();
        });

        localDateTime = TestEnv.LocalNow();
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
    @LiveDataDueTramsTest
    void shouldGetDueTramsForStationWithinQuerytimeNow() {
        Station station = stationWithDepartures;

        LocalTime now = localDateTime.toLocalTime();
        DepartureListDTO result = getDeparturesForStation(station, now, EnumSet.of(Tram));
        SortedSet<DepartureDTO> departures = result.getDepartures();
        assertFalse(departures.isEmpty(), "no due trams at " + station);
        departures.forEach(depart -> assertEquals(new LocationRefDTO(station), depart.getFrom()));
    }

    @Test
    @LiveDataDueTramsTest
    void shouldGetDueTrainsForStationWithinQuerytimeNow() {
        Station station = RailStationIds.ManchesterPiccadilly.from(stationRepository);

        LocalTime now = localDateTime.toLocalTime();
        DepartureListDTO result = getDeparturesForStation(station, now,  EnumSet.of(Train));
        SortedSet<DepartureDTO> departures = result.getDepartures();
        assertFalse(departures.isEmpty(), "no due trains at " + station);
        departures.forEach(depart -> assertEquals(new LocationRefDTO(station), depart.getFrom()));
    }

    @Test
    @LiveDataDueTramsTest
    void shouldGetNearbyDeparturesQueryTimeNow() {
        LatLong where = betweenPiccAndPiccGardens.latLong();
        LocalTime queryTime = localDateTime.toLocalTime();

        DepartureListDTO result = getDeparturesForLatlongTime(where, queryTime, EnumSet.of(Tram, Train));
        assertFalse(result.isForJourney());
        SortedSet<DepartureDTO> departures = result.getDepartures();
        assertFalse(departures.isEmpty(), "no departures for " + where);

        Set<IdForDTO> departsFrom = departures.stream().
                map(departureDTO -> departureDTO.getFrom().getId()).
                collect(Collectors.toSet());

        assertTrue(departsFrom.contains(Piccadilly.getIdForDTO()),
                "Missing tram departure in " + departsFrom + " " + departures);
        assertTrue(departsFrom.contains(RailStationIds.ManchesterPiccadilly.getIdForDTO()),
                "Missing train departure in " + departsFrom + " " + departures);

        List<Note> notes = result.getNotes();
        assertFalse(notes.isEmpty(),"no notes for " + nearAltrinchamInterchange);

        Set<IdForDTO> notesDisplayedAt = notes.stream().
                filter(note -> !note.getDisplayedAt().isEmpty()).
                flatMap(note -> note.getDisplayedAt().stream()).
                map(LocationRefDTO::getId).
                collect(Collectors.toSet());

        assertTrue(notesDisplayedAt.contains(Piccadilly.getIdForDTO()), "Did not see altrincham departure in " + notes);
        assertFalse(notesDisplayedAt.contains(RailStationIds.ManchesterPiccadilly.getIdForDTO()),
                "Should not see train departure in " + notes);

    }

    private DepartureListDTO getDeparturesForLatlongTime(LatLong where, LocalTime queryTime, Set<TransportMode> modes) {
        DeparturesQueryDTO queryDTO = new DeparturesQueryDTO(LocationType.MyLocation, IdForDTO.createFor(where));
        queryDTO.setModes(modes);
        return getDepartureDTOS(queryTime, queryDTO);
    }

    private DepartureListDTO getDeparturesForStation(Station station, LocalTime queryTime, Set<TransportMode> modes) {
        DeparturesQueryDTO queryDTO = new DeparturesQueryDTO(LocationType.Station, IdForDTO.createFor(station));
        queryDTO.setModes(modes);
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

    @NotNull
    private Response getPostResponse(DeparturesQueryDTO queryDTO) {
        Response response = APIClient.postAPIRequest(factory, "departures/location", queryDTO);
        assertEquals(200, response.getStatus());
        return response;
    }


}
