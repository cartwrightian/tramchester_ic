package com.tramchester.dataimport.rail.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;

/***
 * Supports live rail data which uses the CRS id, not tiploc
 * NOTE can contain stations not in main repository if they are not 'in bounds'
 */
@ImplementedBy(RailStationCRSRepository.class)
public interface CRSRepository {
    String getCRSFor(IdFor<Station> station);
    boolean hasStation(IdFor<Station> station);

    Station getFor(String crs);
    boolean hasCrs(String crs);
}
