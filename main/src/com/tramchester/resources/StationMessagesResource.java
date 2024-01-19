package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StationMessagesDTO;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.repository.ProvidesNotes;
import com.tramchester.repository.StationRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Path("/stationMessages")
@Produces(MediaType.APPLICATION_JSON)
public class StationMessagesResource extends TransportResource implements APIResource  {
    private static final Logger logger = LoggerFactory.getLogger(StationMessagesResource.class);

    private final StationRepository stationRepository;
    private final ProvidesNotes providesNotes;

    @Inject
    public StationMessagesResource(ProvidesNow providesNow, StationRepository stationRepository, ProvidesNotes providesNotes) {
        super(providesNow);
        this.stationRepository = stationRepository;
        this.providesNotes = providesNotes;
    }

    @GET
    @Timed
    @Path("/{id}")
    @Operation(description = "Get station by id")
    @ApiResponse(content = @Content(schema = @Schema(implementation = StationMessagesDTO.class)))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.MINUTES)
    public Response get(@PathParam("id") String text) {
        logger.info("Get station by id: " + text);

        IdFor<Station> id = Station.createId(text);
        guardForStationNotExisting(stationRepository, id);

        Station station = stationRepository.getStationById(id);

        TramDate date = providesNow.getTramDate();
        TramTime time = providesNow.getNowHourMins();
        List<Note> notes = providesNotes.createNotesForStations(Collections.singleton(station), date, time);

        logger.info(format("Found %s notes for %s", notes.size(), id));

        StationMessagesDTO dto = new StationMessagesDTO(notes);
        return Response.ok(dto).build();
    }
}
