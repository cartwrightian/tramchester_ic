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
    private final String commonName;
    private final GridPosition gridPosition;
    private final String suburb;
    private final String town;
    private final String street;
    private final String indicator;
    private final NaptanStopType stopType;
    private final boolean localityCenter;
    private final LatLong latlong;

    public NaptanRecord(IdFor<NaptanRecord> id, IdFor<NPTGLocality> localityId, String commonName, GridPosition gridPosition,
                        LatLong latlong, String suburb, String town,
                        NaptanStopType stopType, String street, String indicator, boolean localityCenter) {
        this.id = id;
        this.localityId = localityId;
        this.commonName = commonName;
        this.gridPosition = gridPosition;
        this.latlong = latlong;
        this.suburb = suburb;
        this.town = town;
        this.stopType = stopType;
        this.street = street;
        this.indicator = indicator;
        this.localityCenter = localityCenter;
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

    public String getCommonName() {
        return commonName;
    }

    public String getTown() { return town; }

    public NaptanStopType getStopType() {
        return stopType;
    }

    public GridPosition getGridPosition() {
        return gridPosition;
    }

    public LatLong getLatLong() {
        return latlong;
    }

    public boolean isLocalityCenter() {
        return localityCenter;
    }

    public String getDisplayName() {
        String result = commonName;
        if (stopType==NaptanStopType.busCoachTrolleyStationBay) {
            if (!indicator.isEmpty()) {
                result = result + " (" + indicator + ")";
            }
        } else {
            // TODO landmark

            if (!street.isEmpty()) {
                result = result + " (";
                if (!indicator.isEmpty()) {
                    result = result + indicator + " ";
                }
                result = result + street + ")";
            }
        }

        // add suburb and town if present
        if (!suburb.isEmpty()) {
            result = result + ", " + suburb;
        }
        if (!town.isEmpty()) {
            result = result + ", " +town;
        }
        return result;
    }

    @Override
    public String toString() {
        return "NaptanRecord{" +
                "id=" + id +
                ", localityId=" + localityId +
                ", commonName='" + commonName + '\'' +
                ", gridPosition=" + gridPosition +
                ", suburb='" + suburb + '\'' +
                ", town='" + town + '\'' +
                ", stopType=" + stopType +
                ", localityCenter=" + localityCenter +
                ", latlong=" + latlong +
                '}';
    }

    public IdFor<NPTGLocality> getLocalityId() {
        return localityId;
    }

    public String getStreet() {
        return street;
    }

    public String getIndicator() {
        return indicator;
    }
}
