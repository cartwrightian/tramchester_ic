package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.tramchester.RedirectToHttpsUsingELBProtoHeader;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.query.JourneyQueryDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.mappers.JourneyDTODuplicateFilter;
import com.tramchester.mappers.JourneyToDTOMapper;
import com.tramchester.repository.LocationRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@SuppressWarnings("unused")
@Path("/journey")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyPlannerResource extends UsesRecentCookie implements APIResource, GraphDatabaseDependencyMarker {
    private static final Logger logger = LoggerFactory.getLogger(JourneyPlannerResource.class);

    private final LocationJourneyPlanner locToLocPlanner;
    private final JourneyToDTOMapper journeyToDTOMapper;
    private final GraphDatabase graphDatabase;
    private final TramchesterConfig config;
    private final JourneyDTODuplicateFilter duplicateFilter;
    private final LocationRepository locationRepository;

    @Inject
    public JourneyPlannerResource(UpdateRecentJourneys updateRecentJourneys,
                                  GraphDatabase graphDatabase,
                                  ProvidesNow providesNow, LocationJourneyPlanner locToLocPlanner, JourneyToDTOMapper journeyToDTOMapper,
                                  TramchesterConfig config, JourneyDTODuplicateFilter duplicateFilter, LocationRepository locationRepository) {
        super(updateRecentJourneys, providesNow);
        this.locToLocPlanner = locToLocPlanner;
        this.journeyToDTOMapper = journeyToDTOMapper;
        this.duplicateFilter = duplicateFilter;
        this.locationRepository = locationRepository;
        this.graphDatabase = graphDatabase;
        this.config = config;
    }

    // Content-Type header in the POST request with a value of application/json
    @POST
    @Timed
    @Operation(description = "Find quickest route")
    @ApiResponse(content = @Content(schema = @Schema(implementation = JourneyPlanRepresentation.class)))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.MINUTES)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response quickestRoutePost(final JourneyQueryDTO query,
                                      @CookieParam(StationResource.TRAMCHESTER_RECENT) Cookie cookie,
                                      @HeaderParam(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO) String forwardedHeader,
                                      @Context UriInfo uriInfo)
    {
        logger.info("Got journey query " + query);

        if (query==null) {
            logger.warn("Got null query");
            return Response.serverError().build();
        }

        if (!query.valid()) {
            logger.error("Problem with received: " + query);
            return Response.serverError().build();
        }

        // if no modes provided then default to all modes currently configured
        final EnumSet<TransportMode> modes = query.getModes().isEmpty() ? config.getTransportModes() : EnumSet.copyOf(query.getModes());

        final Location<?> start = locationRepository.getLocation(query.getStartType(), query.getStartId());
        final Location<?> dest = locationRepository.getLocation(query.getDestType(), query.getDestId());

        try(final MutableGraphTransaction tx = graphDatabase.beginTxMutable() ) {

            final TramDate queryTramDate = query.getTramDate();
            final Stream<Journey> journeyStream = getJourneys(tx, queryTramDate, query.getTime(),
                    start, dest, query.isArriveBy(), query.getMaxChanges(), modes);

            Set<Journey> journeys = journeyStream.collect(Collectors.toSet());
            journeyStream.close(); // important, onCLose used to trigger removal of walk nodes etc.

            // duplicates where same path and timings, just different change points
            Set<JourneyDTO> journeyDTOS = journeys.stream().
                    map(journey -> journeyToDTOMapper.createJourneyDTO(journey,queryTramDate)).collect(Collectors.toSet());
            Set<JourneyDTO> filtered = duplicateFilter.apply(journeyDTOS);
            int diff = journeyDTOS.size()-filtered.size();
            if (diff!=0) {
                logger.info(format("Filtered out %s of %s journeys", diff, journeyDTOS.size()));
            }

            JourneyPlanRepresentation planRepresentation = new JourneyPlanRepresentation(filtered);

            if (planRepresentation.getJourneys().isEmpty()) {
                logger.warn(format("No journeys found from %s to %s at %s on %s",
                        start.getId(), dest.getId() , query.getTime(), query.getDate()));
            }

            boolean secure = isHttps(forwardedHeader);

            return buildResponse(Response.ok(planRepresentation), start, dest, cookie, uriInfo, secure);

        } catch(Exception exception) {
            logger.error("Problem processing response", exception);
            return Response.serverError().build();
        }
    }

    @POST
    @Timed
    @Path("/streamed")
    @Operation(description = "Find quickest route")
    @ApiResponse(content = @Content(schema = @Schema(implementation = JourneyDTO.class)))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.MINUTES)
    @Produces(MediaType.APPLICATION_JSON)
    public Response quickestRouteStream(JourneyQueryDTO query,
                                        @CookieParam(StationResource.TRAMCHESTER_RECENT) Cookie cookie,
                                        @HeaderParam(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO) String forwardedHeader,
                                        @Context UriInfo uriInfo) {
        logger.info("Got journey query " + query);

        if (query==null) {
            logger.warn("Got null query");
            return Response.serverError().build();
        }

        if (!query.valid()) {
            logger.error("Problem with received: " + query);
            return Response.serverError().build();
        }
        EnumSet<TransportMode> modes = query.getModes().isEmpty() ? config.getTransportModes() : EnumSet.copyOf(query.getModes());

        Location<?> start = locationRepository.getLocation(query.getStartType(), query.getStartId());
        Location<?> dest = locationRepository.getLocation(query.getDestType(), query.getDestId());

        MutableGraphTransaction tx =  graphDatabase.beginTxMutable();

        try {
            TramDate date = query.getTramDate();
            Stream<JourneyDTO> dtoStream = getJourneys(tx, date, query.getTime(), start, dest,
                    query.isArriveBy(), query.getMaxChanges(), modes).
                    map(journey -> journeyToDTOMapper.createJourneyDTO(journey, date));

            JsonStreamingOutput<JourneyDTO> jsonStreamingOutput = new JsonStreamingOutput<>(tx, dtoStream);

            // stream is closed in JsonStreamingOutput

            boolean secure = isHttps(forwardedHeader);
            return buildResponse(Response.ok(jsonStreamingOutput), start, dest, cookie, uriInfo, secure);

        } catch(Exception exception) {
            logger.error("Problem processing response", exception);
            return Response.serverError().build();
        }
    }

    private Response buildResponse(Response.ResponseBuilder responseBuilder, Location<?> start, Location<?> dest, Cookie cookie,
                                   UriInfo uriInfo, boolean secure) throws JsonProcessingException {
        URI baseUri = uriInfo.getBaseUri();
        responseBuilder.cookie(createRecentCookie(cookie, start, dest, secure, baseUri));
        return responseBuilder.build();
    }

    private Stream<Journey> getJourneys(MutableGraphTransaction tx, TramDate date, LocalTime time, Location<?> start,
                                        Location<?> dest, boolean arriveBy, int maxChanges, EnumSet<TransportMode> modes) {

        TramTime queryTime = TramTime.ofHourMins(time);
        final Duration maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());

        JourneyRequest journeyRequest = new JourneyRequest(date, queryTime, arriveBy, maxChanges,
                maxJourneyDuration,  config.getMaxNumResults(), modes);

        logger.info(format("Plan journey from %s to %s on %s", start.getId(), dest.getId(), journeyRequest));

        return locToLocPlanner.quickestRouteForLocation(tx, start, dest, journeyRequest).
                filter(journey -> !journey.getStages().isEmpty());
        //.map(journey -> journeyToDTOMapper.createJourneyDTO(journey, journeyRequest.getDate()));

    }


}
