package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.DateRangeAndVersion;
import com.tramchester.mappers.serialisation.LocalDateJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalDateJsonSerializer;

import java.time.LocalDate;

@SuppressWarnings("unused")
public class DataVersionDTO {
    private String version;
    private LocalDate validFrom;
    private LocalDate validUntil;

    public DataVersionDTO() {
        // for JSON deserialisation
    }

    public DataVersionDTO(DateRangeAndVersion dateRangeAndVersion) {
        this.validFrom = dateRangeAndVersion.validFrom();
        this.validUntil = dateRangeAndVersion.validUntil();
        this.version = dateRangeAndVersion.version();
    }

    public String getVersion() {
        return version;
    }

    @JsonSerialize(using = LocalDateJsonSerializer.class)
    @JsonDeserialize(using = LocalDateJsonDeserializer.class)
    public LocalDate validFrom() {
        return validFrom;
    }

    @JsonSerialize(using = LocalDateJsonSerializer.class)
    @JsonDeserialize(using = LocalDateJsonDeserializer.class)
    public LocalDate validUntil() {
        return validUntil;
    }

}
