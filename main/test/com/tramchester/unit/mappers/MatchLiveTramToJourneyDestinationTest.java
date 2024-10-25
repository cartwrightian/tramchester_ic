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
import com.tramchester.testSupport.reference.FakeStation;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.stream.Stream;

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

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = createMock(StationRepository.class);
        stopOrderChecker = createMock(StopOrderChecker.class);
        matchLiveTramToJourneyDestination = new MatchLiveTramToJourneyDestination(stationRepository, stopOrderChecker);
        date = LocalDate.of(2024, 6, 30);
        time = TramTime.of(10,45);
    }

    @Test
    void shouldFindDueTramWhenJourneyDestinationMatchesTramDest() {

        Station journeyBegin = Cornbrook.fake();
        Station journeyEnd = Altrincham.fake();

        UpcomingDeparture tram = createDueTramFor(journeyBegin, Altrincham.fake());

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram, IdSet.singleton(journeyEnd.getId()),
                journeyEnd.getId());

        verifyAll();

        assertTrue(matches);
    }

    @Test
    void shouldFindDueTramWhenJourneyAllRoutesCallAtDestinationAndCorrectDirection() {

        Station begin = Cornbrook.fake();
        Station destination = Deansgate.fake(BuryManchesterAltrincham);

        UpcomingDeparture tram = createDueTramFor(begin, Bury.fake(BuryManchesterAltrincham));

        EasyMock.expect(stationRepository.getStationById(destination.getId())).andReturn(destination);
        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), Cornbrook.fake(), Deansgate.fake(), Bury.fake())).andReturn(true);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram,
                IdSet.singleton(destination.getId()), destination.getId());
        verifyAll();

        assertTrue(matches);
    }

    @Disabled("WIP")
    @Test
    void shouldFilterOutTramsMatchingInitialDestinationsButInWrongDirectionForDestination() {

        // Altrincham is the final destination
        // Shudehill -> Piccadilly tram due
        // For city centre walking find Market Street, Piccadilly, Piccadilly Gardens, and Shudehill as nearest stations
        // Piccadilly matches the due tram BUT tram is going in opposite direction from final desired desintation

        Station piccadilly = Piccadilly.fake(PiccadillyVictoria, EtihadPiccadillyAltrincham);
        UpcomingDeparture dueTram = createDueTramFor(Shudehill.fake(), piccadilly);

        IdSet<Station> initialDests = Stream.of(Piccadilly, MarketStreet, PiccadillyGardens).
            map(FakeStation::getId).collect(IdSet.idCollector());

        EasyMock.expect(stationRepository.getStationById(Altrincham.getId())).andReturn(Altrincham.fake(BuryManchesterAltrincham, EtihadPiccadillyAltrincham));
        EasyMock.expect(stationRepository.getStationById(MarketStreet.getId())).andReturn(MarketStreet.fake(PiccadillyVictoria, EtihadPiccadillyAltrincham, BuryManchesterAltrincham));
        EasyMock.expect(stationRepository.getStationById(PiccadillyGardens.getId())).andReturn(PiccadillyGardens.fake(PiccadillyVictoria, EtihadPiccadillyAltrincham));
        EasyMock.expect(stationRepository.getStationById(Piccadilly.getId())).andReturn(piccadilly);

        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), Shudehill.fake(), PiccadillyGardens.fake(), Piccadilly.fake())).andReturn(true);
        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), Shudehill.fake(), MarketStreet.fake(), Piccadilly.fake())).andReturn(true);
        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), Shudehill.fake(), Piccadilly.fake(), Piccadilly.fake())).andReturn(true);

        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), Shudehill.fake(), Altrincham.fake(), piccadilly)).andStubReturn(false);
        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), Shudehill.fake(), piccadilly, Altrincham.fake())).andStubReturn(false);

//        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), Shudehill.fake(), PiccadillyGardens.fake(), Altrincham.fake())).andReturn(false);
//        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), Shudehill.fake(), MarketStreet.fake(), Altrincham.fake())).andReturn(true);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(dueTram, initialDests, Altrincham.getId());
        verifyAll();

        assertFalse(matches);
    }

    @Disabled("WIP")
    @Test
    void shouldNotDueTramWhenJourneyAllRoutesCallAtDestinationButDirectionIncorrect() {

        Station begin = StPetersSquare.fake();
        Station destination = Altrincham.fake(BuryManchesterAltrincham);

        UpcomingDeparture tram = createDueTramFor(begin, Victoria.fake(BuryManchesterAltrincham));

        EasyMock.expect(stationRepository.getStationById(destination.getId())).andReturn(destination);

//        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), StPetersSquare.fake(), Victoria.fake(), Altrincham.fake())).andReturn(false);
        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), StPetersSquare.fake(), Altrincham.fake(), Victoria.fake())).andReturn(false);
//        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), StPetersSquare.fake(), Victoria.fake(), Altrincham.fake())).andReturn(false);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram, IdSet.singleton(destination.getId()),
                destination.getId());
        verifyAll();

        assertFalse(matches);
    }

    @Test
    void shouldNotFindDueTramWhenRoutesDoNotOverlap() {

        Station begin = Altrincham.fake();
        Station destination = TramStations.Bury.fake(BuryManchesterAltrincham);

        UpcomingDeparture tram = createDueTramFor(begin, TramStations.Piccadilly.fake(EtihadPiccadillyAltrincham));

        EasyMock.expect(stationRepository.getStationById(destination.getId())).andReturn(destination);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram, IdSet.singleton(destination.getId()),
                destination.getId());

        verifyAll();

        assertFalse(matches);
    }

    private @NotNull UpcomingDeparture createDueTramFor(Station begin, Station tramDestination) {
        return new UpcomingDeparture(date, begin, tramDestination, "Due",
                time.plusMinutes(1), "Single", TestEnv.MetAgency(), TransportMode.Tram);
    }

}
