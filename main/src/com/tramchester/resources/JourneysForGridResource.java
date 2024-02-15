package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.collections.RequestStopStream;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.DTO.BoxWithCostDTO;
import com.tramchester.domain.presentation.DTO.query.GridQueryDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.FastestRoutesForBoxes;
import com.tramchester.mappers.JourneyToDTOMapper;
import com.tramchester.repository.LocationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.server.ChunkedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static java.lang.String.format;

@Path("/grid")
@Produces(MediaType.APPLICATION_JSON)
public class JourneysForGridResource implements APIResource, GraphDatabaseDependencyMarker {
    private static final Logger logger = LoggerFactory.getLogger(JourneysForGridResource.class);

    private final FastestRoutesForBoxes search;
    private final JourneyToDTOMapper dtoMapper;
    private final TramchesterConfig config;
    private final LocationRepository locationRepository;

    @Inject
    public JourneysForGridResource(FastestRoutesForBoxes search, JourneyToDTOMapper dtoMapper,
                                   TramchesterConfig config, LocationRepository locationRepository) {
        this.config = config;
        this.locationRepository = locationRepository;
        this.search = search;
        this.dtoMapper = dtoMapper;
    }


    // TODO Cache lifetime could potentially be quite long here, but makes testing harder.....
    @Path("/chunked")
    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Find cheapest travel costs for a grid of locations")
    @ApiResponse(content = @Content(schema = @Schema(implementation = BoundingBoxWithCost.class)))
    //@CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public ChunkedOutput<BoxWithCostDTO> gridCostsChunked(GridQueryDTO gridQueryDTO) {
        logger.info(format("Query for grid times for %s", gridQueryDTO));

        final TramTime departureTime = gridQueryDTO.getDepartureTramTime();
        final TramDate date = gridQueryDTO.getTramDate();

        // just find the first one -- todo this won't be lowest cost route....
        long maxNumberOfJourneys = 1;

        final Duration maxDuration = Duration.ofMinutes(gridQueryDTO.getMaxDuration());

        // todo into parameters
        final EnumSet<TransportMode> allModes = config.getTransportModes();

        final JourneyRequest journeyRequest = new JourneyRequest(date, departureTime,
                false, gridQueryDTO.getMaxChanges(), maxDuration, maxNumberOfJourneys, allModes);
        journeyRequest.setWarnIfNoResults(false);

        final Location<?> destination = locationRepository.getLocation(gridQueryDTO.getDestType(), gridQueryDTO.getDestId());

        logger.info("Create search for " + destination.getId());

        // todo single newline should be line
        final ChunkedOutput<BoxWithCostDTO> output = new ChunkedOutput<>(BoxWithCostDTO.class, "\n\n");

        final RequestStopStream<BoxWithCostDTO> result = search.findForGrid(destination, gridQueryDTO.getGridSize(), journeyRequest).
                map(box -> BoxWithCostDTO.createFrom(dtoMapper, date, box));

        Stream<BoxWithCostDTO> dtoStream = result.getStream();

        // todo need way to stop calculation gracefully if socket is closed

        ExecutorService service = Executors.newFixedThreadPool(1);

        Runnable worker = () -> {
            try {
                logger.info("Sending response for request " + gridQueryDTO);
                dtoStream.forEach(dto -> sendTo(output, dto, result));
            } finally {
                try {
                    output.close();
                    logger.info("Finished sending response for " + gridQueryDTO);
                } catch (IOException e) {
                    logger.error("unable to close output", e);
                    dtoStream.close();
                }
            }
        };

        service.submit(worker);

        return output;
    }

    private synchronized void sendTo(ChunkedOutput<BoxWithCostDTO> output, BoxWithCostDTO dto,
                                     RequestStopStream<BoxWithCostDTO> requestStopStream) {
        if (output.isClosed()) {
            logger.error("Output is closed");
            return;
        }

        try {
            output.write(dto);
            if (logger.isDebugEnabled()) {
                logger.debug("send " + dto);
            }

        } catch (IOException exception) {
            logger.error("Could not send " + dto, exception);
            requestStopStream.stop();
        }
    }

}
