package com.tramchester.graph.search.diagnostics;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.diagnostics.DiagnosticReasonDTO;
import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;
import com.tramchester.domain.presentation.DTO.diagnostics.StationDiagnosticsDTO;
import com.tramchester.domain.presentation.DTO.diagnostics.StationDiagnosticsLinkDTO;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.nptg.NPTGRepository;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

@LazySingleton
public class DiagnosticsToGraphViz {

    private final NPTGRepository nptgRepository;
    private final StationRepository stationRepository;
    private final NodeContentsRepository nodeContentsRepository;

    private static final boolean includeAll = false;

    @Inject
    public DiagnosticsToGraphViz(NPTGRepository nptgRepository, StationRepository stationRepository,
                                 NodeContentsRepository nodeContentsRepository) {
        this.nptgRepository = nptgRepository;
        this.stationRepository = stationRepository;
        this.nodeContentsRepository = nodeContentsRepository;
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

        addNodeToDiagram(begin, builder, diagramState);

        List<StationDiagnosticsLinkDTO> links = diagnostic.getLinks();

        links.forEach(link -> {
            LocationRefWithPosition towards = link.getTowards();
            builder.append(format("\"%s\"->\"%s\" [label=\"%s\"]", begin.getId(), towards.getId(), display(link.getReasons())));
            addNodeToDiagram(towards, builder, diagramState);
        });

//        if (includeAll || !reason.isValid()) {
//            if (!diagramState.reasonIds.contains(reasonId)) {
//                diagramState.reasonIds.add(reasonId);
//                String shape = reason.isValid() ? "oval" : "octagon";
//                builder.append(format("\"%s\" [label=\"%s\"] [shape=%s];\n", reasonId, reason.textForGraph(), shape));
//            }
//
//            final Pair<GraphNodeId, String> reasonLink = Pair.of(endNodeId, reasonId);
//            if (!diagramState.reasonRelationships.contains(reasonLink)) {
//                diagramState.reasonRelationships.add(reasonLink);
//                builder.append(format("\"%s\"->\"%s\"", endNodeId, reasonId));
//            }
//        }
//
//        if (!howIGotHere.atStart()) {
//            final GraphRelationship relationship = transaction.getRelationshipById(howIGotHere.getRelationshipId());
//            final GraphNode fromNode = relationship.getStartNode(transaction);
//            addNodeToDiagram(fromNode, builder, diagramState, stateType.name());
//
//            final GraphNodeId fromNodeId = fromNode.getId();
//            final Pair<GraphNodeId,GraphNodeId> link = Pair.of(fromNodeId, endNodeId);
//            if (!diagramState.relationships.contains(link)) {
//                diagramState.relationships.add(link);
//                RelationshipType relationshipType = relationship.getType();
//                builder.append(format("\"%s\"->\"%s\" [label=\"%s\"]", fromNodeId, endNodeId, relationshipType.name()));
//            }
//        }
    }

    private String display(final List<DiagnosticReasonDTO> reasons) {
        final StringBuilder builder = new StringBuilder();
        reasons.forEach(reason -> {
            builder.append(reason.getText()).append("\n");
        });
        return builder.toString();
    }

    private void addNodeToDiagram(final LocationRefWithPosition location, final StringBuilder builder, final DiagramState diagramState) {
        IdForDTO id = location.getId();
        String name = location.getName();
        if (!diagramState.stationDiagnostics.contains(location)) {
            diagramState.stationDiagnostics.add(location);
            final StringBuilder label = new StringBuilder();
            label.append("\n").append(id).append("\n").append(name);
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
