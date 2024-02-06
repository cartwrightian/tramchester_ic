package com.tramchester.dataimport.nptg.xml;


import com.fasterxml.jackson.annotation.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName("NptgLocalities")
@JsonTypeName("NptgLocality")
public class NPTGLocalityXMLData  {

    private final NPTGDescriptor descriptor;
    private final String localityCode;
    private final NptgLocation location;
    private final String parentLocalityRef;

    @JsonCreator
    public NPTGLocalityXMLData(
            @JsonProperty("Descriptor") NPTGDescriptor descriptor,
            @JsonProperty("NptgLocalityCode") String localityCode,
            @JsonProperty("Location") NptgLocation location,
            @JsonProperty("ParentNptgLocalityRef") String parentLocalityRef) {

        this.descriptor = descriptor;
        this.localityCode = localityCode;
        this.location = location;
        this.parentLocalityRef = parentLocalityRef;
    }

    public GridPosition getGridPosition() {
        final String rawEasting = getEasting();
        final String rawNorthin = getNorthing();
        if (rawNorthin==null||rawEasting==null) {
            return GridPosition.Invalid;
        }
        if (rawNorthin.isEmpty()||rawEasting.isEmpty()) {
            return GridPosition.Invalid;
        }
        try {
            int northing = Integer.parseInt(rawNorthin);
            int easting = Integer.parseInt(rawEasting);
            return new GridPosition(easting, northing);
        }
        catch (NumberFormatException exception) {
            return GridPosition.Invalid;
        }
    }

    public String getLocalityName() {
        return descriptor.getLocalityName();
    }

    public String getNptgLocalityCode() {
        return localityCode;
    }

    public String getEasting() {
        return location.getTranslation().getEasting();
    }

    public String getNorthing() {
        return location.getTranslation().getNorthing();
    }

    public String getParentLocalityRef() {
        return parentLocalityRef;
    }

    @Override
    public String toString() {
        return "NPTGLocalityXMLData{" +
                "descriptor=" + descriptor +
                ", localityCode='" + localityCode + '\'' +
                ", location=" + location +
                ", parentLocalityRef='" + parentLocalityRef + '\'' +
                '}';
    }

    public String getLatitude() {
        return location.getTranslation().getLatitude();
    }

    public String getLongitude() {
        return location.getTranslation().getLongitude();
    }

    public LatLong getLatLong() {
        final String rawLat = getLatitude();
        final String rawLon = getLongitude();
        if (rawLat==null||rawLon==null) {
            return LatLong.Invalid;
        }
        if (rawLat.isEmpty()||rawLon.isEmpty()) {
            return LatLong.Invalid;
        }
        try {
            double lat = Double.parseDouble(rawLat);
            double lon = Double.parseDouble(rawLon);
            return new LatLong(lat, lon);
        }
        catch (NumberFormatException exception) {
            return LatLong.Invalid;
        }
    }
}
