package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.tramchester.dataimport.postcodes.PostcodeDataImporter;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.repository.postcodes.PostcodeRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Path("/postcodes")
@Produces(MediaType.APPLICATION_JSON)
public class PostcodeResource implements APIResource, ExperimentalAPIMarker {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeResource.class);

    private final PostcodeRepository postcodeRepository;
    private final PostcodeDataImporter importer;

    @Inject
    public PostcodeResource(PostcodeRepository postcodeRepository, PostcodeDataImporter importer) {
        logger.info("created");
        this.postcodeRepository = postcodeRepository;
        this.importer = importer;
    }

    @GET
    @Timed
    @Operation(description = "Return all loaded (local) postcodes")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = LocationDTO.class))))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS, isPrivate = false)
    public Response get(@Context Request request) {
        logger.info("Get all postcodes");

        try {
            final ZonedDateTime modTimeFromPostcodeImporter = importer.getTargetFolderModTime();
            final ZonedDateTime modTimeUTC = modTimeFromPostcodeImporter.withZoneSameLocal(ZoneOffset.UTC);

            Date modtime = Date.from(modTimeUTC.toInstant());

            logger.info(String.format("Mod time:%s UTC: %s Date: %s", modTimeFromPostcodeImporter, modTimeUTC, modtime));

            Response.ResponseBuilder builder = request.evaluatePreconditions(modtime);

            if (builder == null) {
                logger.debug("modified");
                Collection<PostcodeLocation> allPostcodes = postcodeRepository.getPostcodes();
                List<LocationDTO> postcodeDTOs = mapToDTO(allPostcodes);
                return Response.ok(postcodeDTOs).lastModified(modtime).build();
            } else {
                logger.debug("Not modified");
                return builder.build();
            }
        }
        catch (Exception exception) {
            logger.error("Caught exception", exception);
            return  Response.serverError().build();
        }
    }

    private List<LocationDTO> mapToDTO(final Collection<PostcodeLocation> allPostcodes) {
        final List<PlatformDTO> platform = Collections.emptyList();
        final List<RouteRefDTO> routes = Collections.emptyList();
        return allPostcodes.stream().
                map(postcodeLocation -> new LocationDTO(postcodeLocation, platform, routes)).
                collect(Collectors.toList());
    }
}
