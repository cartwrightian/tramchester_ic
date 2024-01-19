package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.NPTGLocality;
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
    private ProvidesLocalNow providesLocalNow;
    private final int journeyIndex = 42;

    @BeforeEach
    void beforeEachTestRuns() {
        platformMessageSource = createMock(PlatformMessageSource.class);
        DTOFactory stationDTOFactory = new DTOFactory();
        providesLocalNow = new ProvidesLocalNow();
        lastUpdate = providesLocalNow.getDateTime();

        providesNotes = new ProvidesTramNotes(platformMessageSource, stationDTOFactory);
    }

    private LocationRefDTO createStationRefFor(TramStations station) {
        return new LocationRefDTO(station.fake());
    }

    private List<LocationRefDTO> createStationRefFor(List<TramStations> stations) {
        return stations.stream().map(station -> new LocationRefDTO(station.fake())).toList();
    }

    @Test
    void shouldAddNotesForSaturdayJourney() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        TramDate queryDate = TramDate.of(2022,7,9);

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andReturn(Collections.singleton(Tram));
        EasyMock.expect(journey.getCallingPlatformIds()).andReturn(IdSet.emptySet());

        replayAll();
        List<Note> result = getNotesForJourney(journey, queryDate);
        verifyAll();

        assertThat(result, hasItem(new Note(ProvidesTramNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldAddNotesForSaturdayJourneyWhenNoJourneysFound() {
        TramDate queryDate = TramDate.of(2022,7,9);

        replayAll();
        List<Note> result = providesNotes.createNotesForJourneys(Collections.emptySet(), queryDate);
        verifyAll();

        assertThat(result, hasItem(new Note(ProvidesTramNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldAddNotesForSaturdayMultipleJourneys() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        TramDate queryDate = TramDate.of(2022,7,9);

        Journey journeyA = createMock(Journey.class);
        EasyMock.expect(journeyA.getTransportModes()).andReturn(Collections.singleton(Tram));
        EasyMock.expect(journeyA.getCallingPlatformIds()).andReturn(IdSet.emptySet());

        Journey journeyB = createMock(Journey.class);
        EasyMock.expect(journeyB.getTransportModes()).andReturn(Collections.singleton(Tram));
        EasyMock.expect(journeyB.getCallingPlatformIds()).andReturn(IdSet.emptySet());

        replayAll();
        List<Note> result = providesNotes.createNotesForJourneys(new HashSet<>(Arrays.asList(journeyA, journeyB)), queryDate);
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
        List<Note> result = getNotesForJourney(journey, queryDate);
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
        List<Note> result = getNotesForJourney(journey, queryDate);
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
        List<Note> result = getNotesForJourney(journey, queryDate);
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
        List<Note> result = getNotesForJourney(journey, queryDate);
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
        List<Note> result = getNotesForJourney(journey, queryDate);
        verifyAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHaveNoteForChristmasTramServices() {
        EasyMock.expect(platformMessageSource.isEnabled()).andStubReturn(true);

        int year = 2023;
        TramDate beforeChristmas = TramDate.of(year, 12, 23);
        Note christmasNote = new Note(ProvidesTramNotes.christmas2023, Note.NoteType.Christmas);

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andStubReturn(Collections.singleton(Tram));
        EasyMock.expect(journey.getCallingPlatformIds()).andStubReturn(IdSet.emptySet());

        replayAll();

        List<Note> result = getNotesForJourney(journey, beforeChristmas);
        assertThat(result, not(hasItem(christmasNote)));

        for(int offset=1; offset<10; offset++) {
            TramDate queryDate = beforeChristmas.plusDays(offset);
            result = getNotesForJourney(journey, queryDate);
            assertThat(queryDate.toString(), result, hasItem(christmasNote));
        }

        TramDate afterChristmas = TramDate.of(year+1, 1, 2);

        result = getNotesForJourney(journey, afterChristmas);

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

        List<Note> result = getNotesForJourney(journey, date);
        assertTrue(result.isEmpty());

        for(int offset=1; offset<11; offset++) {
            TramDate queryDate = date.plusDays(offset);
            result = getNotesForJourney(journey, queryDate);
            assertTrue(result.isEmpty());
        }

        date = TramDate.of(year+1, 1, 3);

        result = getNotesForJourney(journey, date);

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
        List<Note> notes = getNotesForJourney(journey, date);
        verifyAll();

        int expected = 0;
        if (date.isChristmasPeriod()) {
            expected++;
        }
        assertEquals(expected, notes.size());
    }

    @Test
    void shouldNotAddMessageIfNotMessageIfNotTimelyMessages() {
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
        List<Note> notes = getNotesForJourney(journey, serviceDate);
        verifyAll();

        int expected = 0;
        if (serviceDate.isWeekend()) {
            expected++;
        }
        if (serviceDate.isChristmasPeriod()) {
            expected++;
        }
        assertEquals(expected, notes.size());
    }

    @Test
    void shouldNotAddMessageIfNotMessageIfNotTimelyDate() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        VehicleStage stageA = createStageWithBoardingPlatform("1", nearPiccGardens);

        TramDate localDate = TramDate.from(lastUpdate).plusDays(2);
        TramTime queryTime = TramTime.ofHourMins(lastUpdate.toLocalTime());

        PlatformMessage info = createPlatformMessage(lastUpdate, Pomona, "a message");

        EasyMock.expect(platformMessageSource.messagesFor(stageA.getBoardingPlatform().getId(), localDate, queryTime))
                .andReturn(Optional.of(info));

        Journey journey = new Journey(queryTime.plusMinutes(5), queryTime, queryTime.plusMinutes(10), Collections.singletonList(stageA),
                Collections.emptyList(), requestedNumberChanges, journeyIndex);

        replayAll();
        List<Note> notes = getNotesForJourney(journey, localDate);
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

        final String textA = "Some long message";
        final String textB = "Some Location Long message";

        PlatformMessage infoA = createPlatformMessage(lastUpdate, Pomona, textA);
        PlatformMessage infoB = createPlatformMessage(lastUpdate, Altrincham, textB);
        PlatformMessage infoC = createPlatformMessage(lastUpdate, Cornbrook, textA);
        PlatformMessage infoE = createPlatformMessage(lastUpdate, MediaCityUK, textA);

        EasyMock.expect(platformMessageSource.messagesFor(stageABury.getBoardingPlatform().getId(), date, queryTime)).
                andStubReturn(Optional.of(infoA));
        EasyMock.expect(platformMessageSource.messagesFor(stageBCornbrook.getBoardingPlatform().getId(), date, queryTime)).
                andStubReturn(Optional.of(infoB));
        EasyMock.expect(platformMessageSource.messagesFor(stageCNavigationRoad.getBoardingPlatform().getId(), date, queryTime)).
                andStubReturn(Optional.of(infoC));
        EasyMock.expect(platformMessageSource.messagesFor(stageEAlty.getBoardingPlatform().getId(), date, queryTime)).
                andStubReturn(Optional.of(infoE));

        List<TransportStage<?,?>> stages = Arrays.asList(stageABury, stageBCornbrook, stageCNavigationRoad, stageDAshton, stageEAlty);

        Journey journeyA = new Journey(queryTime.plusMinutes(5), queryTime, queryTime.plusMinutes(10),
                stages, Collections.emptyList(), requestedNumberChanges, journeyIndex);
        Journey journeyB = new Journey(queryTime.plusMinutes(8), queryTime, queryTime.plusMinutes(12),
                stages, Collections.emptyList(), requestedNumberChanges, journeyIndex);

        replayAll();
        List<Note> notes = providesNotes.createNotesForJourneys(new HashSet<>(Arrays.asList(journeyA, journeyB)), date);
        verifyAll();

        int expected = 2;

        if (date.isWeekend()) {
            // can't change date as need live data to be available, so update expectations instead
            expected++;
        }

        if (date.isChristmasPeriod()) {
            expected++;
        }

        assertEquals(expected, notes.size());

        List<LocationRefDTO> stationRefsForA = createStationRefFor(Arrays.asList(Cornbrook, MediaCityUK, Pomona));
        List<LocationRefDTO> stationRefsForB = Collections.singletonList(createStationRefFor(Altrincham));

        Optional<Note> findA = notes.stream().filter(note -> note.getText().equals(textA)).findFirst();
        assertTrue(findA.isPresent());
        StationNote foundA = (StationNote) findA.get();
        assertEquals(stationRefsForA, foundA.getDisplayedAt());

        Optional<Note> findB = notes.stream().filter(note -> note.getText().equals(textB)).findFirst();
        assertTrue(findB.isPresent());
        StationNote foundB = (StationNote) findB.get();
        assertEquals(stationRefsForB, foundB.getDisplayedAt());

    }

    @Test
    void shouldAddNotesForStations() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        final Station pomona = Pomona.fake();
        final Station velopark = VeloPark.fake();
        final Station cornbrook = Cornbrook.fake();

        String textA = "first text";
        String textB = "second text";

        Set<Station> stations = new HashSet<>(Arrays.asList(pomona, velopark, cornbrook));

        TramDate localDate = TramDate.of(2016, 10, 25);

        TramTime queryTime = TramTime.ofHourMins(lastUpdate.toLocalTime());
        PlatformMessage firstMessage = createPlatformMessage(lastUpdate, VeloPark, textA);
        PlatformMessage secondMessage = createPlatformMessage(lastUpdate, Pomona, textB);
        PlatformMessage thirdMessage = createPlatformMessage(lastUpdate, Cornbrook, textB);

        EasyMock.expect(platformMessageSource.messagesFor(pomona, localDate, queryTime)).
                andReturn(Collections.singletonList(secondMessage));
        EasyMock.expect(platformMessageSource.messagesFor(velopark, localDate, queryTime)).
                andReturn(Collections.singletonList(firstMessage));
        EasyMock.expect(platformMessageSource.messagesFor(cornbrook, localDate, queryTime)).
                andReturn(Collections.singletonList(thirdMessage));

        List<LocationRefDTO> stationRefsForA = Collections.singletonList(createStationRefFor(VeloPark));
        List<LocationRefDTO> stationRefsForB = createStationRefFor(Arrays.asList(Cornbrook, Pomona));

        replayAll();
        List<Note> notes = providesNotes.createNotesForStations(stations, localDate, queryTime);
        verifyAll();

        assertEquals(2, notes.size());

        Optional<Note> findA = notes.stream().filter(note -> note.getText().equals(textA)).findFirst();
        assertTrue(findA.isPresent());
        StationNote foundA = (StationNote) findA.get();
        assertEquals(stationRefsForA, foundA.getDisplayedAt());

        Optional<Note> findB = notes.stream().filter(note -> note.getText().equals(textB)).findFirst();
        assertTrue(findB.isPresent());
        StationNote foundB = (StationNote) findB.get();
        assertEquals(stationRefsForB, foundB.getDisplayedAt());

    }

    private List<Note> getNotesForJourney(Journey journey, TramDate queryDate) {
        return providesNotes.createNotesForJourneys(Collections.singleton(journey), queryDate);
    }

    private PlatformMessage createPlatformMessage(LocalDateTime lastUpdate, TramStations tramStation, String message) {

        Station station = tramStation.fakeWithPlatform("1", tramStation.getLatLong(), DataSourceID.unknown, NPTGLocality.InvalidId());

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
        Service service = MutableService.build(Service.createId("serviceId"), DataSourceID.tfgm);
        Trip trip = MutableTrip.build(Trip.createId("tripId"), "headSign", service,
                TestEnv.getTramTestRoute());

        // TODO
        List<Integer> passedStations = new ArrayList<>();

        final Station firstStation = Ashton.fakeWithPlatform(platformNumber,  latLong, DataSourceID.unknown, NPTGLocality.InvalidId());

        VehicleStage vehicleStage = new VehicleStage(firstStation, TestEnv.getTramTestRoute(), Tram,
                trip, departTime, PiccadillyGardens.fake(), passedStations);

        vehicleStage.setBoardingPlatform(TestEnv.findOnlyPlatform(firstStation));
        return vehicleStage;
    }

}
