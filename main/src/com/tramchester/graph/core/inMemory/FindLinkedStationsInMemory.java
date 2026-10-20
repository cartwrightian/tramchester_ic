package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Service;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.ServiceCalendar;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.search.FindLinkedStations;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.core.GraphDirection.Outgoing;
import static com.tramchester.graph.reference.GraphLabel.STATION;
import static com.tramchester.graph.reference.TransportRelationshipTypes.LINKED;

@LazySingleton
public class FindLinkedStationsInMemory extends FindLinkedStations {
    private static final Logger logger = LoggerFactory.getLogger(FindLinkedStationsInMemory.class);

    private final GraphDatabaseInMemory graphDatabase;
    private final ServiceRepository serviceRepository;

    @Inject
    public FindLinkedStationsInMemory(GraphDatabaseInMemory graphDatabase,
                                      @SuppressWarnings("unused") StationsAndLinksGraphBuilder.Ready readyToken,
                                      StationRepository stationRepository, Geography geography, ServiceRepository serviceRepository) {
        super(stationRepository, geography);
        this.graphDatabase = graphDatabase;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public Set<StationToStationConnection> findLinkedFor(final TransportMode mode) {
        final GraphLabel forMode = GraphLabel.forMode(mode);

        try (final GraphTransaction txn = graphDatabase.beginTx()) {
            final Stream<GraphNode> nodes = txn.findNodes(STATION).
                    filter(node -> node.hasLabel(forMode)).
                    filter(node -> node.hasRelationship(txn, Outgoing, LINKED));

            return nodes.flatMap(node -> node.getRelationships(txn, Outgoing, LINKED)).
                    map(relationship -> createConnection(relationship, txn)).collect(Collectors.toSet());
        }
    }

    @Override
    public IdSet<Station> atLeastNLinkedStations(final TransportMode mode, final int threshhold) {
        final GraphLabel forMode = GraphLabel.forMode(mode);

        try (GraphTransaction txn = graphDatabase.beginTx()) {
            final Stream<GraphNode> nodes = txn.findNodes(STATION).
                    filter(node -> node.hasLabel(forMode)).
                    filter(node -> node.hasRelationship(txn, Outgoing, LINKED)).
                    filter(node -> node.getRelationships(txn, Outgoing, LINKED).count() >= threshhold);

            return nodes.map(GraphNode::getStationId).collect(IdSet.idCollector());

        }
    }

    @Override
    public DateRange dateRangeFor(final Station station, final int threshold) {

        final IdFor<Station> stationId = station.getId();
        logger.info("Find link date ranges for %s and threshold %s".formatted(stationId, threshold));

        // non-unique, might all run to the same calendar
        final List<Set<DateRange>> rangesPerLink;
        try (GraphTransaction txn = graphDatabase.beginTx()) {
            final GraphNode node = txn.findNode(station);
            if (!node.hasRelationship(txn, Outgoing, LINKED)) {
                String msg = "Station %s does not have any links";
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            final List<IdSet<Service>> servicesPerLink = node.getRelationships(txn, Outgoing, LINKED).
                    map(GraphRelationship::getServiceIds).
                    toList();

            if (servicesPerLink.size()<threshold) {
                String msg = "Not enough links for %s, got %s and threshold %s. Not an interchange?".
                        formatted(stationId, servicesPerLink.size(), threshold);
                logger.error(msg);
                throw new RuntimeException(msg);
            } else {
                logger.info("Found %s links/calendars for %s".formatted(servicesPerLink.size(), stationId));
            }

            rangesPerLink = servicesPerLink.stream().
                    map(this::createAggregateFor).
                    toList();
        }

        final Set<DateRange> dateRanges = overlappingCalendarsOverThreshold(threshold, rangesPerLink, stationId);

        logger.info("Found %s unique calendars matching %s and threshold %s".formatted(dateRanges.size(), stationId, threshold));

//        final Set<DateRange> dateRanges = results.stream().
//                map(ServiceCalendar::getDateRange).
//                collect(Collectors.toSet());

        if (dateRanges.isEmpty()) {
            String msg = "Did not find any date ranges for %s and %s, services %s".
                    formatted(stationId, threshold, rangesPerLink);
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        final DateRange result = DateRange.leastOverlap(dateRanges);

        // TODO into debug? Should not be too noisy, only interchanges
        logger.info("For station %s and threshold %s found %s (from %s)".
                formatted(stationId, threshold, result, dateRanges));

        return result;
    }

    /***
     * Take the list of aggregated calendars, one per link, and create a set of calendars that represent
     * the overlaps period when >= threshold links are all available
     * @param threshold number of links that need to be active during same period
     * @param dateRangesPerLink the list of aggregated calendars. assuming one per link.
     * @param stationId for diagnostics
     * @return a set calendars, each representing the unique period when the calendarsPerLink overlap such that >=threshold links are available
     */
    private static Set<DateRange> overlappingCalendarsOverThreshold(final int threshold,
              final List<Set<DateRange>> dateRangesPerLink, final IdFor<Station> stationId) {

        final List<DateRange> rangesThatOverlap = new ArrayList<>();
        int linksThatOverlap = 0;
        for (int current = 0; current < dateRangesPerLink.size()-1; current++) {
            final Set<DateRange> currentRanges = dateRangesPerLink.get(current);
            // must to be a list, want to count a link even if same calendar as different link
            //final List<ServiceCalendar> calendarsThatOverlap = new ArrayList<>();
            //calendarsThatOverlap.add(currentCalendar);

            for (int other = current+1; other < dateRangesPerLink.size(); other++) {
                final Set<DateRange> otherRanges = dateRangesPerLink.get(other);
                Set<DateRange> overlaps = findOverlaps(currentRanges, otherRanges);
                if (!overlaps.isEmpty()) {
                    linksThatOverlap++;
                    rangesThatOverlap.addAll(overlaps);
                }
            }
//            if (calendarsThatOverlap.isEmpty()) {
//                logger.warn("Saw no overlapping calendars for " + stationId);
//            } else {
//                logger.info("Saw %s overlapping calendars for %s".formatted(calendarsThatOverlap.size(), stationId));
//            }
//            if (calendarsThatOverlap.size() >= threshold) {
//                // enough links overlap that we are over the threshold
//                final ServiceCalendar overlapping = new AggregateServiceCalendar(calendarsThatOverlap);
//                results.add(overlapping);
//            }
        }
        logger.info("Found %s non-unique calendars for %s and threshold %s".formatted(linksThatOverlap, stationId, threshold));
        if (linksThatOverlap>=threshold) {
            return new HashSet<>(rangesThatOverlap);
        }

        return Collections.emptySet();
    }

    private static Set<DateRange> findOverlaps(Set<DateRange> rangesA, Set<DateRange> rangesB) {
        return rangesA.stream().
                filter(rangeA -> rangesB.stream().anyMatch(rangeB -> rangeB.overlapsWith(rangeA))).
                collect(Collectors.toSet());
    }

    private Set<DateRange> createAggregateFor(final IdSet<Service> serviceIds) {
        return serviceIds.stream().
                map(serviceRepository::getServiceById).
                map(Service::getCalendar).
                map(ServiceCalendar::getDateRange).
                collect(Collectors.toSet());

        //return new AggregateServiceCalendar(calendars);

    }
}
