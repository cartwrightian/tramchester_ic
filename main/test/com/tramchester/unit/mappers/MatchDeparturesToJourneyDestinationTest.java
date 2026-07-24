package com.tramchester.unit.mappers;

import com.tramchester.domain.DestinationAndCallingPoints;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.mappers.MatchDeparturesToJourneyDestination;
import com.tramchester.mappers.StopOrderChecker;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRouteEnum;
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

public class MatchDeparturesToJourneyDestinationTest extends EasyMockSupport {

    private MatchDeparturesToJourneyDestination matchLiveTramToJourneyDestination;
    private LocalDate date;
    private TramTime time;
    private StationRepository stationRepository;
    private StopOrderChecker stopOrderChecker;
    private TramDate when;

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = createMock(StationRepository.class);
        stopOrderChecker = createMock(StopOrderChecker.class);
        matchLiveTramToJourneyDestination = new MatchDeparturesToJourneyDestination(stationRepository, stopOrderChecker);

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
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram,
                createDestAndCalling(journeyEnd.getId(), IdSet.emptySet()));

        verifyAll();

        assertTrue(matches);
    }

    private DestinationAndCallingPoints createDestAndCalling(IdFor<Station> dest, ImmutableIdSet<Station> calling) {
        return new DestinationAndCallingPoints(dest, calling);
    }

    @Test
    void shouldFindDepartureWhenJourneyDestinationMatchesTrain() {

        Station journeyBegin = Piccadilly.fake();

        Station railStation = RailStationIds.Altrincham.fake();
        UpcomingDeparture departure = createDueTrainFor(journeyBegin, railStation, IdSet.singleton(railStation.getId()));

        EasyMock.expect(stationRepository.hasStationId(railStation.getId())).andStubReturn(true);
        EasyMock.expect(stationRepository.getStationById(railStation.getId())).andStubReturn(railStation);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(departure,
                createDestAndCalling(railStation.getId(), IdSet.emptySet())
        );

        verifyAll();

        assertTrue(matches);
    }

    @Test
    void shouldFindDueTramWhenJourneyDestinationTowards() {

        KnownTramRouteEnum route = getPink(when);
        Station journeyBegin = StPetersSquare.fake(route);
        Station journeyEnd = Altrincham.fake(route);
        Station tramDest = Timperley.fake(route);

        UpcomingDeparture tram = createDueTramFor(journeyBegin, tramDest);

        EasyMock.expect(stationRepository.getStationById(tramDest.getId())).andReturn(tramDest);
        EasyMock.expect(stationRepository.getStationById(journeyEnd.getId())).andReturn(journeyEnd);

        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), journeyBegin, journeyEnd.getId(), tramDest.getId())).andReturn(false);
        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), journeyBegin, tramDest.getId(), journeyEnd.getId())).andReturn(true);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram,
                createDestAndCalling(journeyEnd.getId(), IdSet.emptySet()));

        verifyAll();

        assertTrue(matches);
    }

    @Test
    void shouldFindDueTramWhenJourneyAllRoutesCallAtDestinationAndCorrectDirection() {

        Station begin = Cornbrook.fake();
        KnownTramRouteEnum route = getNavy(when);
        Station destination = Deansgate.fake(route);

        Station tramDestination = Bury.fake(route);

        UpcomingDeparture tram = createDueTramFor(begin, tramDestination);
        EasyMock.expect(stationRepository.getStationById(tramDestination.getId())).andReturn(tramDestination);

        EasyMock.expect(stationRepository.getStationById(destination.getId())).andReturn(destination);
        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), Cornbrook.fake(), Deansgate.getId(), Bury.getId())).andReturn(true);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram,
                createDestAndCalling(destination.getId(), IdSet.emptySet()));
        verifyAll();

        assertTrue(matches);
    }


    @Test
    void shouldFindDueTramWhenChangeStationAvailableRouteChanges() {

        KnownTramRouteEnum routeA = getPink(when);
        Station begin = StPetersSquare.fake(routeA);
        Station change = Cornbrook.faker().dropOff(routeA).dropOff(getRed(when)).build();
        Station destination = TraffordCentre.fake(getRed(when));
        Station tramDestination = Altrincham.fake(routeA);

        UpcomingDeparture tram = createDueTramFor(begin, tramDestination);

        EasyMock.expect(stationRepository.getStationById(tramDestination.getId())).andStubReturn(tramDestination);
        EasyMock.expect(stationRepository.getStationById(destination.getId())).andReturn(destination);
        EasyMock.expect(stationRepository.getStationById(change.getId())).andReturn(change);

        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), begin, change.getId(), tramDestination.getId())).andReturn(true);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram,
                createDestAndCalling(destination.getId(), IdSet.singleton(change.getId())));
        verifyAll();

        assertTrue(matches);
    }

    @Test
    void shouldFindDueTramWhenChangeStationDoesNotMatchRoute() {

        KnownTramRouteEnum routeA = getPink(when);
        KnownTramRouteEnum routeB = getRed(when);

        Station begin = StPetersSquare.fake(routeA);
        Station change = Cornbrook.fake(routeB); // removed alty route so no match
        Station destination = TraffordCentre.fake(routeB);
        Station tramDestination = Altrincham.fake(routeA);

        UpcomingDeparture tram = createDueTramFor(begin, tramDestination);

        EasyMock.expect(stationRepository.getStationById(tramDestination.getId())).andStubReturn(tramDestination);

        EasyMock.expect(stationRepository.getStationById(destination.getId())).andReturn(destination);
        EasyMock.expect(stationRepository.getStationById(change.getId())).andReturn(change);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram,
                createDestAndCalling(destination.getId(), IdSet.singleton(change.getId())));
        verifyAll();

        assertFalse(matches);
    }

    @Test
    void shouldFindDueTramWhenChangeStationMatchRouteButWrongDirection() {

        KnownTramRouteEnum routeA = getPink(when);
        KnownTramRouteEnum routeB = getRed(when);

        Station begin = StPetersSquare.fake(routeA);
        Station change = Cornbrook.faker().dropOff(routeB).dropOff(routeA).build();
        Station destination = TraffordCentre.fake(routeB);
        Station tramDestination = Bury.fake(routeA);

        UpcomingDeparture tram = createDueTramFor(begin, tramDestination);

        EasyMock.expect(stationRepository.getStationById(tramDestination.getId())).andStubReturn(tramDestination);
        EasyMock.expect(stationRepository.getStationById(destination.getId())).andReturn(destination);
        EasyMock.expect(stationRepository.getStationById(change.getId())).andReturn(change);

        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), begin, change.getId(), tramDestination.getId())).andReturn(false);
        EasyMock.expect(stopOrderChecker.check(TramDate.of(date), begin, tramDestination.getId(), change.getId())).andReturn(false);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram,
                createDestAndCalling(destination.getId(), IdSet.singleton(change.getId())));
        verifyAll();

        assertFalse(matches);
    }

    @Test
    void shouldNotFindDueTramWhenRoutesDoNotOverlap() {

        Station begin = Altrincham.fake();
        Station destination = TramStations.Bury.fake(getPink(when));

        Station tramDestination = Piccadilly.fake(getBlue(when));
        UpcomingDeparture tram = createDueTramFor(begin, tramDestination);

        EasyMock.expect(stationRepository.getStationById(tramDestination.getId())).andReturn(tramDestination);
        EasyMock.expect(stationRepository.getStationById(destination.getId())).andReturn(destination);

        replayAll();
        boolean matches = matchLiveTramToJourneyDestination.matchesJourneyDestination(tram,
                createDestAndCalling(destination.getId(), IdSet.emptySet()));

        verifyAll();

        assertFalse(matches);
    }

    private @NotNull UpcomingDeparture createDueTramFor(Station begin, Station tramDestination) {
        return new UpcomingDeparture(date, begin, tramDestination, "Due",
                time.plusMinutes(1), "Single", TestEnv.MetAgency(), TransportMode.Tram);
    }

    private UpcomingDeparture createDueTrainFor(Station begin, Station trainDestination, ImmutableIdSet<Station> calling) {
        return new UpcomingDeparture(date, begin, trainDestination, "Due",
                time.plusMinutes(1), "Single", TestEnv.MetAgency(), TransportMode.Train, calling);
    }

}
