package com.tramchester.repository.naptan;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.*;

import java.util.Set;

@ImplementedBy(NaptanRepositoryContainer.class)
public interface NaptanRepository {
    // TODO Check or diag on NaptanStopType
    <T extends Location<?>> boolean containsActo(IdFor<T> locationId);

    // TODO Check or diag on NaptanStopType
    <T extends Location<?>> NaptanRecord getForActo(IdFor<T> actoCode);

    NaptanRecord getForTiploc(IdFor<Station> railStationTiploc);

    boolean containsTiploc(IdFor<Station> tiploc);

    boolean containsArea(IdFor<NPTGLocality> localityId);

    Set<NaptanRecord> getRecordsForLocality(IdFor<NPTGLocality> localityId);

    boolean isEnabled();
}
