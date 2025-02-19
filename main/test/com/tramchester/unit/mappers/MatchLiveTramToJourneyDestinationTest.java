package com.tramchester.unit.mappers;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.mappers.MatchLiveTramToJourneyDestination;
import com.tramchester.mappers.StopOrderChecker;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MatchLiveTramToJourneyDestinationTest extends EasyMockSupport {

    private MatchLiveTramToJourneyDestination matchLiveTramToJourneyDestination;
    private LocalDate date;
    private TramTime time;
    private StationRepository stationRepository;
    private StopOrderChecker stopOrderChecker;
    private TramDate when;

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = createMock(StationRepository.class);
        stopOrderChecker = createMock(StopOrderChecker.class);
        matchLiveTramToJourneyDestination = new MatchLiveTramToJourneyDestination(stationRepository, stopOrderChecker);

        when = TestEnv.testDay();

        date = when.toLocalDate(); //LocalDate.of(2024, 6, 30);
        time = TramTime.of(10,45);
    }

    @Test
    void shouldFindDueTramWhenJourneyDestinationMatchesTramDest() {

        Station journeyBegin = Cornbrook.fake();
        Station journeyEnd = Altrincham.fake();

        UpcomingDeparture tram = createDueTramFor(journeyBegin, Altrincham.fake());

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram, IdSet.emptySet(),
                journeyEnd.getId());

        verifyAll();

        assertTrue(matches);
    }

    @Test
    void shouldFindDueTramWhenJourneyDestinationTowards() {

        Station journeyBegin = StPetersSquare.fake(getBuryManchesterAltrincham(when));
        Station journeyEnd = Altrincham.fake(getBuryManchesterAltrincham(when));
        Station tramDest = Timperley.fake(getBuryManchesterAltrincham(when));

        UpcomingDeparture tram = createDueTramFor(journeyBegin, tramDest);

        EasyMock.expect(stationRepository.getStationById(tramDest.getId())).andReturn(tramDest);
        EasyMock.expect(stationRepository.getStationById(journeyEnd.getId())).andReturn(journeyEnd);

        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), journeyBegin, journeyEnd.getId(), tramDest.getId())).andReturn(false);
        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), journeyBegin, tramDest.getId(), journeyEnd.getId())).andReturn(true);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram, IdSet.emptySet(), journeyEnd.getId());

        verifyAll();

        assertTrue(matches);
    }

    @Test
    void shouldFindDueTramWhenJourneyAllRoutesCallAtDestinationAndCorrectDirection() {

        Station begin = Cornbrook.fake();
        Station destination = Deansgate.fake(getBuryManchesterAltrincham(when));

        Station tramDestination = Bury.fake(getBuryManchesterAltrincham(when));


        UpcomingDeparture tram = createDueTramFor(begin, tramDestination);
        EasyMock.expect(stationRepository.getStationById(tramDestination.getId())).andReturn(tramDestination);


        EasyMock.expect(stationRepository.getStationById(destination.getId())).andReturn(destination);
        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), Cornbrook.fake(), Deansgate.getId(), Bury.getId())).andReturn(true);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram, IdSet.emptySet(), destination.getId());
        verifyAll();

        assertTrue(matches);
    }

    @Test
    void shouldFindDueTramWhenChangeStationAvailableRouteChanges() {

        Station begin = StPetersSquare.fake(getBuryManchesterAltrincham(when));
        Station change = Cornbrook.faker().dropOff(getBuryManchesterAltrincham(when)).dropOff(getCornbrookTheTraffordCentre(when)).build();
        Station destination = TraffordCentre.fake(getCornbrookTheTraffordCentre(when));
        Station tramDestination = Altrincham.fake(getBuryManchesterAltrincham(when));

        UpcomingDeparture tram = createDueTramFor(begin, tramDestination);

        EasyMock.expect(stationRepository.getStationById(tramDestination.getId())).andStubReturn(tramDestination);
        EasyMock.expect(stationRepository.getStationById(destination.getId())).andReturn(destination);
        EasyMock.expect(stationRepository.getStationById(change.getId())).andReturn(change);

        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), begin, change.getId(), tramDestination.getId())).andReturn(true);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram, IdSet.singleton(change.getId()), destination.getId());
        verifyAll();

        assertTrue(matches);
    }

    @Test
    void shouldFindDueTramWhenChangeStationDoesNotMatchRoute() {

        Station begin = StPetersSquare.fake(getBuryManchesterAltrincham(when));
        Station change = Cornbrook.fake(getCornbrookTheTraffordCentre(when)); // removed alty route so no match
        Station destination = TraffordCentre.fake(getCornbrookTheTraffordCentre(when));
        Station tramDestination = Altrincham.fake(getBuryManchesterAltrincham(when));

        UpcomingDeparture tram = createDueTramFor(begin, tramDestination);

        EasyMock.expect(stationRepository.getStationById(tramDestination.getId())).andStubReturn(tramDestination);

        EasyMock.expect(stationRepository.getStationById(destination.getId())).andReturn(destination);
        EasyMock.expect(stationRepository.getStationById(change.getId())).andReturn(change);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram, IdSet.singleton(change.getId()), destination.getId());
        verifyAll();

        assertFalse(matches);
    }

    @Test
    void shouldFindDueTramWhenChangeStationMatchRouteButWrongDirection() {

        Station begin = StPetersSquare.fake(getBuryManchesterAltrincham(when));
        Station change = Cornbrook.faker().dropOff(getCornbrookTheTraffordCentre(when)).dropOff(getBuryManchesterAltrincham(when)).build();
        Station destination = TraffordCentre.fake(getCornbrookTheTraffordCentre(when));
        Station tramDestination = Bury.fake(getBuryManchesterAltrincham(when));

        UpcomingDeparture tram = createDueTramFor(begin, tramDestination);

        EasyMock.expect(stationRepository.getStationById(tramDestination.getId())).andStubReturn(tramDestination);
        EasyMock.expect(stationRepository.getStationById(destination.getId())).andReturn(destination);
        EasyMock.expect(stationRepository.getStationById(change.getId())).andReturn(change);

        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), begin, change.getId(), tramDestination.getId())).andReturn(false);
        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), begin, tramDestination.getId(), change.getId())).andReturn(false);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram, IdSet.singleton(change.getId()), destination.getId());
        verifyAll();

        assertFalse(matches);
    }

    @Test
    void shouldNotFindDueTramWhenRoutesDoNotOverlap() {

        Station begin = Altrincham.fake();
        Station destination = TramStations.Bury.fake(getBuryManchesterAltrincham(when));

        Station tramDestination = Piccadilly.fake(getEtihadPiccadillyAltrincham(when));
        UpcomingDeparture tram = createDueTramFor(begin, tramDestination);

        EasyMock.expect(stationRepository.getStationById(tramDestination.getId())).andReturn(tramDestination);

        EasyMock.expect(stationRepository.getStationById(destination.getId())).andReturn(destination);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram, IdSet.emptySet(),
                destination.getId());

        verifyAll();

        assertFalse(matches);
    }

    private @NotNull UpcomingDeparture createDueTramFor(Station begin, Station tramDestination) {
        return new UpcomingDeparture(date, begin, tramDestination, "Due",
                time.plusMinutes(1), "Single", TestEnv.MetAgency(), TransportMode.Tram);
    }

}
