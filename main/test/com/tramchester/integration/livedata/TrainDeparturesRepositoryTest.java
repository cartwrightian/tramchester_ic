package com.tramchester.integration.livedata;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.DestinationAndCallingPoints;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.openLdb.TrainDeparturesRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.conditional.RequiresNetwork;
import com.tramchester.testSupport.testTags.GMTest;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@RequiresNetwork
@GMTest
@TrainTest
public class TrainDeparturesRepositoryTest {

    private static GuiceContainerDependencies componentContainer;
    private TrainDeparturesRepository trainDeparturesRepository;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new RailAndTramGreaterManchesterConfig(),
                TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        stationRepository = componentContainer.get(StationRepository.class);
        trainDeparturesRepository = componentContainer.get(TrainDeparturesRepository.class);
    }

    @Test
    void shouldGetAllDeparturesForManchester() {

        // If only load stations within geo bounds then destinations for departures can be missing since they
        // can be outside of that area. This is a test for that, can all destinations be found.
        // TODO add test for known outside of area station as well?

        Station station = RailStationIds.ManchesterPiccadilly.from(stationRepository);

        DestinationAndCallingPoints destinationAndCallingPoints = DestinationAndCallingPoints.None();
        List<UpcomingDeparture> departures = trainDeparturesRepository.forStation(station, destinationAndCallingPoints);

        assertFalse(departures.isEmpty());

        departures.forEach(departure -> {
            assertEquals(station, departure.getDisplayLocation());
            assertTrue(station.getId().isValid(), "did not find displaylocaiton " + station.getId());

            IdFor<Station> dest = departure.getDestinationId();
            assertNotNull(dest);
            assertTrue(dest.isValid());
        });
    }

    @Test
    void shouldGetSpecificDeparturesForManchester() {
        Station station = RailStationIds.ManchesterPiccadilly.from(stationRepository);

        DestinationAndCallingPoints destinationAndCallingPoints = new DestinationAndCallingPoints(RailStationIds.StokeOnTrent.getId(),
                IdSet.singleton(RailStationIds.Stockport.getId()));

        List<UpcomingDeparture> departures = trainDeparturesRepository.forStation(station, destinationAndCallingPoints);

        assertFalse(departures.isEmpty());

        departures.forEach(departure -> {
            assertEquals(station, departure.getDisplayLocation());
            assertTrue(station.getId().isValid(), "did not find displaylocaiton " + station.getId());

            IdFor<Station> dest = departure.getDestinationId();
            assertNotNull(dest);
            assertTrue(dest.isValid());

            assertTrue(departure.hasCallingPoints(), "No calling points for " + departure);

            ImmutableIdSet<Station> callings = departure.getCallingPoints();

            assertFalse(callings.isEmpty(), "No calling points for " + departure);

            assertTrue(callings.contains(RailStationIds.StokeOnTrent.getId()) ||
                    callings.contains(RailStationIds.Stockport.getId()), "Did find expected in " + callings);
        });
    }
}
