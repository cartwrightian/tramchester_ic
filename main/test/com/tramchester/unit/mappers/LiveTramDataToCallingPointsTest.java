package com.tramchester.unit.mappers;

import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.DeparturesRepository;
import com.tramchester.mappers.LiveTramDataToCallingPoints;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LiveTramDataToCallingPointsTest extends EasyMockSupport {
    private final EnumSet<TransportMode> modes = EnumSet.of(TransportMode.Tram);

    private DeparturesRepository departuresRepository;
    private LiveTramDataToCallingPoints liveTramDataToCallingPoints;
    private LocalDate date;
    private TramTime time;

    @BeforeEach
    void beforeEachTestRuns() {
        departuresRepository = createMock(DeparturesRepository.class);
        liveTramDataToCallingPoints = new LiveTramDataToCallingPoints(departuresRepository);
        date = LocalDate.of(2024, 6, 30);
        time = TramTime.of(10,45);
    }

    @Test
    void shouldFindDueTramWhenJourneyDestinationMatchesTramDest() {

        Station journeyBegin = TramStations.Cornbrook.fake();
        Station journeyEnd = TramStations.Altrincham.fake();

        UpcomingDeparture tram = createDueTramFor(journeyBegin, TramStations.Altrincham);

        List<UpcomingDeparture> trams = List.of(tram);
        EasyMock.expect(departuresRepository.getDueForLocation(journeyBegin, date, time, modes)).andReturn(trams);

        replayAll();
        List<UpcomingDeparture> results = liveTramDataToCallingPoints.nextTramFor(StationPair.of(journeyBegin, journeyEnd), date, time, modes);
        verifyAll();

        assertEquals(1, results.size());
        assertEquals(tram, results.get(0));
    }

    @Disabled("WIP")
    @Test
    void shouldFindDueTramWhenJourneyAllRoutesCallAtDestination() {

        Station begin = TramStations.Cornbrook.fake();
        Station destination = TramStations.Deansgate.fake();

        UpcomingDeparture tram = createDueTramFor(begin, TramStations.Bury);

        List<UpcomingDeparture> trams = List.of(tram);
        EasyMock.expect(departuresRepository.getDueForLocation(begin, date, time, modes)).andReturn(trams);

        replayAll();
        List<UpcomingDeparture> results = liveTramDataToCallingPoints.nextTramFor(StationPair.of(begin, destination), date, time, modes);
        verifyAll();

        assertEquals(1, results.size());
        assertEquals(tram, results.get(0));
    }

    private @NotNull UpcomingDeparture createDueTramFor(Station begin, TramStations tramDestination) {
        return new UpcomingDeparture(date, begin, tramDestination.fake(), "Due",
                time.plusMinutes(1), "Single", TestEnv.MetAgency(), TransportMode.Tram);
    }

}
