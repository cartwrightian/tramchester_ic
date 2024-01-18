package com.tramchester.repository.naptan;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@ImplementedBy(NaptanRepositoryContainer.class)
public interface NaptanRepository {
    // TODO Check or diag on NaptanStopType
    <T extends Location<?>> boolean containsActo(IdFor<T> locationId);

    // TODO Check or diag on NaptanStopType
    <T extends Location<?>> NaptanRecord getForActo(IdFor<T> actoCode);

    NaptanRecord getForTiploc(IdFor<Station> railStationTiploc);

    boolean containsTiploc(IdFor<Station> tiploc);

    boolean containsLocality(IdFor<NPTGLocality> localityId);

    Set<NaptanRecord> getRecordsForLocality(IdFor<NPTGLocality> localityId);

    List<LatLong> getBoundaryFor(IdFor<NPTGLocality> localityId);

    boolean isEnabled();

    Stream<NaptanRecord> getAll();
}
