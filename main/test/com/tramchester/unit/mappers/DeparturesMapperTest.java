package com.tramchester.unit.mappers;

import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.mappers.DeparturesMapper;
import com.tramchester.mappers.MatchLiveTramToJourneyDestination;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static com.tramchester.testSupport.reference.TramStations.Bury;
import static com.tramchester.testSupport.reference.TramStations.PiccadillyGardens;
import static org.junit.jupiter.api.Assertions.*;

class DeparturesMapperTest extends EasyMockSupport {

    private DeparturesMapper mapper;
    private LocalDateTime lastUpdated;
    private Station displayLocation;
    private final Agency agency = TestEnv.MetAgency();
    private final TransportMode mode = TransportMode.Tram;
    private MatchLiveTramToJourneyDestination matchLiveTramToJourneyDestination;

    @BeforeEach
    void beforeEachTestRuns() {
        lastUpdated = TestEnv.LocalNow();
        matchLiveTramToJourneyDestination = createMock(MatchLiveTramToJourneyDestination.class);
        mapper = new DeparturesMapper(matchLiveTramToJourneyDestination);
        displayLocation = Bury.fake();
    }

    @Test
    void shouldMapToDTOCorrectly() {
        TramTime when = TramTime.of(10, 41);

        Collection<UpcomingDeparture> dueTrams = Collections.singletonList(
                new UpcomingDeparture(lastUpdated.toLocalDate(), displayLocation, PiccadillyGardens.fake(),
                "DUE", when, "single", agency, mode));

        replayAll();
        Set<DepartureDTO> results = mapper.mapToDTO(dueTrams, lastUpdated);
        verifyAll();

        List<DepartureDTO> list = new LinkedList<>(results);

        assertEquals(1, list.size());
        DepartureDTO departureDTO = list.get(0);
        assertEquals(PiccadillyGardens.getIdForDTO(), departureDTO.getDestination().getId());
        assertEquals("DUE", departureDTO.getStatus());
        assertEquals(Bury.getIdForDTO(), departureDTO.getFrom().getId());

        assertEquals(when.asLocalTime(), departureDTO.getDueTime().toLocalTime());
        assertEquals(lastUpdated.toLocalDate(), departureDTO.getDueTime().toLocalDate());

        assertEquals("single", departureDTO.getCarriages());

        assertFalse(departureDTO.getMatchesJourney());

    }

    @Test
    void shouldMapToDTOCorrectlyWhenJourneyDestPresentMatches() {
        TramTime when = TramTime.of(10, 41);

        UpcomingDeparture dueTram = new UpcomingDeparture(lastUpdated.toLocalDate(), displayLocation, PiccadillyGardens.fake(),
                "DUE", when, "single", agency, mode);
        Collection<UpcomingDeparture> dueTrams = Collections.singletonList(
                dueTram);

        IdFor<Station> finalStation = PiccadillyGardens.getId();
        IdSet<Station> journeyDestinations = IdSet.singleton(PiccadillyGardens.getId());
        EasyMock.expect(matchLiveTramToJourneyDestination.matchesJourneyDestination(dueTram, journeyDestinations, finalStation)).andReturn(true);

        replayAll();
        Set<DepartureDTO> results = mapper.mapToDTO(dueTrams, lastUpdated, journeyDestinations, finalStation);
        verifyAll();

        List<DepartureDTO> list = new LinkedList<>(results);

        assertEquals(1, list.size());
        DepartureDTO departureDTO = list.get(0);
        assertTrue(departureDTO.getMatchesJourney());
    }

    @Test
    void shouldMapToDTOCorrectlyWhenJourneyPresentDestNoMatch() {
        TramTime when = TramTime.of(10, 41);

        UpcomingDeparture dueTram = new UpcomingDeparture(lastUpdated.toLocalDate(), displayLocation, PiccadillyGardens.fake(),
                "DUE", when, "single", agency, mode);
        Collection<UpcomingDeparture> dueTrams = Collections.singletonList(dueTram);

        IdSet<Station> journeyDestinations = IdSet.singleton(Bury.getId());

        IdFor<Station> finalStation = Bury.getId();
        EasyMock.expect(matchLiveTramToJourneyDestination.matchesJourneyDestination(dueTram, journeyDestinations, finalStation)).andReturn(false);

        replayAll();
        Set<DepartureDTO> results = mapper.mapToDTO(dueTrams, lastUpdated, journeyDestinations, finalStation);
        verifyAll();

        List<DepartureDTO> list = new LinkedList<>(results);

        assertEquals(1, list.size());
        DepartureDTO departureDTO = list.get(0);
        assertFalse(departureDTO.getMatchesJourney());
    }

    @Test
    void shouldHandleCrossingMidnight() {
        TramTime when = TramTime.of(23,58).plusMinutes(9);

        Collection<UpcomingDeparture> dueTrams = Collections.singletonList(
                new UpcomingDeparture(lastUpdated.toLocalDate(), displayLocation, PiccadillyGardens.fake(),
                "DUE", when, "single", agency, mode));

        replayAll();
        Set<DepartureDTO> results = mapper.mapToDTO(dueTrams, lastUpdated);
        verifyAll();

        List<DepartureDTO> list = new LinkedList<>(results);

        assertEquals(1, list.size());
        DepartureDTO departureDTO = list.get(0);
        LocalDateTime result = departureDTO.getDueTime();

        assertEquals(LocalTime.of(0,7), result.toLocalTime());
        assertEquals(lastUpdated.plusDays(1).toLocalDate(), result.toLocalDate());
    }

}
