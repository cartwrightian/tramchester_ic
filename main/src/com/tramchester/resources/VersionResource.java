package com.tramchester.resources;

import com.google.inject.Inject;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.presentation.DTO.ConfigDTO;
import com.tramchester.domain.presentation.Version;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.TransportModeRepository;
import com.tramchester.repository.VersionRepository;
import io.dropwizard.jersey.caching.CacheControl;
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

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
public class VersionResource implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(VersionResource.class);

    private final TransportModeRepository repository;
    private final TramchesterConfig config;

    @Inject
    public VersionResource(TransportModeRepository repository, TramchesterConfig config) {
        logger.info("created");
        this.repository = repository;
        this.config = config;
    }

    @GET
    @Operation(description = "Return version of server code")
    @ApiResponse(content = @Content(schema = @Schema(implementation = Version.class)))
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Version version() {
        logger.info("Get version");
        return VersionRepository.getVersion();
    }

    // TODO Rename as /config
    @GET
    @Operation(description = "Config from server includes, Transport modes enabled, Postcode enabled, etc")
    @ApiResponse(content = @Content(schema = @Schema(implementation = ConfigDTO.class)))
    @Path("/modes")
    @CacheControl(maxAge = 10, maxAgeUnit = TimeUnit.SECONDS)
    public Response modes(@QueryParam("beta") String betaRaw) {
        logger.info("Get modes");

        boolean beta = betaRaw!=null;
        Set<TransportMode> modes = repository.getModes(beta);

        logger.info("Returning modes " + modes);

        final boolean postcodesEnabled = config.hasRemoteDataSourceConfig(DataSourceID.postcode);
        ConfigDTO configDTO = new ConfigDTO(modes, postcodesEnabled, config.getMaxNumResults());

        return Response.ok(configDTO).build();
    }
}
