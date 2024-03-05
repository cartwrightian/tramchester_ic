package com.tramchester.integration.testSupport;

import com.tramchester.ComponentContainer;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.search.RouteCalculator;
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
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteCalculatorTestFacade {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorTestFacade.class);

    private final RouteCalculator routeCalculator;
    private final StationRepository stationRepository;
    private final MutableGraphTransaction txn;
    private final StationGroupsRepository stationGroupsRepository;
    private final DiagnosticsToGraphViz diagnosticsToGraphViz;

    public  RouteCalculatorTestFacade(ComponentContainer componentContainer, MutableGraphTransaction txn) {
        this.routeCalculator = componentContainer.get(RouteCalculator.class);
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

    public List<Journey> calculateRouteAsList(FakeStation start, StationGroup end, JourneyRequest journeyRequest) {
        return calculateRouteAsList(start.from(stationRepository), end, journeyRequest);
    }

    public List<Journey> calculateRouteAsList(KnownLocality begin, KnownLocality end, JourneyRequest request) {
        return calculateRouteAsList(begin.from(stationGroupsRepository), end.from(stationGroupsRepository), request);
    }

    public @NotNull List<Journey> calculateRouteAsList(final Location<?> start, final Location<?> dest, final JourneyRequest request) {
        final Stream<Journey> stream = routeCalculator.calculateRoute(txn, start, dest, request);
        final List<Journey> result = stream.toList();
        stream.close();
        if (request.getDiagnosticsEnabled()) {
            if (request.hasReceivedDiagnostics()) {

                final JourneyDiagnostics diagnostics = request.getDiagnostics();

                createGraphFile(diagnostics, request, !result.isEmpty());
            } else {
                throw new RuntimeException("Diagnostics requested, but not received");
            }
        }
        return result;
    }

    private Station getFor(IdFor<Station> id) {
        return stationRepository.getStationById(id);
    }


    private void createGraphFile(final JourneyDiagnostics journeyDiagnostics, final JourneyRequest journeyRequest, boolean success) {
        final String fileName = createFilename(journeyRequest, success);

        logger.warn("Creating diagnostic dot file: " + fileName);

        try {
            final StringBuilder builder = new StringBuilder();
            builder.append("digraph G {\n");
            diagnosticsToGraphViz.appendTo(builder, journeyDiagnostics);
            builder.append("}");

            final FileWriter writer = new FileWriter(fileName);
            writer.write(builder.toString());
            writer.close();
            logger.info(format("Created file %s", fileName));
        }
        catch (IOException e) {
            logger.warn("Unable to create diagnostic graph file", e);
        }
    }

    private String createFilename(final JourneyRequest journeyRequest, boolean success) {
        final String status = success ? "found" : "notfound";
        final String dateString = journeyRequest.getDate().toLocalDate().toString();
        final String changes = "changes" + journeyRequest.getMaxChanges();
        final String postfix = journeyRequest.getUid().toString();
        TramTime queryTime = journeyRequest.getOriginalTime();

        String fileName = format("%s_%s%s_at_%s_%s_%s.dot", status,
                queryTime.getHourOfDay(), queryTime.getMinuteOfHour(),
                dateString, changes, postfix);
        fileName = fileName.replaceAll(":","");
        return fileName;
    }

}
