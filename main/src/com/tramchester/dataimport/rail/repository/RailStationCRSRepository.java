package com.tramchester.dataimport.rail.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;

/***
 *  * NOTE can contain stations not in main repository if they are not 'in bounds'
 */
@LazySingleton
public class RailStationCRSRepository implements CRSRepository {
    private static final Logger logger = LoggerFactory.getLogger(RailStationCRSRepository.class);

    private final Map<IdFor<Station>, String> toCrs; // stationId -> crs code
    private final Map<String, Station> toStation; // crs code -> station

    public RailStationCRSRepository() {
        toStation = new HashMap<>();
        toCrs = new HashMap<>();
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        toCrs.clear();
        toStation.clear();
        logger.info("Stopped");
    }

    public void putCRS(final Station station, final String crs) {
        final IdFor<Station> stationId = station.getId();
        if (crs.isBlank()) {
            logger.error("Attempt to insert blank CRS for station " + station.getId());
            return;
        }
        toCrs.put(stationId, crs);
        toStation.put(crs, station);
    }

    /***
     * @param stationId the station
     * @return the crs code
     */
    public String getCRSCodeFor(final IdFor<Station> stationId) {
        return toCrs.get(stationId);
    }

    public boolean hasStation(final IdFor<Station> stationId) {
        return toCrs.containsKey(stationId);
    }

    @Override
    public Station getStationFor(final IdFor<Station> stationId) {
        final String crs = getCRSCodeFor(stationId);
        return getStationFor(crs);
    }

    @Override
    public Station getStationFor(final String crs) {
        return toStation.get(crs);
    }

    @Override
    public boolean hasCRSCode(final String crs) {
        return toStation.containsKey(crs);
    }

}
