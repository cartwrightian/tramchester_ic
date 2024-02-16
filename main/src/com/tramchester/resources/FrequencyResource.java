package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.tramchester.domain.BoxWithServiceFrequency;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.BoxWithFrequencyDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.StopCallsForGrid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@Path("/frequency")
@Produces(MediaType.APPLICATION_JSON)
public class FrequencyResource extends TransportResource implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(FrequencyResource.class);

    private final DTOFactory DTOFactory;
    private final StopCallsForGrid stopCallsForGrid;

    @Inject
    public FrequencyResource(StopCallsForGrid stopCallsForGrid, ProvidesNow providesNow,
                             DTOFactory DTOFactory) {
        super(providesNow);
        this.DTOFactory = DTOFactory;
        logger.info("created");
        this.stopCallsForGrid = stopCallsForGrid;
    }

    @GET
    @Timed
    @Operation(description = "Get number of services for the given time period for each grid box")
    @ApiResponse(content = @Content(schema = @Schema(implementation = BoxWithServiceFrequency.class)))
    //@CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response gridCosts(@QueryParam("gridSize") int gridSize,
                              @QueryParam("date") String dateRaw,
                              @QueryParam("startTime") String startTimeRaw,
                              @QueryParam("endTime") String endTimeRaw) {
        logger.info(format("Query for %s gridsize meters, date: '%s' start: '%s' end: '%s", gridSize,
                dateRaw, startTimeRaw, endTimeRaw));

        TramDate date = TramDate.parse(dateRaw);
        TramTime startTime = parseTime(startTimeRaw);
        TramTime endTime = parseTime(endTimeRaw);

        Stream<BoxWithServiceFrequency> results = stopCallsForGrid.getServiceFrequencies(gridSize, date, startTime, endTime);
        Stream<BoxWithFrequencyDTO> dtoStream = results.map(this::createDTO);
        JsonStreamingOutput<BoxWithFrequencyDTO> jsonStreamingOutput = new JsonStreamingOutput<>(dtoStream);

        Response.ResponseBuilder responseBuilder = Response.ok(jsonStreamingOutput);
        return responseBuilder.build();
    }

    private BoxWithFrequencyDTO createDTO(BoxWithServiceFrequency result) {
        List<LocationRefDTO> stopDTOs = result.getStationsWithStopCalls().stream().
                map(DTOFactory::createLocationRefDTO).
                collect(Collectors.toList());

        return new BoxWithFrequencyDTO(result, stopDTOs, result.getNumberOfStopcalls(), new ArrayList<>(result.getModes()));
    }

}
