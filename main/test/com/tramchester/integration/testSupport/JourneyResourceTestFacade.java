package com.tramchester.integration.testSupport;

import com.tramchester.App;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.JourneyQueryDTO;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.ParseJSONStream;
import com.tramchester.testSupport.reference.FakeStation;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class JourneyResourceTestFacade {

    private final IntegrationAppExtension appExtension;
    private final ParseJSONStream<JourneyDTO> parseStream;
    private final StationRepository stationRepository;

    public JourneyResourceTestFacade(IntegrationAppExtension appExtension) {
        this.appExtension = appExtension;
        App app =  appExtension.getApplication();
        stationRepository = app.getDependencies().get(StationRepository.class);
        parseStream = new ParseJSONStream<>(JourneyDTO.class);
    }

    @NotNull
    public JourneyQueryDTO getQueryDTO(TramDate date, TramTime time, Location<?> start, FakeStation dest, boolean arriveBy, int maxChanges) {
        return JourneyQueryDTO.create(date, time, start, dest.from(stationRepository), arriveBy, maxChanges);
    }

    @NotNull
    public JourneyQueryDTO getQueryDTO(TramDate date, TramTime time, Location<?> start,  Location<?> dest, boolean arriveBy, int maxChanges) {
        return JourneyQueryDTO.create(date, time, start, dest, arriveBy, maxChanges);
    }

    @NotNull
    public JourneyQueryDTO getQueryDTO(TramDate date, TramTime time, FakeStation start, Location<?> dest, boolean arriveBy, int maxChanges) {
        return JourneyQueryDTO.create(date, time, start.from(stationRepository), dest, arriveBy, maxChanges);
    }

    @NotNull
    public JourneyQueryDTO getQueryDTO(TramDate date, TramTime queryTime, FakeStation start, FakeStation dest, boolean arriveBy, int maxChanges) {
        return JourneyQueryDTO.create(date, queryTime, start.from(stationRepository), dest.from(stationRepository),
                arriveBy, maxChanges);
    }

    public List<JourneyDTO> getJourneyPlanStreamed(TramDate queryDate, TramTime time, FakeStation start,
                                                   FakeStation dest, boolean arriveBy, int maxChanges) throws IOException {

        JourneyQueryDTO query = JourneyQueryDTO.create(queryDate, time, start.from(stationRepository), dest.from(stationRepository), arriveBy, maxChanges);

        Response response = getResponse(true, Collections.emptyList(), query);

        Assertions.assertEquals(200, response.getStatus());

        InputStream inputStream = response.readEntity(InputStream.class);

        return parseStream.receive(response, inputStream);
    }

    public JourneyPlanRepresentation getJourneyPlan(JourneyQueryDTO query) {
        Response response = getResponse(false, Collections.emptyList(), query);

        Assertions.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    public Response getResponse(boolean streamed, List<Cookie> cookies, JourneyQueryDTO query) {
        String prefix = streamed ? "journey/streamed" : "journey";
        return APIClient.postAPIRequest(appExtension, prefix, query, cookies);
    }


}
