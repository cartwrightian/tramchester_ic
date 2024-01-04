package com.tramchester.dataimport.nptg;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.geo.GridPosition;


 // National Public Transport Gazetteer
 //
 // Cross-referenced by naptan data via the nptgLocalityCode
 //
public class NPTGData {

    public static final String UK_Ord_Survey = "UKOS";
    @JsonProperty("NptgLocalityCode")
    private String nptgLocalityCode;

    @JsonProperty("LocalityName")
    private String localityName;

    @JsonProperty("ParentLocalityName")
    private String parentLocalityName;

    @JsonProperty("GridType")
    private String gridType; // one of UKOS, IrelandOS, ITM

    @JsonProperty("Easting")
    private String easting;

    @JsonProperty("Northing")
    private String northing;

    public NPTGData() {
        // deserialisation
    }

    public String getNptgLocalityCode() {
        return nptgLocalityCode;
    }

    public String getLocalityName() {
        return localityName;
    }

     @Override
     public String toString() {
         return "NPTGData{" +
                 "nptgLocalityCode='" + nptgLocalityCode + '\'' +
                 ", localityName='" + localityName + '\'' +
                 ", parentLocalityName='" + parentLocalityName + '\'' +
                 ", gridType='" + gridType + '\'' +
                 ", easting='" + easting + '\'' +
                 ", northing='" + northing + '\'' +
                 '}';
     }

     public String getParentLocalityName() {
        return parentLocalityName;
    }

    public GridPosition getGridPosition() {
        if (gridType.isEmpty() || gridType.equals(UK_Ord_Survey)) {
            try {
                long eastingNumber = Long.parseLong(easting);
                long northingNumber = Long.parseLong(northing);
                return new GridPosition(eastingNumber, northingNumber);
            }
            catch (NumberFormatException exception) {
                return GridPosition.Invalid;
            }
        } else {
            return GridPosition.Invalid;
        }

    }
 }
