package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;

@SuppressWarnings("unused")
public class AgencyData {

    // todo agency_noc?

    @JsonProperty("agency_id")
    private String id;

    @JsonProperty("agency_name")
    private String name;

    public AgencyData() {
        // deserialisation
    }

    public String getName() {
        return name;
    }

    public IdFor<Agency> getId() {
        return Agency.createId(id);
    }
}
