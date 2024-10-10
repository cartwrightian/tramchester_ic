package com.tramchester.unit.mappers;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.JourneyDTODuplicateFilter;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JourneyDTODuplicateFilterTest {

    private TramDate queryDate;
    private TramTime queryTime;
    private JourneyDTODuplicateFilter filter;

    @BeforeEach
    void beforeEachTestRuns() {
        queryDate = TestEnv.testDay();
        queryTime = TramTime.of(9,5);
        filter = new JourneyDTODuplicateFilter();
    }

    @Test
    void shouldFilterOutIfSameTimesStopsAndChanges() {

        List<ChangeStationRefWithPosition> changeStations = Collections.singletonList(getChangeStationRef(Deansgate));
        List<LocationRefWithPosition> path = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));

        JourneyDTO journeyA = createJourneyFor(TramTime.of(9,14), 10, changeStations, path, 0);
        JourneyDTO journeyB = createJourneyFor(TramTime.of(9,14), 10, changeStations, path, 1);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(1, results.size());
    }

    @Test
    void shouldFilterOutIfSameTimesStopsAndDiffChanges() {

        List<ChangeStationRefWithPosition> changeStationsA = Collections.singletonList(getChangeStationRef(Deansgate));
        List<ChangeStationRefWithPosition> changeStationsB = Collections.singletonList(getChangeStationRef(TraffordBar));

        List<LocationRefWithPosition> path = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));

        JourneyDTO journeyA = createJourneyFor(TramTime.of(9,14), 10, changeStationsA, path, 0);
        JourneyDTO journeyB = createJourneyFor(TramTime.of(9,14), 10, changeStationsB, path, 1);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(1, results.size());
    }

    @Test
    void shouldNotFilterOutIfDiffDepartTimes() {

        List<ChangeStationRefWithPosition> changeStations = Collections.singletonList(getChangeStationRef(Deansgate));

        List<LocationRefWithPosition> path = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));

        JourneyDTO journeyA = createJourneyFor(TramTime.of(9,14), 10, changeStations, path, 0);
        JourneyDTO journeyB = createJourneyFor(TramTime.of(9,17), 10, changeStations, path, 1);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB, journeyA));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(2, results.size());
    }

    @Test
    void shouldNotFilterOutIfDiffDuration() {

        List<ChangeStationRefWithPosition> changeStations = Collections.singletonList(getChangeStationRef(Deansgate));

        List<LocationRefWithPosition> path = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));

        JourneyDTO journeyA = createJourneyFor(TramTime.of(9,14), 15, changeStations, path, 0);
        JourneyDTO journeyB = createJourneyFor(TramTime.of(9,14), 10, changeStations, path, 1);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB, journeyA));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(2, results.size());
    }

    @Test
    void shouldNotFilterOutIfSDiffPath() {

        List<ChangeStationRefWithPosition> changeStations = Collections.singletonList(getChangeStationRef(Deansgate));

        List<LocationRefWithPosition> pathA = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));
        List<LocationRefWithPosition> pathB = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(Altrincham));

        JourneyDTO journeyA = createJourneyFor(TramTime.of(9,14), 10, changeStations, pathA, 0);
        JourneyDTO journeyB = createJourneyFor(TramTime.of(9,14), 10, changeStations, pathB, 1);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB, journeyB));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(2, results.size());
    }

    private JourneyDTO createJourneyFor(TramTime departTime, int duration, List<ChangeStationRefWithPosition> changeStations,
                                        List<LocationRefWithPosition> path, int journeyIndex) {
        LocationRefWithPosition begin = getStationRef(Ashton);
        VehicleStageDTO stageA = new VehicleStageDTO();
        VehicleStageDTO stageB = new VehicleStageDTO();
        List<SimpleStageDTO> stages = Arrays.asList(stageA, stageB);

        LocalDateTime firstDepartureTime = departTime.toDate(queryDate);
        LocalDateTime expectedArrivalTime = departTime.plusMinutes(duration).toDate(queryDate);

        return new JourneyDTO(begin, getStationRef(NavigationRoad), stages,
                expectedArrivalTime, firstDepartureTime,
                changeStations, queryTime,
                path, queryDate, journeyIndex);
    }

    private LocationRefWithPosition getStationRef(TramStations tramStations) {
        return new LocationRefWithPosition(tramStations.fake());
    }

    private ChangeStationRefWithPosition getChangeStationRef(TramStations tramStations) {
        return new ChangeStationRefWithPosition(tramStations.fake(), TransportMode.Tram);
    }
}
