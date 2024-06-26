package com.tramchester.unit.cloud.data;

import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.cloud.DownloadsLiveDataFromS3;
import com.tramchester.livedata.cloud.FindUniqueDueTramStatus;
import com.tramchester.livedata.domain.DTO.archived.ArchivedDepartureDTO;
import com.tramchester.livedata.domain.DTO.archived.ArchivedStationDepartureInfoDTO;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FindUniqueDueTramStatusTest extends EasyMockSupport  {


    private FindUniqueDueTramStatus finder;
    private DownloadsLiveDataFromS3 downloader;

    @BeforeEach
    void onceBeforeEachTestRuns() {

        downloader = createStrictMock(DownloadsLiveDataFromS3.class);
        finder = new FindUniqueDueTramStatus(downloader);
    }

    @Test
    void shouldExtractStatusForDateRange() {
        final LocalDateTime start = LocalDateTime.of(2020, 2, 27, 0, 1);
        final Duration duration = Duration.of(2, ChronoUnit.MINUTES);
        final Duration sample = Duration.ofMinutes(1);

        List<ArchivedDepartureDTO> dueTramsA = Arrays.asList(createDueTram(start, "tramStatusA"), createDueTram(start, "tramStatusA"),
                createDueTram(start, "tramStatusB"), createDueTram(start, "tramStatusC"));

        List<ArchivedDepartureDTO> dueTramsB = Arrays.asList(createDueTram(start, "tramStatusA"), createDueTram(start, "tramStatusA"),
                createDueTram(start, "tramStatusB"), createDueTram(start, "tramStatusD"));

        Stream<ArchivedStationDepartureInfoDTO> stream = Stream.of(createDepartureDTO(start, dueTramsA), createDepartureDTO(start, dueTramsB));
        EasyMock.expect(downloader.downloadFor(start, duration, sample)).andReturn(stream);

        replayAll();
        Set<String> results = finder.getUniqueDueTramStatus(start, duration, sample);
        verifyAll();

        assertEquals(4, results.size());
        assertTrue(results.contains("tramStatusA"));
        assertTrue(results.contains("tramStatusB"));
        assertTrue(results.contains("tramStatusC"));
        assertTrue(results.contains("tramStatusD"));
    }

    @NotNull
    private ArchivedStationDepartureInfoDTO createDepartureDTO(LocalDateTime start, List<ArchivedDepartureDTO> dueTrams) {
        return new ArchivedStationDepartureInfoDTO("lineName", "stationPlatform",
                "message", dueTrams, start, "displayId", "location");
    }

    @NotNull
    private ArchivedDepartureDTO createDueTram(LocalDateTime start, String tramStatusA) {
        return new ArchivedDepartureDTO("from", "destination", "carriages", tramStatusA,
                start, TramTime.ofHourMins(start.toLocalTime()), 1);
    }
}
