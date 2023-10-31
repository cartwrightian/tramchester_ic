package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.presentation.LatLong;

@SuppressWarnings("unused")
public class PlatformDTO {

    private IdFor<Platform> platformId;

    private IdForDTO id;
    private String name;
    private String platformNumber;
    private LatLong latLong;
    private LocationRefDTO station;

    public PlatformDTO() {
        // for deserialisation
    }

    public PlatformDTO(Platform original) {
        platformId = original.getId();
        this.id = IdForDTO.createFor(original);
        this.name = original.getName();
        this.platformNumber = original.getPlatformNumber();
        this.latLong = original.getLatLong();
        this.station = new LocationRefDTO(original.getStation());
    }

    @JsonIgnore
    public IdFor<Platform> getPlatformId() {
        return platformId;
    }

    public IdForDTO getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPlatformNumber() {
        return platformNumber;
    }

    public LocationRefDTO getStation() {
        return station;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlatformDTO that = (PlatformDTO) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public LatLong getLatLong() {
        return latLong;
    }

    @Override
    public String toString() {
        return "PlatformDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", platformNumber='" + platformNumber + '\'' +
                ", latLong=" + latLong +
                ", station=" + station +
                '}';
    }
}
