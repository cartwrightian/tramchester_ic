package com.tramchester.unit.domain;

import com.tramchester.domain.Platform;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.StationNote;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.repository.PlatformMessageSource;
import com.tramchester.livedata.tfgm.ProvidesTramNotes;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class ProvidesTramNotesTest extends EasyMockSupport {
    private ProvidesTramNotes providesNotes;
    private PlatformMessageSource platformMessageSource;
    private LocalDateTime lastUpdate;

    @BeforeEach
    void beforeEachTestRuns() {
        platformMessageSource = createMock(PlatformMessageSource.class);
        DTOFactory stationDTOFactory = new DTOFactory();
        ProvidesLocalNow providesLocalNow = new ProvidesLocalNow();
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
        TramTime queryTime = TramTime.of(9, 0);

        EasyMock.expect(platformMessageSource.messagesFor(StPetersSquare.fake(), queryDate, queryTime)).andReturn(Collections.emptyList());

        replayAll();
        List<Note> result = getNotesForStations(Collections.singleton(StPetersSquare.fake()), queryDate, queryTime);
        verifyAll();

        assertTrue(result.contains(new Note(ProvidesTramNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldStillAddNotesForSaturdayJourneySourceDisabled() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(false);

        TramDate queryDate = TramDate.of(2016,10,29);

        TramTime queryTime = TramTime.of(9, 0);

        replayAll();
        List<Note> result = getNotesForStations(Collections.singleton(StPetersSquare.fake()), queryDate, queryTime);
        verifyAll();

        assertTrue(result.contains(new Note(ProvidesTramNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldAddNotesForSundayJourney() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);
        TramDate queryDate = TramDate.of(2016,10,30);

        TramTime queryTime = TramTime.of(11, 22);
        EasyMock.expect(platformMessageSource.messagesFor(StPetersSquare.fake(), queryDate, queryTime)).andReturn(Collections.emptyList());

        replayAll();
        List<Note> result = getNotesForStations(Collections.singleton(StPetersSquare.fake()), queryDate, queryTime);
        verifyAll();

        assertTrue(result.contains(new Note(ProvidesTramNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldNotShowNotesOnOtherDay() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        TramDate queryDate = TramDate.of(2016,10,31);

        replayAll();
        List<Note> result = getNotesForStations(Collections.emptySet(), queryDate, TramTime.of(11,22));
        verifyAll();

        assertFalse(result.contains(new Note(ProvidesTramNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldHandleDisabled() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(false);

        TramDate queryDate = TramDate.of(2016,10,31);

        replayAll();
        List<Note> result = getNotesForStations(Collections.emptySet(), queryDate,  TramTime.of(11,22));
        verifyAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHaveNoteForChristmasTramServices() {
        EasyMock.expect(platformMessageSource.isEnabled()).andStubReturn(true);

        int year = 2023;
        TramDate beforeChristmas = TramDate.of(year, 12, 23);
        Note christmasNote = new Note(ProvidesTramNotes.christmas2023, Note.NoteType.Christmas);

        TramTime queryTime = TramTime.of(13,42);
        TramDate afterChristmas = TramDate.of(year+1, 1, 2);

        Set<Station> stations = Collections.singleton(StPetersSquare.fake());

        for(int offset=0; offset<10; offset++) {
            TramDate queryDate = beforeChristmas.plusDays(offset);
            EasyMock.expect(platformMessageSource.messagesFor(StPetersSquare.fake(), queryDate, queryTime)).andReturn(Collections.emptyList());
        }
        EasyMock.expect(platformMessageSource.messagesFor(StPetersSquare.fake(), afterChristmas, queryTime)).andReturn(Collections.emptyList());

        replayAll();

        List<Note> result = getNotesForStations(stations, beforeChristmas, queryTime);
        assertFalse(result.contains(christmasNote));

        for(int offset=1; offset<10; offset++) {
            TramDate queryDate = beforeChristmas.plusDays(offset);
            result = getNotesForStations(stations, queryDate, queryTime);
            assertTrue(result.contains(christmasNote));
        }


        result = getNotesForStations(stations, afterChristmas, queryTime);

        verifyAll();

        assertFalse(result.contains(christmasNote));
    }

    @Test
    void shouldHaveNoNoteForChristmasServicesIfNotTram() {
        EasyMock.expect(platformMessageSource.isEnabled()).andStubReturn(true);

        int year = 2020;
        TramDate date = TramDate.of(year, 12, 23);
        Note christmasNote = new Note(ProvidesTramNotes.christmas, Note.NoteType.Christmas);

        Set<Station> stations = Collections.singleton(BusStations.KnutsfordStationStand3.fake());
        TramTime queryTime = TramTime.of(11,24);
        TramDate afterChristmas = TramDate.of(year+1, 1, 3);

        for(int offset=0; offset<11; offset++) {
            TramDate queryDate = date.plusDays(offset);
            EasyMock.expect(platformMessageSource.messagesFor(BusStations.KnutsfordStationStand3.fake(), queryDate, queryTime)).
                    andStubReturn(Collections.emptyList());
        }
        EasyMock.expect(platformMessageSource.messagesFor(BusStations.KnutsfordStationStand3.fake(), afterChristmas, queryTime)).
                andStubReturn(Collections.emptyList());

        replayAll();

        List<Note> result = getNotesForStations(stations,  date, queryTime);
        assertTrue(result.isEmpty());

        for(int offset=1; offset<11; offset++) {
            TramDate queryDate = date.plusDays(offset);
            result = getNotesForStations(stations, queryDate, queryTime);
            assertTrue(result.isEmpty());
        }

        result = getNotesForStations(stations, afterChristmas, queryTime);

        verifyAll();

        assertFalse(result.contains(christmasNote));
    }

    @Test
    void shouldNotAddMessageIfNoMessagesFoundForStation() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        TramTime queryTime = TramTime.ofHourMins(lastUpdate.toLocalTime().minusHours(4));
        TramDate serviceDate = TramDate.from(lastUpdate);

        TramDate date = TramDate.from(lastUpdate);

        EasyMock.expect(platformMessageSource.messagesFor(PiccadillyGardens.fake(), date, queryTime)).
                andReturn(Collections.emptyList());

        Set<Station> stations = Collections.singleton(PiccadillyGardens.fake());

        replayAll();
        List<Note> notes = getNotesForStations(stations, serviceDate, queryTime);
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

    private List<Note> getNotesForStations(Set<Station> stations, TramDate queryDate, TramTime queryTime) {
        return providesNotes.createNotesForStations(stations, queryDate, queryTime);
    }

    private PlatformMessage createPlatformMessage(LocalDateTime lastUpdate, TramStations tramStation, String message) {

        Station station = tramStation.fakeWithPlatform(1, TramDate.of(lastUpdate.toLocalDate()));

        Platform platform = TestEnv.findOnlyPlatform(station);
        return new PlatformMessage(platform, message, lastUpdate, station, "displayId");
    }

}
