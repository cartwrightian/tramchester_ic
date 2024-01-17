package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.presentation.DTO.factory.LocationDTOFactory;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.mappers.RecentJourneysToStations;
import com.tramchester.repository.DataSourceRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationRepositoryPublic;
import com.tramchester.repository.TransportModeRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@SuppressWarnings("unused")
@Path("/locations")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyLocationsResource extends UsesRecentCookie implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(JourneyLocationsResource.class);

    private final StationRepositoryPublic stationRepository;
    private final DataSourceRepository dataSourceRepository;
    private final StationLocations stationLocations;
    private final DTOFactory DTOFactory;
    private final LocationDTOFactory locationDTOFactory;
    private final TramchesterConfig config;
    private final TransportModeRepository transportModeRepository;
    private final RecentJourneysToStations recentJourneysToStations;

    @Inject
    public JourneyLocationsResource(StationRepository stationRepository,
                                    UpdateRecentJourneys updateRecentJourneys,
                                    ProvidesNow providesNow,
                                    DataSourceRepository dataSourceRepository, StationLocations stationLocations,
                                    DTOFactory DTOFactory,
                                    LocationDTOFactory locationDTOFactory, TramchesterConfig config, TransportModeRepository transportModeRepository, RecentJourneysToStations recentJourneysToStations) {
        super(updateRecentJourneys, providesNow);
        this.DTOFactory = DTOFactory;
        this.locationDTOFactory = locationDTOFactory;
        this.transportModeRepository = transportModeRepository;
        this.recentJourneysToStations = recentJourneysToStations;
        logger.info("created");
        this.stationRepository = stationRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.stationLocations = stationLocations;
        this.config = config;
    }

    @GET
    @Timed
    @Path("/mode/{mode}")
    @Operation(description = "Get all locations for transport mode")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = LocationRefDTO.class))))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS, isPrivate = false)
    public Response getByMode(@PathParam("mode") String rawMode, @Context Request request) {
        logger.info("Get stations for transport mode: " + rawMode);

        try {
            final TransportMode mode = TransportMode.valueOf(rawMode);

            final LocalDateTime modTime = dataSourceRepository.getNewestModTimeFor(mode);
            final Date date = Date.from(modTime.toInstant(ZoneOffset.UTC));

            Response.ResponseBuilder builder = request.evaluatePreconditions(date);

            if (builder==null) {
                final Set<Station> matching = stationRepository.getStationsServing(mode);
                final List<LocationRefDTO> results = toStationRefDTOList(matching);
                if (results.isEmpty()) {
                    logger.warn("No stations found for " + mode.name());
                } else {
                    logger.info("Returning " + results.size() + " stations for mode " + mode);
                }
                return Response.ok(results).lastModified(date).build();
            } else {
                logger.info("Returning Not Modified for stations mode " + mode);
                return builder.build();
            }
        }
        catch(IllegalArgumentException missing) {
            logger.warn("Unable to match transport mode string " + rawMode, missing);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Timed
    @Path("/near/{mode}")
    @Operation(description = "Get locations close to a given lat/lon")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = LocationRefDTO.class))))
    @CacheControl(noCache = true)
    public Response getNearWithMode(@PathParam("mode") String rawMode, @QueryParam("lat") double lat, @QueryParam("lon") double lon) {
        MarginInMeters margin = MarginInMeters.of(config.getNearestStopRangeKM());
        logger.info(format("Get stations with %s of %s,%s and mode %s", margin, lat, lon, rawMode));

        TransportMode mode = TransportMode.valueOf(rawMode);

        EnumSet<TransportMode> modes = EnumSet.of(mode);

        LatLong latLong = new LatLong(lat,lon);

        MyLocation location = new MyLocation(latLong);

        List<Station> nearestStations = stationLocations.nearestStationsSorted(location,
                config.getNumOfNearestStopsToOffer(), margin, modes);

        List<LocationRefDTO> results = toStationRefDTOList(nearestStations);

        return Response.ok(results).build();
    }

    @GET
    @Timed
    @Path("/recent")
    @Operation(description = "Get recent stations based on supplied cookie")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = LocationRefDTO.class))))
    @CacheControl(noCache = true)
    public Response getRecent(@CookieParam(TRAMCHESTER_RECENT) Cookie cookie, @QueryParam("modes") String rawModes) {
        logger.info(format("Get recent stations for cookie %s", cookie));

        final EnumSet<TransportMode> modes;
        if (rawModes!=null) {
            modes = TransportMode.parseCSV(rawModes);
        } else {
            modes = transportModeRepository.getModes();
        }

        RecentJourneys recentJourneys = recentFromCookie(cookie);

        Set<Station> recent = recentJourneysToStations.from(recentJourneys, modes);

        List<LocationRefDTO> results = toStationRefDTOList(recent);

        return Response.ok(results).build();
    }

    @NotNull
    private List<LocationRefDTO> toStationRefDTOList(Collection<Station> stations) {
        return stations.stream().
                map(DTOFactory::createLocationRefDTO).
                // sort server side is here as an optimisation for front end sorting time
                sorted(Comparator.comparing(dto -> dto.getName().toLowerCase())).
                collect(Collectors.toList());
    }


}
