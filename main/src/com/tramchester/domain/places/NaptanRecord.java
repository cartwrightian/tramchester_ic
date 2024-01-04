package com.tramchester.domain.places;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanStopType;

public class NaptanRecord implements HasId<NaptanRecord>, CoreDomain {
    private final IdFor<NaptanRecord> id; // actoCode
    private final IdFor<NPTGLocality> localityId;
    private final String name;
    private final GridPosition gridPosition;
    private final String suburb;
    private final String town;
    private final NaptanStopType stopType;
    private final LatLong latlong;

    public NaptanRecord(IdFor<NaptanRecord> id, IdFor<NPTGLocality> localityId, String name, GridPosition gridPosition,
                        LatLong latlong, String suburb, String town,
                        NaptanStopType stopType) {
        this.id = id;
        this.localityId = localityId;
        this.name = name;
        this.gridPosition = gridPosition;
        this.latlong = latlong;
        this.suburb = suburb;
        this.town = town;
        this.stopType = stopType;
    }

    public static IdFor<NaptanRecord> createId(String text) {
        return StringIdFor.createId(text, NaptanRecord.class);
    }

    @Override
    public IdFor<NaptanRecord> getId() {
        return id;
    }

    public String getSuburb() {
        return suburb;
    }

    public String getName() {
        return name;
    }

    public NaptanStopType getStopType() {
        return stopType;
    }

    public GridPosition getGridPosition() {
        return gridPosition;
    }

    public LatLong getLatLong() {
        return latlong;
    }

    @Override
    public String toString() {
        return "NaptanRecord{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gridPosition=" + gridPosition +
                ", suburb='" + suburb + '\'' +
                ", town='" + town + '\'' +
                ", stopType=" + stopType +
                '}';
    }

//    @Deprecated
//    public void setAreaCodes(List<String> stopAreaCodes) {
//        this.stopAreaCodes = stopAreaCodes;
//    }

    public IdFor<NPTGLocality> getLocalityId() {
        return localityId;
    }
}
