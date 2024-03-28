package com.tramchester.graph.search.diagnostics;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.diagnostics.DiagnosticReasonDTO;
import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;
import com.tramchester.domain.presentation.DTO.diagnostics.StationDiagnosticsDTO;
import com.tramchester.domain.presentation.DTO.diagnostics.StationDiagnosticsLinkDTO;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

@LazySingleton
public class DiagnosticsToGraphViz {

    private final HashSet<Pair<IdForDTO, String>> locationToNode;
    private final HashSet<Pair<String, String>> reasonToReason;
//    private final HashSet<Pair<GraphNodeId, IdForDTO>> graphNodeToLocation;
    private final HashSet<Pair<String, String>> nodeToReason;

    @Inject
    public DiagnosticsToGraphViz() {
        locationToNode = new HashSet<>();
        reasonToReason = new HashSet<>();
//        graphNodeToLocation = new HashSet<>();
        nodeToReason = new HashSet<>();
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

        IdForDTO locationId = addNodeToDiagram(begin, diagnostic.getReasons(), builder, diagramState);

        List<StationDiagnosticsLinkDTO> links = diagnostic.getLinks();

        Set<String> seenGraphNodeId = new HashSet<>();

//        IdForDTO beginLocationId = begin.getId();
        links.forEach(link -> {

            LocationRefWithPosition towards = link.getTowards();

            builder.append(format("\"%s\"->\"%s\" [label=\"%s\"]\n", locationId,
                    towards.getId(), "towards"));

            link.getReasons().forEach(reason -> {
                final boolean valid = reason.isValid();

                String beginNodeId = reason.getBeginId();
                String endNodeId = reason.getEndId();

                String reasonNodeId = reason.getStateType().name() + "_" + reason.getCode() + "_" + endNodeId;

                // location -> begin node
                Pair<IdForDTO, String> locationToNodeKey = Pair.of(locationId, beginNodeId);
                if (!locationToNode.contains(locationToNodeKey)) {
                    builder.append(format("\"%s\"->\"%s\"\n", locationId, beginNodeId));
                    locationToNode.add(locationToNodeKey);
                }

                String reasonLinkText = valid ? "yes" : "no";
                String reasonColor = valid ? "green" : "red";

                // begin node -> reason node
                Pair<String, String> nodeToReasonKey = Pair.of(beginNodeId, reasonNodeId);
                if (!nodeToReason.contains(nodeToReasonKey)) {
                    builder.append(format("\"%s\"->\"%s\" [label=\"%s\" color=\"%s\"]\n", beginNodeId,
                            reasonNodeId, reasonLinkText, reasonColor));
                    nodeToReason.add(nodeToReasonKey);
                }

                String reasonShape = valid ? "oval" : "octagon";

                builder.append(format("\"%s\" [label=\"%s\"] [shape=\"%s\" style=\"filled\" color=\"%s\"]\n",
                        reasonNodeId, reason.getText(), reasonShape, reasonColor));

//                Pair<String, String> reasonLinkKey = Pair.of(reasonNodeId, endNodeId);
//                if (!reasonToReason.contains(reasonLinkKey)) {
//                    builder.append(format("\"%s\"->\"%s\" [label=\"RR\"] \n", reasonNodeId, endNodeId));
//                    reasonToReason.add(reasonLinkKey);
//                }
                seenGraphNodeId.add(endNodeId);
            });

            // towards node
            //addNodeToDiagram(towards, Collections.emptyList(), builder, diagramState);


//            builder.append(format("\"%s\"->\"%s\" [label=\"%s\"]", begin.getId(),
//                    towards.getId(), display(link.getReasons())));

        });


//        diagnostic.getAssociatedNodeIds().
//                stream().filter(graphNodeId -> seenGraphNodeId.contains(graphNodeId.toString())).
//                forEach(graphNodeId -> {
//                    Pair<GraphNodeId, IdForDTO> key = Pair.of(graphNodeId, beginLocationId);
//                    if (!graphNodeToLocation.contains(key)) {
//                        builder.append(format("\"%s\"->\"%s\" [label=\"%s\"]\n", graphNodeId,
//                                beginLocationId, "assoc"));
//                        graphNodeToLocation.add(key);
//                    }
//        });

    }

    private String display(final List<DiagnosticReasonDTO> reasons) {
        final StringBuilder builder = new StringBuilder();
        reasons.forEach(reason -> {
            builder.append(reason.getText()).append("\n");
        });
        return builder.toString();
    }

    private IdForDTO addNodeToDiagram(final LocationRefWithPosition location, List<DiagnosticReasonDTO> reasons,
                                      final StringBuilder builder, final DiagramState diagramState) {
        final IdForDTO id = location.getId();
        final String name = location.getName();

        final String nodeColor;
        if (reasons.isEmpty()) {
            nodeColor = "yellow";
        } else {
            boolean allInvalid = reasons.stream().noneMatch(DiagnosticReasonDTO::isValid);
            nodeColor = allInvalid ? "red" : "white";
        }
        if (!diagramState.stationDiagnostics.contains(location)) {
            diagramState.stationDiagnostics.add(location);
            String label = "\n" + id.getActualId() + "\n" + name + "\n" +
                    display(reasons);
            builder.append(format("\"%s\" [label=\"%s\"] [shape=%s style=\"filled\" fillcolor=\"%s\"];\n", id, label,
                    "hexagon", nodeColor));
        }
        return id;
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
