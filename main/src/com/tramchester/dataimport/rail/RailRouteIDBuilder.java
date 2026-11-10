package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.repository.RailRouteCallingPoints;
import com.tramchester.dataimport.rail.repository.RailRouteIds;
import com.tramchester.domain.Agency;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;


@LazySingleton
public class RailRouteIDBuilder {
    private static final Logger logger = LoggerFactory.getLogger(RailRouteIDBuilder.class);

    private final Set<RailRouteId> alreadyAllocated;
    private final Map<AgencyCallingPoints, RailRouteId> callingPointsToId;

    @Inject
    public RailRouteIDBuilder() {
        alreadyAllocated = new HashSet<>();
        callingPointsToId = new HashMap<>();
    }

    public RailRouteId getIdFor(final RailRouteCallingPoints railRouteCallingPoints) {
        final IdFor<Agency> agencyId = railRouteCallingPoints.getAgencyId();
        final List<IdFor<Station>> callingPoints = railRouteCallingPoints.getCallingPoints();
        // find unique route id based on the atoc code (aka agency) and list of calling points
        // have to include agency id since different agencies might serve same calling points

        final AgencyCallingPoints agencyCallingPoints = new AgencyCallingPoints(agencyId, callingPoints, railRouteCallingPoints.getMode());

        if (callingPointsToId.containsKey(agencyCallingPoints)) {
            return callingPointsToId.get(agencyCallingPoints);
        }

        int index = 1;
        RailRouteId railRouteId = RailRouteId.createId(agencyId, callingPoints, index);
        while (alreadyAllocated.contains(railRouteId)) {
            index = index + 1;
            railRouteId = RailRouteId.createId(agencyId, callingPoints, index);
        }

        alreadyAllocated.add(railRouteId);
        callingPointsToId.put(agencyCallingPoints, railRouteId);

        return railRouteId;
    }

    /***
     * create RailRouteId for each set of callings points
     * @param agencyId the agency for the callings points
     * @param railRoutes the set of all collections calling points, comprising all the "routes" for the agency
     * @return callings points mapped to RailRouteId
     */
    public Set<RailRouteIds.RailRouteCallingPointsWithRouteId> getRouteIdsFor(final IdFor<Agency> agencyId,
                                                                              final Stream<RailRouteCallingPoints> railRoutes) {

        // (begin,end) -> CallingPoints
        final Map<StationIdPair, Set<RailRouteCallingPoints>> routesGroupedByBeginEnd = new HashMap<>();

        // group by (begin, end) of the route as a whole
        railRoutes.forEach(route -> {
            final StationIdPair beginEnd = route.getBeginEnd();
            if (!routesGroupedByBeginEnd.containsKey(beginEnd)) {
                routesGroupedByBeginEnd.put(beginEnd, new HashSet<>());
            }
            routesGroupedByBeginEnd.get(beginEnd).add(route);
        });

        final Set<RailRouteIds.RailRouteCallingPointsWithRouteId> results = routesGroupedByBeginEnd.entrySet().stream().
                flatMap(grouped -> createRouteIdsFor(agencyId, grouped.getKey(), grouped.getValue()).stream()).
                collect(Collectors.toSet());

        logger.info("Added " + results.size() + " entries for " + agencyId);

        return results;
    }

    /***
     * Takes all railRoutes for a given agency and same Begin and End and creates RailRouteId for those
     * @param agencyId the agency for the routes
     * @param beginEnd the shared beginning and end for every route
     * @param matchingCallingPoints the rail 'routes' operating between being and end
     * @return routes decorated with the RailRouteId
     */
    private List<RailRouteIds.RailRouteCallingPointsWithRouteId> createRouteIdsFor(final IdFor<Agency> agencyId,
                               final StationIdPair beginEnd, final Set<RailRouteCallingPoints> matchingCallingPoints) {

        logger.debug("Create route ids for " + agencyId + " and " + beginEnd);

        if (matchingCallingPoints.size()==1) {
            return matchingCallingPoints.stream().
                    map(railRoute -> new RailRouteIds.RailRouteCallingPointsWithRouteId(railRoute, getIdFor(railRoute))).
                    collect(Collectors.toList());
        }

        // longest list first, so "sub-routes" are contains within larger routes
        final List<RailRouteCallingPoints> sortedRoutes = new ArrayList<>(matchingCallingPoints);
        sortedRoutes.sort(RailRouteCallingPoints::compareTo);

        // longestRoutes are created by 'consuming' shorter routes that are subsets of the calling points for the
        // longer routes
        final List<RailRouteCallingPoints> longestRoutes = new ArrayList<>();
        for (RailRouteCallingPoints agencyCallingPoints : sortedRoutes) {
            if (longestRoutes.stream().noneMatch(existing -> existing.contains(agencyCallingPoints))) {
                longestRoutes.add(agencyCallingPoints);
            }
        }

        // now gen Id's (only) for those longest routes
        final List<RailRouteIds.RailRouteCallingPointsWithRouteId> railRoutesWithIds = longestRoutes.stream().
                map(railRoute -> new RailRouteIds.RailRouteCallingPointsWithRouteId(railRoute, getIdFor(railRoute))).
                collect(Collectors.toList());

        logger.debug(format("Created %s rail route ids for %s %s for %s sets of calling points", railRoutesWithIds.size(), agencyId,
                beginEnd, matchingCallingPoints.size()));

        return railRoutesWithIds;
    }

//    private RailRouteId getIdFor(final IdFor<Agency> agencyId, final List<IdFor<Station>> callingPoints) {
//        // find unique route id based on the atoc code (aka agency) and list of calling points
//        // have to include agency id since different agencies might serve same calling points
//
//        final AgencyCallingPoints agencyCallingPoints = new AgencyCallingPoints(agencyId, callingPoints, railRouteCallingPoints.getMode());
//
//        if (callingPointsToId.containsKey(agencyCallingPoints)) {
//            return callingPointsToId.get(agencyCallingPoints);
//        }
//
//        int index = 1;
//        RailRouteId railRouteId = RailRouteId.createId(agencyId, callingPoints, index);
//        while (alreadyAllocated.contains(railRouteId)) {
//            index = index + 1;
//            railRouteId = RailRouteId.createId(agencyId, callingPoints, index);
//        }
//
//        alreadyAllocated.add(railRouteId);
//        callingPointsToId.put(agencyCallingPoints, railRouteId);
//        return railRouteId;
//
//    }

    /**
     * @param callingPoints order matters
     */
    private record AgencyCallingPoints(IdFor<Agency> agencyId, TransportMode mode, List<IdFor<Station>> callingPoints) {
            private AgencyCallingPoints(final IdFor<Agency> agencyId, final List<IdFor<Station>> stationCallingPoints, TransportMode mode) {
                this(agencyId, mode, stationCallingPoints);
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                AgencyCallingPoints that = (AgencyCallingPoints) o;
                return Objects.equals(agencyId, that.agencyId) && mode == that.mode && Objects.equals(callingPoints, that.callingPoints);
            }

        @Override
            public String toString() {
                return "AgencyCallingPoints{" +
                        "agencyId=" + agencyId +
                        ", mode=" + mode +
                        ", callingPoints=" + callingPoints +
                        '}';
            }
        }

}
