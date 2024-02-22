package com.tramchester.geo;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

@ImplementedBy(StationLocations.class)
public interface StationLocationsRepository {

    List<Station> nearestStationsSorted(Location<?> location, int maxToFind, MarginInMeters rangeInMeters, EnumSet<TransportMode> modes);

    Stream<Station> nearestStationsUnsorted(Station station, MarginInMeters rangeInMeters);

    BoundingBox getActiveStationBounds();

    LocationCollection getLocationsWithin(IdFor<NPTGLocality> areaId);

    boolean hasStationsOrPlatformsIn(IdFor<NPTGLocality> areaId);

    boolean withinBounds(Location<?> location);
}
