package com.tramchester.unit.domain.presentation;


import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.ChangeStationRefWithPosition;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.SimpleStageDTO;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.mappers.MapJourneyDTOToStations;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.NavigationRoad;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapJourneyDTOToStationsTest extends EasyMockSupport {

    StationRepository stationRepository;
    private MapJourneyDTOToStations mapper;
    private TramDate when;

    @BeforeEach
    void onceBeforeAllTestsRun() {
        when = TestEnv.testDay();
        stationRepository = createMock(StationRepository.class);
        mapper = new MapJourneyDTOToStations(stationRepository);
    }

    @Test
    void shouldExtractDeparturePointForStations() {
        Station start = Altrincham.fake();
        Station dest = NavigationRoad.fake();

        SimpleStageDTO stage = createMock(SimpleStageDTO.class);

        JourneyDTO journeyDTO = createJourneyFor(0, start, dest, stage);

        EasyMock.expect(stationRepository.getStationById(IdForDTO.createFor(start))).andStubReturn(start);

        replayAll();
        Set<Station> results = mapper.getDepartureStations(Collections.singletonList(journeyDTO));
        verifyAll();

        assertEquals(1, results.size());
        assert(results.contains(start));
    }

    @Test
    void shouldExtractDeparturePointForLocations() {
        MyLocation start = KnownLocations.betweenAltrinchamAndNavigationRoad.location();
        Station dest = NavigationRoad.fake();

        SimpleStageDTO stage = createMock(SimpleStageDTO.class);

        JourneyDTO journeyDTO = createJourneyFor(0, start, dest, stage);

        EasyMock.expect(stationRepository.getStationById(IdForDTO.createFor(dest))).andStubReturn(dest);

        replayAll();
        Set<Station> results = mapper.getDepartureStations(Collections.singletonList(journeyDTO));
        verifyAll();

        assertEquals(1, results.size());
        assert(results.contains(dest));
    }

    private JourneyDTO createJourneyFor(int journeyIndex, Location<?> start, Location<?> dest, SimpleStageDTO stage) {

        TramTime queryTime = TramTime.of(9,45);
        TramTime departTime = queryTime.plusMinutes(4);

        int duration = 10;

        LocationRefWithPosition begin = new LocationRefWithPosition(start);
        LocationRefWithPosition end = new LocationRefWithPosition(dest);

        List<ChangeStationRefWithPosition> changeStations = Collections.emptyList();
        List<LocationRefWithPosition> path = Arrays.asList(begin, end);

        List<SimpleStageDTO> stages = Arrays.asList(stage);

        LocalDateTime firstDepartureTime = departTime.toDate(when);
        LocalDateTime expectedArrivalTime = departTime.plusMinutes(duration).toDate(when);

        return new JourneyDTO(begin, end, stages,
                expectedArrivalTime, firstDepartureTime,
                changeStations, queryTime,
                path, when.toLocalDate(), journeyIndex);
    }


}
