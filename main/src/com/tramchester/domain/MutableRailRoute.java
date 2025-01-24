package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

public class MutableRailRoute extends MutableRoute implements RailRoute {

    // used to find and name unique routes for rail, as routes are not part of the data set we have to
    // derive them
    private final List<Station> callingPoints;
    private static final Set<TransportMode> railModes = EnumSet.of(TransportMode.Train, TransportMode.RailReplacementBus);

    public MutableRailRoute(final RailRouteId id, final List<Station> callingPoints, final Agency agency, final TransportMode transportMode) {
        super(id, createShortName(agency, callingPoints), createName(agency, callingPoints), agency, transportMode);
        if (callingPoints.size()<2) {
            final String message = format("Need at least 2 calling points route %s (%s) and calling points %s",
                    id, transportMode, callingPoints);
            throw new RuntimeException(message);
        }
        if (!railModes.contains(transportMode)) {
            throw new RuntimeException("Invalid mode " + transportMode + " must be on of " + railModes);
        }
        this.callingPoints = callingPoints;
    }

    @Override
    public Station getBegin() {
        return callingPoints.getFirst();
    }

    @Override
    public Station getEnd() {
        return callingPoints.getLast();
    }

    @Override
    public boolean callsAtInOrder(final Station first, final Station second) {
        final int indexOfFirst = callingPoints.indexOf(first);
        if (indexOfFirst<0) {
            return false;
        }
        final int indexOfSecond = callingPoints.indexOf(second);
        if (indexOfSecond<0) {
            return false;
        }
        return indexOfSecond>indexOfFirst;

    }

    @Override
    public List<Station> getCallingPoints() {
        return callingPoints;
    }


    @Override
    public String toString() {
        return "MutableRailRoute{" +
                "callingPoints=" + HasId.asIds(callingPoints) +
                "} " + super.toString();
    }

    private static String createShortName(final Agency agency, final List<Station> callingPoints) {
        final Station first = callingPoints.getFirst();
        final Station last = callingPoints.getLast();

        return format("%s service from %s to %s", agency.getName(), first.getName(), last.getName());

    }

    private static String createName(final Agency agency, final List<Station> callingPoints) {
        final Station first = callingPoints.getFirst();
        final Station last = callingPoints.getLast();
        final StringBuilder result = new StringBuilder();

        result.append(format("%s service from %s to %s", agency.getName(), first.getName(), last.getName()));

        final int lastIndex = callingPoints.size() - 1;
        for (int i = 1; i < lastIndex; i++) {
            if (i>1) {
                result.append(", ");
            } else {
                result.append(" via ");
            }
            result.append(callingPoints.get(i).getName());
        }
        return result.toString();
    }

}
