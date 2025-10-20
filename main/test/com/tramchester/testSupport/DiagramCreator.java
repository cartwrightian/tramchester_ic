package com.tramchester.testSupport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.graph.core.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.nptg.NPTGRepository;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.graph.reference.TransportRelationshipTypes.*;
import static com.tramchester.graph.reference.GraphLabel.*;
import static java.lang.String.format;


@LazySingleton
public class DiagramCreator {
    private static final Logger logger = LoggerFactory.getLogger(DiagramCreator.class);

    private final GraphDatabase graphDatabase;
    private final TransportRelationshipTypes[] toplevelRelationships =
            new TransportRelationshipTypes[]{LINKED, ON_ROUTE, ROUTE_TO_STATION, STATION_TO_ROUTE, DIVERSION,
                    ENTER_PLATFORM, LEAVE_PLATFORM, DIVERSION_DEPART  };
    private final StationRepository stationRepository;
    private final NPTGRepository nptgRepository;

    private static final Path diagramsFolder = Path.of("diagrams");

    enum Shape {
        oval, house, octagon, box
    }

    enum Color {
        black, blue, darkgreen, red
    }

    @Inject
    public DiagramCreator(GraphDatabase graphDatabase, StationRepository stationRepository,
                          NPTGRepository nptgRepository) {
        // ready is token to express dependency on having a built graph DB
        this.graphDatabase = graphDatabase;
        this.stationRepository = stationRepository;
        this.nptgRepository = nptgRepository;
    }

    public void create(Path filename, Station station, int depthLimit, boolean topLevel) throws IOException {
        create(filename, Collections.singleton(station), depthLimit, topLevel);
    }

    public void create(Path diagramFile, Collection<Station> startPointsList, int depthLimit, boolean topLevel) throws IOException {

        createFolderIfRequired();

        Path filePath = diagramsFolder.resolve(diagramFile);
        logger.info("Creating diagram " + filePath.toAbsolutePath());

        Set<GraphNodeId> nodeSeen = new HashSet<>();
        Set<GraphRelationshipId> relationshipSeen = new HashSet<>();

        OutputStream fileStream = new FileOutputStream(filePath.toFile());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileStream);
        PrintStream printStream = new PrintStream(bufferedOutputStream);

        final DiagramBuild builder = new DiagramBuild(printStream);

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            builder.append("digraph G {\n");

            startPointsList.forEach(startPoint -> {

                final GraphNode startNode = txn.findNode((Location<?>) startPoint);

                if (startNode==null) {
                    logger.error("Can't find start node for station " + startPoint.getId());
                    builder.append("MISSING NODE\n");
                } else {
                    visit(startNode, builder, depthLimit, nodeSeen, relationshipSeen, topLevel, txn);
                }
            });

            builder.append("}");
        }

        relationshipSeen.clear();
        nodeSeen.clear();

        printStream.close();
        bufferedOutputStream.close();
        fileStream.close();

        logger.info("Finished diagram");
    }

    private void createFolderIfRequired() throws IOException {
        if (Files.exists(diagramsFolder)) {
            return;
        }
        Files.createDirectory(diagramsFolder);
    }

    private void visit(final GraphNode node, final DiagramBuild builder, final int depth,
                       final Set<GraphNodeId> nodeSeen, final Set<GraphRelationshipId> relationshipSeen,
                       boolean topLevel, GraphTransaction txn) {
        if (depth<=0) {
            return;
        }
        if (nodeSeen.contains(node.getId())) {
            return;
        }
        nodeSeen.add(node.getId());

        addLine(builder, format("\"%s\" %s;\n", createNodeId(node), getAttributesFor(node)));

        visitOutbounds(node, builder, depth, nodeSeen, relationshipSeen, topLevel, txn);
        visitInbounds(node, builder, depth, nodeSeen, relationshipSeen, topLevel, txn);

    }

    private void visitInbounds(GraphNode targetNode, DiagramBuild builder, int depth, Set<GraphNodeId> nodeSeen,
                               Set<GraphRelationshipId> relationshipSeen,
                               boolean topLevel, GraphTransaction txn) {
        getRelationships(targetNode, GraphDirection.Incoming, topLevel, txn).forEach(towards -> {

            GraphNode startNode = towards.getStartNode(txn);
            addNode(builder, startNode);

            // startNode -> targetNode
            addEdge(builder, towards, createNodeId(startNode), createNodeId(targetNode), relationshipSeen);
            visit(startNode, builder, depth-1, nodeSeen, relationshipSeen, topLevel, txn);
        });
    }

    private Stream<GraphRelationship> getRelationships(GraphNode targetNode, GraphDirection direction,
                                                       boolean toplevelOnly, GraphTransaction txn) {
        TransportRelationshipTypes[] types = toplevelOnly ?  toplevelRelationships : TransportRelationshipTypes.values();
        return targetNode.getRelationships(txn, direction, types);
    }

    private void visitOutbounds(GraphNode startNode, DiagramBuild builder, int depth, Set<GraphNodeId> seen,
                                Set<GraphRelationshipId> relationshipSeen, boolean topLevel, GraphTransaction txn) {
        Map<GraphRelationshipId,GraphRelationship> goesToRelationships = new HashMap<>();

        getRelationships(startNode, GraphDirection.Outgoing, topLevel, txn).forEach(awayFrom -> {

            GraphNode rawEndNode = awayFrom.getEndNode(txn);

            addNode(builder, startNode);
            addEdge(builder, awayFrom, createNodeId(startNode), createNodeId(rawEndNode), relationshipSeen);

            if (TransportRelationshipTypes.goesTo(awayFrom.getType())) {
                if (!goesToRelationships.containsKey(awayFrom.getId())) {
                    goesToRelationships.put(awayFrom.getId(), awayFrom);
                }
            }
            visit(rawEndNode, builder, depth-1, seen, relationshipSeen, topLevel, txn);
        });

        // add services for this node
        // Node -> Service End Node
    }


    private void addEdge(DiagramBuild builder, GraphRelationship edge, String startNodeId, String endNodeId,
                         Set<GraphRelationshipId> relationshipSeen) {

        TransportRelationshipTypes relationshipType = TransportRelationshipTypes.valueOf(edge.getType().name());

        if (relationshipSeen.contains(edge.getId())) {
            return;
        }
        relationshipSeen.add(edge.getId());

        if (relationshipType==TransportRelationshipTypes.ON_ROUTE) {
            addLine(builder, format("\"%s\"->\"%s\" [color=\"%s\"];\n", startNodeId, endNodeId,
                    getColorFor(relationshipType)));
        } else if (relationshipType== LINKED) {
            Set<TransportMode> modes = edge.getTransportModes();
            addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\" color=\"%s\"];\n", startNodeId, endNodeId, "L:"+modes,
                    getColorFor(relationshipType)));
        } else {
            String shortForm = createShortForm(relationshipType, edge);
            addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\" color=\"%s\"];\n", startNodeId, endNodeId, shortForm,
                    getColorFor(relationshipType)));
        }
    }

    private Color getColorFor(TransportRelationshipTypes relationshipType) {
        return switch (relationshipType) {
            case ON_ROUTE -> Color.red;
            case LINKED -> Color.blue;
            case ENTER_PLATFORM, LEAVE_PLATFORM -> Color.darkgreen;
            default -> Color.black;
        };
    }

    private void addNode(final DiagramBuild builder, final GraphNode sourceNode) {
        addLine(builder, format("\"%s\" %s;\n", createNodeId(sourceNode),
                getAttributesFor(sourceNode)));
    }

    private String createNodeId(final GraphNode node) {
        return node.getId().toString();
    }

    private String getAttributesFor(final GraphNode node) {
        return format("[label=\"%s\" shape=\"%s\" color=\"%s\"]", getLabelFor(node), getShapeFor(node).name(),
                getColorFor(node).name());
    }

    private Color getColorFor(GraphNode node) {
        if (node.hasLabel(ROUTE_STATION)) {
            return Color.red;
        }
        if (node.hasLabel(STATION)) {
            return Color.blue;
        }
        if (node.hasLabel(PLATFORM)) {
            return Color.darkgreen;
        }
        return Color.black;
    }

    private Shape getShapeFor(final GraphNode node) {
        if (node.hasLabel(PLATFORM)) {
            return Shape.box;
        }
        if (node.hasLabel(ROUTE_STATION)) {
            return Shape.oval;
        }
        if (node.hasLabel(STATION)) {
            return Shape.house;
        }
        if (node.hasLabel(SERVICE)) {
            return Shape.octagon;
        }
        if (node.hasLabel(HOUR)) {
            return Shape.box;
        }
        if (node.hasLabel(GraphLabel.MINUTE)) {
            return Shape.box;
        }
        return Shape.box;
    }

    private String getLabelFor(final GraphNode node) {
        if (node.hasLabel(PLATFORM)) {
            return node.getPlatformId().getGraphId();
        }
        if (node.hasLabel(ROUTE_STATION)) {

            String stationId = node.getStationId().getGraphId();
            TransportMode mode = node.getTransportMode();
            String routeId = node.getRouteId().getGraphId();
            return format("%s\n%s\n%s", routeId, stationId, mode.name());
        }
        if (node.hasLabel(GROUPED)) {
            //return getAreaIdFromGrouped(graphNode.getNode());
            IdFor<NPTGLocality> areaId = node.getAreaId();
            if (nptgRepository.hasLocality(areaId)) {
                NPTGLocality area = nptgRepository.get(areaId);
                return format("%s %s\n%s", area.getLocalityName(), area.getParentLocalityName(), areaId.getGraphId());
            } else {
                return format("unknown locality %s\n%s", areaId, areaId.getGraphId());
            }
        }
        if (node.hasLabel(STATION)) {
            //return getStationIdFrom(node.getNode());
            IdFor<Station> stationId = node.getStationId();
            Station station = stationRepository.getStationById(stationId);
            return format("%s\n%s", station.getName(), stationId.getGraphId());
        }
        if (node.hasLabel(SERVICE)) {
            return node.getServiceId().getGraphId();
        }
        if (node.hasLabel(HOUR)) {
            return Integer.toString(node.getHour());
        }
        if (node.hasLabel(MINUTE)) {
            final TramTime time = node.getTime();
            String days = time.isNextDay() ? "+1" : "";
            return format("%s:%s\n%s", time.getHourOfDay(), time.getMinuteOfHour(), days);
        }
        if (node.hasLabel(GROUPED)) {
            IdFor<Station> stationId = node.getStationId();
            Station station = stationRepository.getStationById(stationId);
            return format("%s\n%s\n%s", station.getName(), station.getLocalityId(), stationId.getGraphId());
        }

        return "No_Label";
    }

    private void addLine(DiagramBuild builder, String line) {
        builder.append(line);
    }

    private String createShortForm(TransportRelationshipTypes relationshipType, GraphRelationship edge) {
        String text = "";
        if (hasCost(relationshipType)) {
            Duration cost = edge.getCost();
            if (!cost.isZero()) {
                text = "(" + edge.getCost() + ")";
            }
        }
        IdSet<Trip> tripIds = edge.getTripIds();
        if (!tripIds.isEmpty()) {
            if (!text.isEmpty()) {
                text = text + System.lineSeparator();
            }
            text = text + tripIds;
        } else {
            if (edge.hasProperty(GraphPropertyKey.TRIP_ID)) {
                text = text + edge.getTripId();
            }
        }
        return getNameFor(relationshipType) + text;
    }

    @NotNull
    private String getNameFor(TransportRelationshipTypes relationshipType) {
        return switch (relationshipType) {
            case ENTER_PLATFORM -> "E";
            case LEAVE_PLATFORM -> "L";
            case WALKS_TO_STATION -> "WT";
            case BOARD -> "B";
            case TO_SERVICE -> "Svc";
            case TO_HOUR -> "H";
            case TO_MINUTE -> "T";
            case ON_ROUTE -> "R";
            case INTERCHANGE_BOARD -> "IB";
            case INTERCHANGE_DEPART -> "ID";
            case DIVERSION_DEPART -> "DD";
            case TRAM_GOES_TO -> "Tram";
            case DEPART -> "D";
            case BUS_GOES_TO -> "Bus";
            case TRAIN_GOES_TO -> "Train";
            case LINKED -> "Link";
            case DIVERSION -> "divert";
            case NEIGHBOUR -> "neigh";
            case FERRY_GOES_TO -> "Ferry";
            case SUBWAY_GOES_TO -> "Subway";
            case GROUPED_TO_CHILD -> "groupChild";
            case GROUPED_TO_PARENT -> "groupParent";
            case GROUPED_TO_GROUPED -> "G2G";
            case ROUTE_TO_STATION -> "RS";
            case STATION_TO_ROUTE -> "SR";
            case WALKS_FROM_STATION -> "WF";
        };
    }

    private static class DiagramBuild {
        private final PrintStream printStream;

        public DiagramBuild(PrintStream printStream) {
            this.printStream = printStream;
        }

        public void append(String text) {
            printStream.print(text);
        }
    }
}
