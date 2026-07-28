package com.tramchester.domain.places;

import com.tramchester.domain.LocationSet;
import com.tramchester.domain.StationGroup;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/***
 * Stations grouped together as in same nptg locality, use as starting or end point for a journey
 * see also class: com.tramchester.graph.GraphQuery::getGroupedNode
 */
public class StationLocalityGroup extends StationGroup {

    private final IdFor<StationGroup> parentId;

    public StationLocalityGroup(final Set<Station> groupedStations, final IdFor<NPTGLocality> localityId, final String name,
                                final IdFor<NPTGLocality> parentId, final LatLong latLong) {
        super(LocationSet.of(groupedStations), name, latLong, localityId);
        if (groupedStations.isEmpty()) {
            throw new RuntimeException("Attempt to create empty group for " + localityId + " name name " +name);
        }
        if (parentId.isValid()) {
            this.parentId = createId(parentId);
        } else {
            this.parentId = StringIdFor.invalid(StationGroup.class);
        }
    }

    @NotNull
    public static IdFor<StationGroup> createId(final IdFor<NPTGLocality> localityId) {
        return StringIdFor.convert(localityId, StationGroup.class);
    }

    public static IdFor<StationGroup> createId(String text) {
        return StringIdFor.createId(text, StationGroup.class);
    }


    public IdFor<StationGroup> getParentId() {
        return parentId;
    }

    public boolean hasParent() {
        return parentId.isValid();
    }

    @Override
    public String toString() {
        return "StationLocalityGroup{" +
                "parentId=" + parentId +
                "} " + super.toString();
    }

}
