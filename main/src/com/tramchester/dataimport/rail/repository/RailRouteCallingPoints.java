package com.tramchester.dataimport.rail.repository;

import com.tramchester.domain.Agency;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class RailRouteCallingPoints implements Comparable<RailRouteCallingPoints> {

    private final IdFor<Agency> agencyId;
    private final List<IdFor<Station>> callingPoints;
    private final StationIdPair beginEnd;
    private final TransportMode mode;

    public RailRouteCallingPoints(IdFor<Agency> agencyId, List<IdFor<Station>> callingPoints, TransportMode mode) {
        this(agencyId, callingPoints, findBeginAndEnd(callingPoints), mode);
    }

    private RailRouteCallingPoints(IdFor<Agency> agencyId, List<IdFor<Station>> callingPoints, StationIdPair beginEnd, TransportMode mode) {
        this.agencyId = agencyId;
        this.callingPoints = callingPoints;
        this.beginEnd = beginEnd;
        this.mode = mode;
    }

    public IdFor<Agency> getAgencyId() {
        return agencyId;
    }

    @NotNull
    private static StationIdPair findBeginAndEnd(final List<IdFor<Station>> callingPoints) {
        if (callingPoints.size() < 2) {
            throw new RuntimeException("Not enough calling points for " + callingPoints);
        }
        final IdFor<Station> first = callingPoints.getFirst();
        final IdFor<Station> last = callingPoints.getLast();
        return StationIdPair.of(first, last);
    }

    public List<IdFor<Station>> getCallingPoints() {
        return callingPoints;
    }

    public int numberCallingPoints() {
        return callingPoints.size();
    }

    public boolean contains(final RailRouteCallingPoints other) {
        if (!agencyId.equals(other.agencyId)) {
            throw new RuntimeException("AgencyId mismatch for " + this + " and provided " + other);
        }

        // to be same route need same begin and end
        if (!beginEnd.equals(other.beginEnd)) {
            return false;
        }

        if (!mode.equals(other.mode)) {
            return false;
        }

        final int otherSize = other.numberCallingPoints();
        final int size = numberCallingPoints();

        // can't contain a bigger list
        if (otherSize > size) {
            return false;
        }

        // both lists are ordered by calling order
        int searchIndex = 0;

        for (int i = 0; i < size; i++) {
            if (callingPoints.get(i).equals(other.callingPoints.get(searchIndex))) {
                searchIndex++;
            }
            if (searchIndex >= otherSize) {
                break;
            }
        }
        return searchIndex == otherSize;

    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RailRouteCallingPoints that = (RailRouteCallingPoints) o;
        return Objects.equals(agencyId, that.agencyId) && Objects.equals(callingPoints, that.callingPoints)
                && Objects.equals(beginEnd, that.beginEnd) && mode == that.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(agencyId, callingPoints, beginEnd, mode);
    }

    @Override
    public String toString() {
        return "RailRouteCallingPoints{" +
                "agencyId=" + agencyId +
                ", callingPoints=" + callingPoints +
                ", beginEnd=" + beginEnd +
                ", mode=" + mode +
                '}';
    }

    public StationIdPair getBeginEnd() {
        return beginEnd;
    }

    @Override
    public int compareTo(@NotNull final RailRouteCallingPoints other) {
        if (!other.agencyId.equals(agencyId)) {
            throw new RuntimeException("Undefined when agency is different, got " + agencyId + " and " + other.agencyId);
        }
        // enum ordering here
        final int compareMode = Integer.compare(mode.ordinal(), other.mode.ordinal());
        if (compareMode!=0) {
            return compareMode;
        }
        // longer first
        final int compareSize = Integer.compare(other.numberCallingPoints(), numberCallingPoints());
        if (compareSize!=0) {
            return compareSize;
        }
        // same size...
        return compareCallingPoints(other.callingPoints);
    }

    private int compareCallingPoints(List<IdFor<Station>> otherCallingPoints) {
        int size = callingPoints.size();
        if (size != otherCallingPoints.size()) {
            throw new RuntimeException("Must call for same number of calling points");
        }
        for (int i = 0; i < size; i++) {
            IdFor<Station> id = callingPoints.get(i);
            IdFor<Station> otherId = otherCallingPoints.get(i);

            int comparison = id.compareTo(otherId);
            if (comparison!=0) {
                return comparison;
            }
        }
        return 0;
    }

    public TransportMode getMode() {
        return mode;
    }
}
