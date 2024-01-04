package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.presentation.LatLong;

import java.util.List;

public class AreaBoundaryDTO extends BoundaryDTO {
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

    public IdForDTO getAreaId() {
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
