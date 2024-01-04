package com.tramchester.testSupport.reference;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;

public class StationHelper {

    public static Station forTest(String id, String area, String stationName, LatLong latLong, DataSourceID dataSourceID, String stationCode) {
        IdFor<NPTGLocality> areaId = NPTGLocality.createId(area);
        return new MutableStation(Station.createId(id), areaId, stationName, latLong, CoordinateTransforms.getGridPosition(latLong),
                dataSourceID, stationCode);
    }

    public static MutableStation forTestMutable(String id, String area, String stationName, LatLong latLong, DataSourceID dataSourceID, String stationCode) {
        IdFor<NPTGLocality> areaId = NPTGLocality.createId(area);
        return new MutableStation(Station.createId(id), areaId, stationName, latLong, CoordinateTransforms.getGridPosition(latLong),
                dataSourceID, stationCode);
    }

    public static MutableStation forTestMutable(String id, String area, String stationName, KnownLocations knowLocation, DataSourceID dataSourceID, String stationCode) {
        IdFor<NPTGLocality> areaId = NPTGLocality.createId(area);
        return new MutableStation(Station.createId(id), areaId, stationName, knowLocation.latLong(), knowLocation.grid(),
                dataSourceID, stationCode);
    }

}
