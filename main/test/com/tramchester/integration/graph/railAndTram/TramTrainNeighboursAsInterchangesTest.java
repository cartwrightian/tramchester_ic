package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.InterchangeType;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.repository.InterchangesTramTest;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@GMTest
public class TramTrainNeighboursAsInterchangesTest {

    private static GuiceContainerDependencies componentContainer;
    private InterchangeRepository interchangeRepository;
    private Station altrinchamTram;
    private NeighboursRepository neighboursRepository;
    private Station altrinchamTrain;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new RailAndTramGreaterManchesterConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
        neighboursRepository = componentContainer.get(NeighboursRepository.class);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        altrinchamTram = stationRepository.getStationById(Altrincham.getId());
        altrinchamTrain = stationRepository.getStationById(RailStationIds.Altrincham.getId());
    }

    /***
     * @see InterchangesTramTest#altrinchamNotAnInterchange()
     */
    @Test
    public void altrinchamBecomesInterchangeWhenNeighboursCreated() {
        assertTrue(interchangeRepository.isInterchange(altrinchamTram));
        assertTrue(interchangeRepository.isInterchange(altrinchamTrain));
    }

    @Test
    public void shouldHaveAltrinchamTramAndTrainStationsAsNeighbours() {
        assertTrue(neighboursRepository.hasNeighbours(Altrincham.getId()));
        assertTrue(neighboursRepository.hasNeighbours(RailStationIds.Altrincham.getId()));

        Set<Station> tramsNeighbours = neighboursRepository.getNeighboursFor(Altrincham.getId());
        assertTrue(tramsNeighbours.contains(altrinchamTrain));

        Set<Station> trainsNeighbours = neighboursRepository.getNeighboursFor(RailStationIds.Altrincham.getId());
        assertTrue(trainsNeighbours.contains(altrinchamTram));

    }

    @Test
    public void shouldHaveAltrinchamTramAsMultimode() {
        InterchangeStation interchange = interchangeRepository.getInterchange(altrinchamTram);
        assertEquals(InterchangeType.NeighbourLinks, interchange.getType());
        assertTrue(interchange.isMultiMode(), "not multi-mode "  + interchange);
        assertEquals(EnumSet.of(Tram, Train), interchange.getTransportModes());
    }

    @Test
    public void shouldHaveAltrinchamTrainAsMultimode() {
        InterchangeStation interchange = interchangeRepository.getInterchange(altrinchamTrain);
        assertEquals(InterchangeType.NeighbourLinks, interchange.getType());
        assertTrue(interchange.isMultiMode(), "not multi-mode "  + interchange);
        assertEquals(EnumSet.of(Tram, Train), interchange.getTransportModes());
    }

}
