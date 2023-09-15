package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.StationNote;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.VehicleStage;
import com.tramchester.domain.transportStages.WalkingToStationStage;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.repository.PlatformMessageSource;
import com.tramchester.livedata.tfgm.ProvidesTramNotes;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocations;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static com.tramchester.domain.presentation.Note.NoteType.Live;
import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static com.tramchester.testSupport.reference.KnownLocations.nearPiccGardens;
import static com.tramchester.testSupport.reference.TramStations.*;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProvidesTramNotesTest extends EasyMockSupport {
    private ProvidesTramNotes providesNotes;
    private PlatformMessageSource platformMessageSource;
    private LocalDateTime lastUpdate;
    private final int requestedNumberChanges = 2;
    private DTOFactory stationDTOFactory;
    private ProvidesLocalNow providesLocalNow;
    private final int journeyIndex = 42;

    @BeforeEach
    void beforeEachTestRuns() {
        platformMessageSource = createMock(PlatformMessageSource.class);
        stationDTOFactory = createMock(DTOFactory.class);
        providesNotes = new ProvidesTramNotes(platformMessageSource, stationDTOFactory);
        providesLocalNow = new ProvidesLocalNow();
        lastUpdate = providesLocalNow.getDateTime();
    }

    private Station createStationFor(TramStations tramStation) {
        return tramStation.fake();
    }

    private LocationRefDTO createStationRefFor(TramStations station) {
        return new LocationRefDTO(createStationFor(station));
    }

    @Test
    void shouldAddNotesForSaturdayJourney() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        TramDate queryDate = TramDate.of(2022,7,9);

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andReturn(Collections.singleton(Tram));
        EasyMock.expect(journey.getCallingPlatformIds()).andReturn(IdSet.emptySet());

        replayAll();
        List<Note> result = providesNotes.createNotesForJourney(journey, queryDate);
        verifyAll();

        assertThat(result, hasItem(new Note(ProvidesTramNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldStillAddNotesForSaturdayJourneySourceDisabled() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(false);

        TramDate queryDate = TramDate.of(2016,10,29);

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andReturn(Collections.singleton(Tram));

        replayAll();
        List<Note> result = providesNotes.createNotesForJourney(journey, queryDate);
        verifyAll();

        assertThat(result, hasItem(new Note(ProvidesTramNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldNotAddNotesForWeekendWhenNoTramInvolved() {

        TramDate queryDate = TramDate.of(2016,10,29);

        Journey journey = createMock(Journey.class);
        Set<TransportMode> modes = new HashSet<>(Arrays.asList(Train, Bus, Walk));
        EasyMock.expect(journey.getTransportModes()).andReturn(modes);

        replayAll();
        List<Note> result = providesNotes.createNotesForJourney(journey, queryDate);
        verifyAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldAddNotesForSundayJourney() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);
        TramDate queryDate = TramDate.of(2016,10,30);

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andReturn(Collections.singleton(Tram));
        EasyMock.expect(journey.getCallingPlatformIds()).andReturn(IdSet.emptySet());

        replayAll();
        List<Note> result = providesNotes.createNotesForJourney(journey, queryDate);
        verifyAll();

        assertThat(result, hasItem(new Note(ProvidesTramNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldNotShowNotesOnOtherDay() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        TramDate queryDate = TramDate.of(2016,10,31);

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andReturn(Collections.singleton(Tram));
        EasyMock.expect(journey.getCallingPlatformIds()).andReturn(IdSet.emptySet());

        replayAll();
        List<Note> result = providesNotes.createNotesForJourney(journey, queryDate);
        verifyAll();

        assertThat(result, not(hasItem(new Note(ProvidesTramNotes.weekend, Note.NoteType.Weekend))));
    }

    @Test
    void shouldHandleDisabled() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(false);

        TramDate queryDate = TramDate.of(2016,10,31);

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andReturn(Collections.singleton(Tram));

        replayAll();
        List<Note> result = providesNotes.createNotesForJourney(journey, queryDate);
        verifyAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHaveNoteForChristmasTramServices() {
        EasyMock.expect(platformMessageSource.isEnabled()).andStubReturn(true);

        int year = 2021;
        TramDate date = TramDate.of(year, 12, 23);
        Note christmasNote = new Note(ProvidesTramNotes.christmas2021, Note.NoteType.Christmas);

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andStubReturn(Collections.singleton(Tram));
        EasyMock.expect(journey.getCallingPlatformIds()).andStubReturn(IdSet.emptySet());

        replayAll();

        List<Note> result = providesNotes.createNotesForJourney(journey, date);
        assertThat(result, not(hasItem(christmasNote)));

        for(int offset=1; offset<11; offset++) {
            TramDate queryDate = date.plusDays(offset);
            result = providesNotes.createNotesForJourney(journey, queryDate);
            assertThat(queryDate.toString(), result, hasItem(christmasNote));
        }

        date = TramDate.of(year+1, 1, 3);

        result = providesNotes.createNotesForJourney(journey, date);

        verifyAll();

        assertThat(result, not(hasItem(christmasNote)));
    }

    @Test
    void shouldHaveNoNoteForChristmasServicesIfNotTram() {
        EasyMock.expect(platformMessageSource.isEnabled()).andStubReturn(true);

        int year = 2020;
        TramDate date = TramDate.of(year, 12, 23);
        Note christmasNote = new Note(ProvidesTramNotes.christmas, Note.NoteType.Christmas);

        Journey journey = createMock(Journey.class);
        Set<TransportMode> modes = new HashSet<>(Arrays.asList(Train, Bus, Walk));

        EasyMock.expect(journey.getTransportModes()).andStubReturn(modes);

        replayAll();

        List<Note> result = providesNotes.createNotesForJourney(journey,date);
        assertTrue(result.isEmpty());

        for(int offset=1; offset<11; offset++) {
            TramDate queryDate = date.plusDays(offset);
            result = providesNotes.createNotesForJourney(journey, queryDate);
            assertTrue(result.isEmpty());
        }

        date = TramDate.of(year+1, 1, 3);

        result = providesNotes.createNotesForJourney(journey, date);

        verifyAll();

        assertThat(result, not(hasItem(christmasNote)));
    }

    @Test
    void shouldNotAddMessageIfNotMessageForJourney() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        VehicleStage stageA = createStageWithBoardingPlatform("1", nearPiccGardens);

        TramTime queryTime = TramTime.of(8,11);
        TramDate date = providesLocalNow.getTramDate();
        if ((date.getDayOfWeek()==SATURDAY) || (date.getDayOfWeek()==SUNDAY)) {
            date = date.plusDays(3);
        }

        PlatformMessage info = createPlatformMessage(lastUpdate, Pomona, "<no message>");
        EasyMock.expect(platformMessageSource.messagesFor(stageA.getBoardingPlatform().getId(), date, queryTime)).
                andReturn(Optional.of(info));

        Journey journey = new Journey(queryTime.plusMinutes(5), queryTime, queryTime.plusMinutes(10),
                Collections.singletonList(stageA), Collections.emptyList(), requestedNumberChanges, journeyIndex);

        replayAll();
        List<Note> notes = providesNotes.createNotesForJourney(journey, date);
        verifyAll();

        int expected = 0;
        if (date.isChristmasPeriod()) {
            expected++;
        }
//        if (ProvidesTramNotes.summer2022Closure(serviceDate.getDate())) {
//            expected++;
//        }
        assertEquals(expected, notes.size());
    }

    @Test
    void shouldNotAddMessageIfNotMessageIfNotTimelTime() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        VehicleStage stageA = createStageWithBoardingPlatform("1", nearPiccGardens);

        TramTime queryTime = TramTime.ofHourMins(lastUpdate.toLocalTime().minusHours(4));
        TramDate serviceDate = TramDate.from(lastUpdate);

        PlatformMessage info = createPlatformMessage(lastUpdate, Pomona, "a message");
        TramDate date = TramDate.from(lastUpdate);
        EasyMock.expect(platformMessageSource.messagesFor(stageA.getBoardingPlatform().getId(), date, queryTime)).
                andReturn(Optional.of(info));

        Journey journey = new Journey(queryTime.plusMinutes(5), queryTime, queryTime.plusMinutes(10),
                Collections.singletonList(stageA), Collections.emptyList(),
                requestedNumberChanges, journeyIndex);

        replayAll();
        List<Note> notes = providesNotes.createNotesForJourney(journey, serviceDate);
        verifyAll();

        int expected = 0;
        if (serviceDate.isWeekend()) {
            expected++;
        }
        if (serviceDate.isChristmasPeriod()) {
            expected++;
        }
//        if (ProvidesTramNotes.summer2022Closure(serviceDate.getDate())) {
//            expected++;
//        }
        assertEquals(expected, notes.size());
    }

    @Test
    void shouldNotAddMessageIfNotMessageIfNotTimelyDate() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        VehicleStage stageA = createStageWithBoardingPlatform("1", nearPiccGardens);

        TramDate localDate = TramDate.from(lastUpdate).plusDays(2);
        //TramServiceDate queryDate = new TramServiceDate(localDate);
        TramTime queryTime = TramTime.ofHourMins(lastUpdate.toLocalTime());

        PlatformMessage info = createPlatformMessage(lastUpdate, Pomona, "a message");

        EasyMock.expect(platformMessageSource.messagesFor(stageA.getBoardingPlatform().getId(), localDate, queryTime))
                .andReturn(Optional.of(info));

        Journey journey = new Journey(queryTime.plusMinutes(5), queryTime, queryTime.plusMinutes(10), Collections.singletonList(stageA),
                Collections.emptyList(), requestedNumberChanges, journeyIndex);

        replayAll();
        List<Note> notes = providesNotes.createNotesForJourney(journey, localDate);
        verifyAll();

        int expected = 0;
        if (localDate.isWeekend()) {
            expected++;
        }
        if (localDate.isChristmasPeriod()) {
            expected++;
        }

        assertEquals(expected, notes.size(), notes.toString());
    }

    @Test
    void shouldAddNotesForJourneysBasedOnLiveDataIfPresent() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        VehicleStage stageABury = createStageWithBoardingPlatform("platformId1", Bury);
        VehicleStage stageBCornbrook = createStageWithBoardingPlatform("platformId2", Cornbrook);
        VehicleStage stageCNavigationRoad = createStageWithBoardingPlatform("platformId3", NavigationRoad);

        WalkingToStationStage stageDAshton = new WalkingToStationStage(nearAltrincham.location(), Ashton.fake(),
                Duration.ofMinutes(7), TramTime.of(8,11));

        VehicleStage stageEAlty = createStageWithBoardingPlatform("platformId5", Altrincham);

        TramDate date = TramDate.from(lastUpdate);
        TramTime queryTime = TramTime.ofHourMins(lastUpdate.toLocalTime());

        final String messageOne = "Some long message";
        final String messageTwo = "Some Location Long message";

        PlatformMessage infoA = createPlatformMessage(lastUpdate, Pomona, messageOne);
        PlatformMessage infoB = createPlatformMessage(lastUpdate, Altrincham, messageTwo);
        PlatformMessage infoC = createPlatformMessage(lastUpdate, Cornbrook, messageOne);
        PlatformMessage infoE = createPlatformMessage(lastUpdate, MediaCityUK, messageOne);

        EasyMock.expect(platformMessageSource.messagesFor(stageABury.getBoardingPlatform().getId(), date, queryTime)).
                andReturn(Optional.of(infoA));
        EasyMock.expect(platformMessageSource.messagesFor(stageBCornbrook.getBoardingPlatform().getId(), date, queryTime)).
                andReturn(Optional.of(infoB));
        EasyMock.expect(platformMessageSource.messagesFor(stageCNavigationRoad.getBoardingPlatform().getId(), date, queryTime)).
                andReturn(Optional.of(infoC));
        EasyMock.expect(platformMessageSource.messagesFor(stageEAlty.getBoardingPlatform().getId(), date, queryTime)).
                andReturn(Optional.of(infoE));

        final StationNote noteOne = new StationNote(Live, messageOne, createStationRefFor(Pomona));
        final StationNote noteTwo = new StationNote(Live, messageOne, createStationRefFor(Cornbrook));
        final StationNote noteThree = new StationNote(Live, messageOne, createStationRefFor(MediaCityUK));
        final StationNote noteFour = new StationNote(Live, messageTwo, createStationRefFor(Altrincham));

        EasyMock.expect(stationDTOFactory.createStationNote(Live, messageOne, Pomona.fake())).andReturn(noteOne);
        EasyMock.expect(stationDTOFactory.createStationNote(Live, messageOne, Cornbrook.fake())).andReturn(noteTwo);
        EasyMock.expect(stationDTOFactory.createStationNote(Live, messageOne, MediaCityUK.fake())).andReturn(noteThree);
        EasyMock.expect(stationDTOFactory.createStationNote(Live, messageTwo, Altrincham.fake())).andReturn(noteFour);

        List<TransportStage<?,?>> stages = Arrays.asList(stageABury, stageBCornbrook, stageCNavigationRoad, stageDAshton, stageEAlty);

        Journey journey = new Journey(queryTime.plusMinutes(5), queryTime, queryTime.plusMinutes(10), stages, Collections.emptyList(), requestedNumberChanges, journeyIndex);

        replayAll();
        List<Note> notes = providesNotes.createNotesForJourney(journey, date);
        verifyAll();

        int expected = 4;

        if (date.isWeekend()) {
            // can't change date as need live data to be available, so update expectations instead
            expected++;
        }

        if (date.isChristmasPeriod()) {
            expected++;
        }

//        if (ProvidesTramNotes.summer2022Closure(serviceDate.getDate())) {
//            expected++;
//        }

        assertEquals(expected, notes.size());
        assertTrue(notes.contains(noteOne), notes.toString());
        assertTrue(notes.contains(noteTwo), notes.toString());
        assertTrue(notes.contains(noteThree), notes.toString());
        assertTrue(notes.contains(noteFour), notes.toString());
    }

    @Test
    void shouldAddNotesForStations() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        final Station pomona = createStationFor(Pomona);
        final Station velopark = createStationFor(VeloPark);
        final Station cornbrook = createStationFor(Cornbrook);

        List<Station> stations = Arrays.asList(pomona, velopark, cornbrook);

        final StationNote firstNote = new StationNote(Live, "first message", createStationRefFor(VeloPark));
        final StationNote secondNote = new StationNote(Live, "second message", createStationRefFor(Cornbrook));
        final StationNote thirdNote = new StationNote(Live, "second message", createStationRefFor(Pomona));

        TramDate localDate = TramDate.of(2016, 10, 25);
        //TramServiceDate queryDate = new TramServiceDate(localDate);

        TramTime queryTime = TramTime.ofHourMins(lastUpdate.toLocalTime());
        EasyMock.expect(platformMessageSource.messagesFor(pomona, localDate, queryTime)).
                andReturn(Collections.singletonList(createPlatformMessage(lastUpdate, Pomona, "second message")));
        EasyMock.expect(platformMessageSource.messagesFor(velopark, localDate, queryTime)).
                andReturn(Collections.singletonList(createPlatformMessage(lastUpdate, VeloPark, "first message")));
        EasyMock.expect(platformMessageSource.messagesFor(cornbrook, localDate, queryTime)).
                andReturn(Collections.singletonList(createPlatformMessage(lastUpdate, Cornbrook, "second message")));

        EasyMock.expect(stationDTOFactory.createStationNote(Live, "second message", pomona)).andReturn(firstNote);
        EasyMock.expect(stationDTOFactory.createStationNote(Live, "first message", velopark)).andReturn(secondNote);
        EasyMock.expect(stationDTOFactory.createStationNote(Live,"second message", cornbrook)).andReturn(thirdNote);

        replayAll();
        List<Note> notes = providesNotes.createNotesForStations(stations, localDate, queryTime);
        verifyAll();

        assertEquals(3, notes.size());
        assertThat(notes.toString(), notes.contains(firstNote));
        assertThat(notes.toString(), notes.contains(secondNote));
        assertThat(notes.toString(), notes.contains(thirdNote));
    }

    private PlatformMessage createPlatformMessage(LocalDateTime lastUpdate, TramStations tramStation, String message) {

        Station station = tramStation.fakeWithPlatform("1", tramStation.getLatLong(), DataSourceID.unknown, NaptanArea.invalidId());

        Platform platform = TestEnv.findOnlyPlatform(station);
        return new PlatformMessage(platform, message, lastUpdate, station, "displayId");
    }

    private VehicleStage createStageWithBoardingPlatform(String platformNumber, KnownLocations location) {
        return createStageWithBoardingPlatform(platformNumber, location.latLong());
    }

    private VehicleStage createStageWithBoardingPlatform(String platformNumber, TramStations tramStation) {
        return createStageWithBoardingPlatform(platformNumber, tramStation.getLatLong());
    }

    private VehicleStage createStageWithBoardingPlatform(String platformNumber, LatLong latLong) {
        TramTime departTime = TramTime.of(11,22);
        Service service = MutableService.build(Service.createId("serviceId"));
        Trip trip = MutableTrip.build(Trip.createId("tripId"), "headSign", service,
                TestEnv.getTramTestRoute());

        // TODO
        List<Integer> passedStations = new ArrayList<>();

        final Station firstStation = Ashton.fakeWithPlatform(platformNumber,  latLong, DataSourceID.unknown, NaptanArea.invalidId());

        VehicleStage vehicleStage = new VehicleStage(firstStation, TestEnv.getTramTestRoute(), Tram,
                trip, departTime, createStationFor(PiccadillyGardens), passedStations);

        vehicleStage.setBoardingPlatform(TestEnv.findOnlyPlatform(firstStation));
        return vehicleStage;
    }

}
