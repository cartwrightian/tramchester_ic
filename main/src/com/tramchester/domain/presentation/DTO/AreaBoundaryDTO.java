package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.presentation.LatLong;

import java.util.List;

public class AreaBoundaryDTO extends BoundaryDTO implements HasIdForDTO {
    private IdForDTO areaId;
    private String areaName;

    public AreaBoundaryDTO(List<LatLong> points, NPTGLocality locality) {
        super(points);
        this.areaId = IdForDTO.createFor(locality);
        this.areaName = locality.getLocalityName();
    }

    public AreaBoundaryDTO() {
        // for deserialisation
    }

    /***
     * Use GetId
     * @return area id
     */
    @Deprecated
    public IdForDTO getAreaId() {
        return areaId;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public IdForDTO getId() {
        return areaId;
    }

    public String getAreaName() {
        return areaName;
    }

    @Override
    public String toString() {
        return "AreaBoundaryDTO{" +
                "areaId=" + areaId +
                ", areaName='" + areaName + '\'' +
                "} " + super.toString();
    }

}
