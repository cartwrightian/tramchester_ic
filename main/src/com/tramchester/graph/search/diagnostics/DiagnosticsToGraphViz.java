package com.tramchester.graph.search.diagnostics;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.diagnostics.DiagnosticReasonDTO;
import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;
import com.tramchester.domain.presentation.DTO.diagnostics.StationDiagnosticsDTO;
import com.tramchester.domain.presentation.DTO.diagnostics.StationDiagnosticsLinkDTO;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

@LazySingleton
public class DiagnosticsToGraphViz {

    private static final boolean includeAll = false;

    @Inject
    public DiagnosticsToGraphViz() {
    }

    public void appendTo(final StringBuilder builder, final JourneyDiagnostics diagnostics) {
        final DiagramState diagramState = new DiagramState();
        List<StationDiagnosticsDTO> stationDiagnostics = diagnostics.getDtoList();
        stationDiagnostics.forEach(diag -> add(diag, builder, diagramState));
        diagramState.clear();
    }

    private void add(final StationDiagnosticsDTO diagnostic, final StringBuilder builder,
                     final DiagramState diagramState) {
        //final HowIGotHere howIGotHere = reason.getHowIGotHere();

        LocationRefWithPosition begin = diagnostic.getBegin();

        addNodeToDiagram(begin, diagnostic.getReasons(), builder, diagramState);

        List<StationDiagnosticsLinkDTO> links = diagnostic.getLinks();

        Set<String> seenGraphNodeId = new HashSet<>();

        links.forEach(link -> {
            LocationRefWithPosition towards = link.getTowards();

            builder.append(format("\"%s\"->\"%s\" [label=\"%s\"]", begin.getId(),
                    towards.getId(), "link"));

            link.getReasons().forEach(reason -> {
                builder.append(format("\"%s\"->\"%s\" [label=\"%s\"]", begin.getId(),
                        reason.getBeginId(), "reason"));

                builder.append(format("\"%s\" [label=\"%s\"]", reason.getBeginId(), reason.getText()));

                builder.append(format("\"%s\"->\"%s\" ", reason.getBeginId(),
                        reason.getEndId()));
                seenGraphNodeId.add(reason.getEndId());
            });


//            builder.append(format("\"%s\"->\"%s\" [label=\"%s\"]", begin.getId(),
//                    towards.getId(), display(link.getReasons())));
            addNodeToDiagram(towards, Collections.emptyList(), builder, diagramState);
        });


        diagnostic.getAssociatedNodeIds().
                stream().filter(graphNodeId -> seenGraphNodeId.contains(graphNodeId.toString())).
                forEach(graphNodeId -> {
            builder.append(format("\"%s\"->\"%s\" [label=\"%s\"]", graphNodeId,
                    begin.getId(), "assoc"));
        });

    }

    private String display(final List<DiagnosticReasonDTO> reasons) {
        final StringBuilder builder = new StringBuilder();
        reasons.forEach(reason -> {
            builder.append(reason.getText()).append("\n");
        });
        return builder.toString();
    }

    private void addNodeToDiagram(final LocationRefWithPosition location, List<DiagnosticReasonDTO> reasons,
                                  final StringBuilder builder, final DiagramState diagramState) {
        final IdForDTO id = location.getId();
        final String name = location.getName();
        if (!diagramState.stationDiagnostics.contains(location)) {
            diagramState.stationDiagnostics.add(location);
            final StringBuilder label = new StringBuilder();
            label.append("\n").append(id.getActualId()).append("\n").append(name).append("\n");
            label.append(display(reasons));
            builder.append(format("\"%s\" [label=\"%s\"] [shape=%s];\n", id, label, "hexagon"));
        }
    }


    private static class DiagramState {
        private final Set<LocationRefWithPosition> stationDiagnostics;

        private DiagramState() {

            stationDiagnostics = new HashSet<>();
        }

        public void clear() {
            stationDiagnostics.clear();
        }
    }
}
