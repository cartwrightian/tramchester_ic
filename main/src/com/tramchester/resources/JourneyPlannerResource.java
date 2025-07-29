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
import com.tramchester.graph.facade.neo4j.MutableGraphTransactionNeo4J;
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
import org.jetbrains.annotations.NotNull;
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

        final boolean diagnostics = checkForDiagnosticsEnabled(query);

        final Location<?> start = locationRepository.getLocation(query.getStartType(), query.getStartId());
        final Location<?> dest = locationRepository.getLocation(query.getDestType(), query.getDestId());

        final JourneyRequest journeyRequest = createJourneyRequest(query);

        if (diagnostics) {
            logger.info("diagnostics requested");
            journeyRequest.setDiag(true);
        }

        try(final MutableGraphTransaction tx = graphDatabase.beginTxMutable() ) {

            final TramDate queryTramDate = query.getTramDate();
            final Stream<Journey> journeyStream = getJourneyStream(tx, start, dest, journeyRequest);

            final Set<Journey> journeys = journeyStream.collect(Collectors.toSet());
            journeyStream.close(); // important, onCLose used to trigger removal of walk nodes etc.

            // duplicates where same path and timings, just different change points
            final Set<JourneyDTO> journeyDTOS = journeys.stream().
                    map(journey -> journeyToDTOMapper.createJourneyDTO(journey,queryTramDate)).
                    collect(Collectors.toSet());
            final Set<JourneyDTO> filtered = duplicateFilter.apply(journeyDTOS);
            final int diff = journeyDTOS.size()-filtered.size();
            if (diff!=0) {
                logger.info(format("Filtered out %s of %s journeys", diff, journeyDTOS.size()));
            }

            // TODO likely this will only remain for the diag API, with streamed for the production one
            final JourneyPlanRepresentation planRepresentation = new JourneyPlanRepresentation(filtered);

            if (diagnostics) {
                if (journeyRequest.hasReceivedDiagnostics()) {
                    planRepresentation.addDiag(journeyRequest.getDiagnostics());
                } else {
                    logger.error("Diagnostics requested, but not received");
                }
            }

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

    private boolean checkForDiagnosticsEnabled(final JourneyQueryDTO query) {
        final boolean diagnostics;
        if (query.getDiagnostics()) {
            if (config.inProdEnv()) {
                logger.error("Got diagnostics flags while in production, will be ignored");
                diagnostics = false;
            } else {
                diagnostics = true;
            }
        } else {
            diagnostics = false;
        }
        return diagnostics;
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

        // diagnostics not supported for streamed response, no way to send them via the stream

        if (query==null) {
            logger.warn("Got null query");
            return Response.serverError().build();
        }

        if (!query.valid()) {
            logger.error("Problem with received: " + query);
            return Response.serverError().build();
        }

        final Location<?> start = locationRepository.getLocation(query.getStartType(), query.getStartId());
        final Location<?> dest = locationRepository.getLocation(query.getDestType(), query.getDestId());

        final MutableGraphTransactionNeo4J tx =  graphDatabase.beginTxMutable();

        try {
            final TramDate date = query.getTramDate();

            final JourneyRequest journeyRequest = createJourneyRequest(query);

            final Stream<JourneyDTO> dtoStream = getJourneyStream(tx, start, dest, journeyRequest).
                    map(journey -> journeyToDTOMapper.createJourneyDTO(journey, date));

            final JsonStreamingOutput<JourneyDTO> jsonStreamingOutput = new JsonStreamingOutput<>(tx, dtoStream);

            // stream is closed in JsonStreamingOutput

            final boolean secure = isHttps(forwardedHeader);
            return buildResponse(Response.ok(jsonStreamingOutput), start, dest, cookie, uriInfo, secure);

        } catch(Exception exception) {
            logger.error("Problem processing response", exception);
            return Response.serverError().build();
        }
    }

    private JourneyRequest createJourneyRequest(final JourneyQueryDTO query) {
        // if no modes provided then default to all modes currently configured
        final EnumSet<TransportMode> modes = query.getModes().isEmpty() ? config.getTransportModes() : EnumSet.copyOf(query.getModes());
        final TramDate date = query.getTramDate();
        final LocalTime time = query.getTime();
        final boolean arriveBy = query.isArriveBy();
        final int maxChanges = query.getMaxChanges();
        final TramTime queryTime = TramTime.ofHourMins(time);
        final Duration maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());

        int maxNumberResults = (query.getMaxNumResults()==null) ? config.getMaxNumResults() : query.getMaxNumResults();

        return new JourneyRequest(date, queryTime, arriveBy, JourneyRequest.MaxNumberOfChanges.of(maxChanges),
                maxJourneyDuration,  maxNumberResults, modes);
    }

    private Response buildResponse(Response.ResponseBuilder responseBuilder, Location<?> start, Location<?> dest, Cookie cookie,
                                   UriInfo uriInfo, boolean secure) throws JsonProcessingException {
        final URI baseUri = uriInfo.getBaseUri();
        responseBuilder.cookie(createRecentCookie(cookie, start, dest, secure, baseUri));
        return responseBuilder.build();
    }

    @NotNull
    private Stream<Journey> getJourneyStream(MutableGraphTransaction tx, Location<?> start, Location<?> dest, JourneyRequest journeyRequest) {
        logger.info(format("Plan journey from %s to %s on %s", start.getId(), dest.getId(), journeyRequest));

        return locToLocPlanner.quickestRouteForLocation(tx, start, dest, journeyRequest).
                filter(journey -> !journey.getStages().isEmpty());
    }



}
