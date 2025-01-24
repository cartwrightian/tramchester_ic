package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.TreeSet;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class DepartureDTOTest {

    private LocalTime updateTime;
    private LocalDate updateDate;
    private LocalDateTime now;

    @BeforeEach
    void beforeEachTestRuns() {
        now = LocalDateTime.of(2022, 5, 30, 11, 45);

        updateTime = now.toLocalTime();
        updateDate = now.toLocalDate();
    }

    @Test
    void shouldCreateFromDueTramAndLocation() {

        UpcomingDeparture dueTram = getDueTram(updateTime, TramStations.Bury, 42);
        DepartureDTO departureDTO = new DepartureDTO(StPetersSquare.fake(), dueTram, now, true);

        assertEquals(StPetersSquare.getIdForDTO(), departureDTO.getFrom().getId());
        assertEquals(Bury.getIdForDTO(), departureDTO.getDestinationId());
        assertEquals("status", departureDTO.getStatus());
        assertEquals("carriages", departureDTO.getCarriages());
        LocalDateTime when = departureDTO.getDueTime();
        assertEquals(updateTime.plusMinutes(42).truncatedTo(ChronoUnit.MINUTES), when.toLocalTime());
        assertEquals(updateDate, when.toLocalDate());
        assertTrue(departureDTO.getMatchesJourney());
    }

    @Test
    void shouldCompareBasedOnWhenTramDue() {

        Station stPetersSquare = StPetersSquare.fake();
        DepartureDTO departureDTOA = new DepartureDTO(stPetersSquare,
                getDueTram(updateTime, TramStations.Deansgate, 5), now, false);
        DepartureDTO departureDTOB = new DepartureDTO(stPetersSquare,
                getDueTram(updateTime, TramStations.Bury, 3), now, false);
        DepartureDTO departureDTOC = new DepartureDTO(stPetersSquare,
                getDueTram(updateTime, TramStations.Piccadilly, 12), now, false);

        Set<DepartureDTO> list = new TreeSet<>();
        list.add(departureDTOA);
        list.add(departureDTOB);
        list.add(departureDTOC);

        DepartureDTO[] elements = list.toArray(new DepartureDTO[3]);
        assertEquals(Bury.getIdForDTO(), elements[0].getDestinationId());
        assertEquals(Deansgate.getIdForDTO(), elements[1].getDestinationId());
        assertEquals(Piccadilly.getIdForDTO(), elements[2].getDestinationId());
    }

    @NotNull
    private UpcomingDeparture getDueTram(LocalTime updateTime, TramStations station, int wait) {
        TramTime dueTime = TramTime.ofHourMins(updateTime.plusMinutes(wait));
        return new UpcomingDeparture(updateDate, NavigationRoad.fake(),
                station.fake(), "status", dueTime, "carriages",
                TestEnv.MetAgency(), TransportMode.Tram);
    }
}
