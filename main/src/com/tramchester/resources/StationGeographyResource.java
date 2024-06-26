package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.search.FindLinkedStations;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.repository.nptg.NPTGRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/geo")
@Produces(MediaType.APPLICATION_JSON)
public class StationGeographyResource implements APIResource, GraphDatabaseDependencyMarker, ExperimentalAPIMarker {
    private static final Logger logger = LoggerFactory.getLogger(StationGeographyResource.class);

    private final FindLinkedStations findStationLinks;
    private final NeighboursRepository neighboursRepository;
    private final StationGroupsRepository stationGroupsRepository;
    private final TramchesterConfig config;
    private final StationLocations stationLocations;
    private final NaptanRepository naptanRepository;
    private final NPTGRepository nptgRepository;
    private final DTOFactory dtoFactory;

    @Inject
    public StationGeographyResource(FindLinkedStations findStationLinks, NeighboursRepository neighboursRepository,
                                    StationGroupsRepository stationGroupsRepository, TramchesterConfig config,
                                    StationLocations stationLocations, NaptanRepository naptanRepository, NPTGRepository nptgRepository, DTOFactory dtoFactory) {
        this.naptanRepository = naptanRepository;
        this.nptgRepository = nptgRepository;
        this.dtoFactory = dtoFactory;
        this.findStationLinks = findStationLinks;
        this.neighboursRepository = neighboursRepository;
        this.stationGroupsRepository = stationGroupsRepository;
        this.config = config;
        this.stationLocations = stationLocations;
    }

    @GET
    @Timed
    @Path("/links")
    @Operation(description = "Get pairs of station links")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = StationToStationConnectionDTO.class))))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getAll() {
        logger.info("Get station links");

        final Stream<StationToStationConnection> allLinks = config.getTransportModes().
                stream().flatMap(mode -> findStationLinks.findLinkedFor(mode).stream());

        final List<StationToStationConnectionDTO> results = allLinks.
                filter(StationToStationConnection::hasValidLatlongs).
                map(dtoFactory::createStationLinkDTO).
                toList();

        return Response.ok(results).build();
    }

    @GET
    @Timed
    @Path("/neighbours")
    @Operation(description = "Get all pairs of neighbours")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = StationToStationConnectionDTO.class))))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getNeighbours() {
        logger.info("Get station neighbours");

        if (!config.hasNeighbourConfig()) {
            logger.warn("Neighbours disabled");
            return Response.ok(Collections.<StationToStationConnectionDTO>emptyList()).build();
        }

        Set<StationToStationConnection> neighbours = neighboursRepository.getAll();

        List<StationToStationConnectionDTO> results = neighbours.stream().
                filter(StationToStationConnection::hasValidLatlongs).
                map(dtoFactory::createStationLinkDTO).collect(Collectors.toList());

        return Response.ok(results).build();
    }

    @GET
    @Timed
    @Path("/areas")
    @Operation(description = "List of boundaries for each area along with the area code")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = AreaBoundaryDTO.class))))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getAreas() {
        logger.info("Get areas");

        Set<NPTGLocality> areas = nptgRepository.getAll();

        logger.info("Get boundaries for " + areas.size() + " areas");

        List<AreaBoundaryDTO> allBoundaries = areas.stream().
                filter(area -> stationLocations.hasStationsOrPlatformsIn(area.getId())).
                map(area -> new AreaBoundaryDTO(naptanRepository.getBoundaryFor(area.getId()), area))
                .collect(Collectors.toList());

        logger.info("Found " + allBoundaries.size() + " areas with boundaries");

        return Response.ok(allBoundaries).build();
    }

    @GET
    @Timed
    @Path("/stationsboundary")
    @Operation(description = "Boundary around all stations")
    @ApiResponse(content = @Content(schema = @Schema(implementation = BoundaryDTO.class)))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getBoundaryForAllStations() {
        logger.info("Get boundary for all stations");

        BoundaryDTO result = new BoundaryDTO(stationLocations.getBoundaryForStations());

        return Response.ok(result).build();
    }


    @GET
    @Timed
    @Path("/groups")
    @Operation(description = "Get all pairs of composites (parent & child)")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = StationGroupDTO.class))))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getLinks() {
        logger.info("Get composite links");

        List<StationGroupDTO> groups = new ArrayList<>();
        config.getTransportModes().forEach(mode ->
                groups.addAll(stationGroupsRepository.getStationGroupsFor(mode).stream().
                        map(dtoFactory::createStationGroupDTO).collect(Collectors.toSet())));

        return Response.ok(groups).build();
    }

    @GET
    @Timed
    @Path("/quadrants")
    @Operation(description = "Get station location quadrants")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = BoxDTO.class))))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getQuadrants() {
        logger.info("Get quadrants");

        List<BoxDTO> quadrantDTOs = stationLocations.getQuadrants().stream().
                map(BoxDTO::new).collect(Collectors.toList());

        return Response.ok(quadrantDTOs).build();
    }

    @GET
    @Timed
    @Path("/bounds")
    @Operation(description = "Get current geographical bounds in effect")
    @ApiResponse(content = @Content(schema = @Schema(implementation = BoxDTO.class)))
    public Response getBounds() {
        logger.info("Get bounds");

        final BoxDTO bounds = new BoxDTO(config.getBounds());

        return Response.ok(bounds).build();

    }


}
