package com.tramchester.domain.places;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.caching.CachableData;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanStopType;

public class NaptanRecord implements HasId<NaptanRecord>, CoreDomain, CachableData {
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
    private final LatLong latLong;
    private final IdFor<Station> railStationId;

    public NaptanRecord(@JsonProperty("id") IdFor<NaptanRecord> id,
                        @JsonProperty("localityId") IdFor<NPTGLocality> localityId,
                        @JsonProperty("commonName") String commonName,
                        @JsonProperty("gridPosition") GridPosition gridPosition,
                        @JsonProperty("latLong") LatLong latLong,
                        @JsonProperty("suburb") String suburb,
                        @JsonProperty("town") String town,
                        @JsonProperty("stopType") NaptanStopType stopType,
                        @JsonProperty("street") String street,
                        @JsonProperty("indicator") String indicator,
                        @JsonProperty("localityCenter") boolean localityCenter,
                        @JsonProperty(value = "railStationId") IdFor<Station> railStationId) {
        this.id = id;
        this.localityId = localityId;
        this.commonName = commonName;
        this.gridPosition = gridPosition;
        this.latLong = latLong;
        this.suburb = suburb;
        this.town = town;
        this.stopType = stopType;
        this.street = street;
        this.indicator = indicator;
        this.localityCenter = localityCenter;
        if (railStationId==null) {
            this.railStationId = Station.InvalidId();
        } else {
            this.railStationId = railStationId;
        }
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
        return latLong;
    }

    public boolean isLocalityCenter() {
        return localityCenter;
    }

    @JsonIgnore
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
                ", street='" + street + '\'' +
                ", indicator='" + indicator + '\'' +
                ", stopType=" + stopType +
                ", localityCenter=" + localityCenter +
                ", latLong=" + latLong +
                ", railStationId=" + railStationId +
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

    @JsonIgnore
    public IdFor<Station> getRailStationId() {
        return railStationId;
    }

    @JsonProperty("railStationId")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private IdFor<Station> getRailStationIdForSerialization() {
        if (railStationId.isValid()) {
            return railStationId;
        }
        return null;
    }
}
