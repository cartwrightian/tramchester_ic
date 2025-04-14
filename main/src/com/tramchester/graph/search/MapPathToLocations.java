package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Path;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static com.tramchester.graph.graphbuild.GraphLabel.*;
import static java.lang.String.format;

@LazySingleton
public class MapPathToLocations {
    private final StationRepository stationRepository;
    private final StationGroupsRepository stationGroupsRepository;

    @Inject
    public MapPathToLocations(StationRepository stationRepository,
                              StationGroupsRepository stationGroupsRepository) {
        this.stationRepository = stationRepository;
        this.stationGroupsRepository = stationGroupsRepository;
    }

    public List<Location<?>> mapToLocations(Path path, GraphTransaction txn) {
        Location<?> previous = null;
        List<Location<?>> results = new ArrayList<>();
        for(GraphNode node : txn.iter(path.nodes())) {
//            GraphNode node = GraphNode.from(pathNode);

            Optional<Location<?>> maybeLocation = mapNode(node);
            maybeLocation.ifPresent(location -> {});
            if (maybeLocation.isPresent()) {
                Location<?> location = maybeLocation.get();
                if (results.isEmpty()) {
                    results.add(location);
                } else  {
                    if (!location.equals(previous)) {
                        results.add(location);
                    }
                }
                previous = location;
            }
        }
        return results;
    }

    private Optional<Location<?>> mapNode(final GraphNode node) {
        final EnumSet<GraphLabel> labels = node.getLabels();
        if (labels.contains(GROUPED)) {
            //return getAreaIdFromGrouped(graphNode.getNode());
            final IdFor<NPTGLocality> areaId = node.getAreaId();
            final StationLocalityGroup stationGroup = stationGroupsRepository.getStationGroupForArea(areaId);
            if (stationGroup==null) {
                throw new RuntimeException(format("Missing grouped station %s for %s labels %s props %s",
                        areaId, node.getId(), node.getLabels(), node.getAllProperties()));
            }
            return Optional.of(stationGroup);
        }
        if (labels.contains(STATION)) {
            IdFor<Station> stationId = node.getStationId();
            return Optional.of(stationRepository.getStationById(stationId));
        }
        if (labels.contains(ROUTE_STATION)) {
            IdFor<Station> stationId = node.getStationId();
            return Optional.of(stationRepository.getStationById(stationId));
        }
        if (labels.contains(QUERY_NODE)) {
            LatLong latLong =  node.getLatLong(); // GraphProps.getLatLong(node);
            return  Optional.of(MyLocation.create(latLong));
        }
        return Optional.empty();
    }

}
