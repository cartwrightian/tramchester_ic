package com.tramchester.integration.testSupport;

import com.tramchester.ComponentContainer;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.search.TramRouteCalculator;
import com.tramchester.graph.search.diagnostics.DiagnosticsToGraphViz;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.reference.FakeStation;
import com.tramchester.testSupport.reference.KnownLocality;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteCalculatorTestFacade {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorTestFacade.class);

    private final TramRouteCalculator routeCalculator;
    private final StationRepository stationRepository;
    private final GraphTransaction txn;
    private final StationGroupsRepository stationGroupsRepository;
    private final DiagnosticsToGraphViz diagnosticsToGraphViz;

    public  RouteCalculatorTestFacade(ComponentContainer componentContainer, GraphTransaction txn) {
        this.routeCalculator = componentContainer.get(TramRouteCalculator.class);
        this.stationRepository = componentContainer.get(StationRepository.class);
        this.stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        this.txn = txn;
        this.diagnosticsToGraphViz = componentContainer.get(DiagnosticsToGraphViz.class);
    }

    public List<Journey> calculateRouteAsList(FakeStation start, FakeStation end, JourneyRequest journeyRequest) {
        return calculateRouteAsList(start.from(stationRepository), end.from(stationRepository), journeyRequest);
    }

    public List<Journey> calculateRouteAsList(IdFor<Station> startId, IdFor<Station> destId, JourneyRequest request) {
        return calculateRouteAsList(getFor(startId), getFor(destId), request);
    }

    public List<Journey> calculateRouteAsList(FakeStation start, StationLocalityGroup end, JourneyRequest journeyRequest) {
        return calculateRouteAsList(start.from(stationRepository), end, journeyRequest);
    }

    public List<Journey> calculateRouteAsList(KnownLocality begin, KnownLocality end, JourneyRequest request) {
        return calculateRouteAsList(begin.from(stationGroupsRepository), end.from(stationGroupsRepository), request);
    }

    public @NotNull List<Journey> calculateRouteAsList(final Location<?> start, final Location<?> dest, final JourneyRequest request) {
        Running running = new TimesOutRunner(Duration.ofSeconds(30));
        final Stream<Journey> stream = routeCalculator.calculateRoute(txn, start, dest, request, running);
        final List<Journey> result = stream.toList();
        stream.close();
        if (request.getDiagnosticsEnabled()) {
            if (request.hasReceivedDiagnostics()) {

                final JourneyDiagnostics diagnostics = request.getDiagnostics();

                createGraphFile(start, dest, diagnostics, request, !result.isEmpty());
            } else {
                throw new RuntimeException("Diagnostics requested, but not received");
            }
        }
        return result;
    }

    private Station getFor(IdFor<Station> id) {
        return stationRepository.getStationById(id);
    }


    private void createGraphFile(Location<?> start, Location<?> dest, final JourneyDiagnostics journeyDiagnostics, final JourneyRequest journeyRequest, boolean success) {
        final String fileName = createFilename(start, dest, journeyRequest, success);


        try {
            final StringBuilder builder = new StringBuilder();
            builder.append("digraph G {\n");
            diagnosticsToGraphViz.appendTo(builder, journeyDiagnostics);
            builder.append("}");

            final FileWriter writer = new FileWriter(fileName);
            writer.write(builder.toString());
            writer.close();

            logger.warn(format("Created file %s", fileName));
        }
        catch (IOException e) {
            logger.warn("Unable to create diagnostic graph file", e);
        }
    }

    private String createFilename(Location<?> start, Location<?> dest, final JourneyRequest journeyRequest, boolean success) {
        final String status = success ? "found" : "notfound";
        final String dateString = journeyRequest.getDate().toLocalDate().toString();
        final String changes = "maxChn" + journeyRequest.getMaxChanges().get();
        final String postfix = journeyRequest.getUid().toString();
        TramTime queryTime = journeyRequest.getOriginalTime();

        String fileName = format("%s_%s_to_%s_%s%s_at_%s_%s_%s.dot",
                status,
                start.getName(), dest.getName(),
                queryTime.getHourOfDay(), queryTime.getMinuteOfHour(),
                dateString, changes, postfix);
        fileName = fileName.replaceAll(":","");
        return fileName;
    }

    private static class TimesOutRunner implements Running {
        private final Instant creationTime;
        private final Duration timeout;
        private final AtomicBoolean timedOut;

        private TimesOutRunner(final Duration timeout) {
            this.timeout = timeout;
            this.creationTime = Instant.now();
            timedOut = new AtomicBoolean(false);
        }

        @Override
        public boolean isRunning() {
            if (timedOut.get()) {
                return false;
            }

            final Instant current = Instant.now();
            final Duration duration = Duration.between(creationTime, current);

            final boolean carryOn = duration.compareTo(timeout) < 0;
            if (!carryOn) {
                logger.warn("Timeout signaled for " + duration);
                timedOut.set(true);
            }
            return carryOn;
        }
    }
}
