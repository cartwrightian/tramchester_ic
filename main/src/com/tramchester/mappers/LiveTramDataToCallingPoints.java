package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.DeparturesRepository;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import jakarta.inject.Inject;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@LazySingleton
public class LiveTramDataToCallingPoints {
    private static final Logger logger = LoggerFactory.getLogger(LiveTramDataToCallingPoints.class);

    private final DeparturesRepository departuresRepository;

    @Inject
    public LiveTramDataToCallingPoints(DeparturesRepository departuresRepository) {
        this.departuresRepository = departuresRepository;
    }

    public Set<StationPair> map(final List<TramStationDepartureInfo> updates) {
        logger.info("Received " + updates.size() + " departure updates");

        Set<StationPair> allAmbiguousRoutings = updates.stream().
                flatMap(update -> map(update).stream()).
                collect(Collectors.toSet());

        if (!allAmbiguousRoutings.isEmpty()) {
            logger.error("Found:" + allAmbiguousRoutings.size() + " Ambiguous between " + HasId.asIds(allAmbiguousRoutings));

            IdSet<Station> ambiguousStarts = allAmbiguousRoutings.stream().map(StationPair::getBegin).collect(IdSet.collector());
            IdSet<Station> ambiguousEnds = allAmbiguousRoutings.stream().map(StationPair::getEnd).collect(IdSet.collector());

            logger.info("Starts: " +ambiguousStarts.size() + " " + ambiguousStarts);
            logger.info("Ends:" + ambiguousEnds.size() + " " + ambiguousEnds);
        }

        return allAmbiguousRoutings;
    }

    private Set<StationPair> map(TramStationDepartureInfo update) {
        Station displayStation = update.getStation();

        return update.getDueTrams().stream().
                map(UpcomingDeparture::getDestination).
                filter(dest -> !dest.equals(displayStation)).
                filter(dest -> ambiguousRoutingBetween(update, dest)).
                map(dest -> StationPair.of(update.getStation(), dest)).
                collect(Collectors.toSet());
    }

    /**
     * @param tramStationDepartureInfo the live display information
     * @param destination the destination of the due tram
     * @return true if ambiguous
     */
    private boolean ambiguousRoutingBetween(TramStationDepartureInfo tramStationDepartureInfo, Station destination) {
        Station displayStation = tramStationDepartureInfo.getStation();

        final Set<Route> pickups;
        if (tramStationDepartureInfo.hasStationPlatform()) {
            final Platform platform = tramStationDepartureInfo.getStationPlatform();
            pickups = platform.getPickupRoutes();
        } else {
            pickups = displayStation.getPickupRoutes();
        }

        Set<Route> dropoffs = destination.getDropoffRoutes();

        SetUtils.SetView<Route> overlap = SetUtils.union(pickups, dropoffs);

        String suffix = " between " + displayStation.getId() + " and " + destination.getId();

        if (overlap.isEmpty()) {
            logger.error("No overlap " + suffix);
            return false;
        }

        if (overlap.size()==1) {
            logger.debug("Only one route (" + HasId.asIds(overlap) + ") " + suffix);
            return false;
        }

        // appears this does not happen when we have more than one route....
        boolean sameCallingPoints = sameCallingPoints(overlap, displayStation, destination);
        if (sameCallingPoints) {
            logger.info("Same calling points for routes " + HasId.asIds(overlap) + suffix);
        }
        return !sameCallingPoints;
    }

    private boolean sameCallingPoints(SetUtils.SetView<Route> overlap, Station start, Station end) {
        Set<List<Station>> allCallingPoints = new HashSet<>();
        for(Route route : overlap) {
            route.getTrips().forEach(trip -> {
                if (trip.callsAt(start.getId()) && trip.callsAt(end.getId())) {
                    List<Station> callingPoints = new ArrayList<>();
                    final StopCalls stopCalls = trip.getStopCalls();
                    final int firstIndex = stopCalls.getStopFor(start.getId()).getGetSequenceNumber();
                    final int lastIndex = stopCalls.getStopFor(end.getId()).getGetSequenceNumber();
                    for (int i = firstIndex; i <= lastIndex; i++) {
                        callingPoints.add(stopCalls.getStopBySequenceNumber(i).getStation());
                    }
                    allCallingPoints.add(callingPoints);
                }
            });
        }

        boolean sameCallingPoints = allCallingPoints.size() == 1;
        if (sameCallingPoints) {
            // unambiguous
            // seems this  not happen, if multiple routes we always end up with an ambiguous set of calling points
            logger.info("Unambiguous calling points between " + start.getId() + " and " + end.getId()
                    + " for routes " + HasId.asIds(overlap));
        } else {
            logger.error("Ambiguous calling points between " + start.getId() + " and " + end.getId()
                    + " for routes " + HasId.asIds(overlap));
        }

        return sameCallingPoints;
    }

    public List<UpcomingDeparture> nextTramFor(final StationPair journeyBeginAndEnd, LocalDate date, TramTime time, EnumSet<TransportMode> modes) {

        Station journeyStart = journeyBeginAndEnd.getBegin();
        Station journeyDest = journeyBeginAndEnd.getEnd();

        List<UpcomingDeparture> departures = departuresRepository.getDueForLocation(journeyStart, date, time, modes);

        logger.info("Found " + departures.size() + " departures for " + journeyStart.getId());

        // quick win
        List<UpcomingDeparture> quickWin = departures.stream().filter(departure -> departure.getDestination().equals(journeyDest)).toList();

        if (!quickWin.isEmpty()) {
            logger.info("Quick win Found trams " + quickWin.size() + " with matching destination " + journeyDest.getId());
            return quickWin;
        }

        

        throw new RuntimeException("Not implemented yet");
        //return Collections.emptyList();
    }
}
