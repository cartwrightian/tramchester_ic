package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DateRangeAndVersion;

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


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TramchesterConfig.DateFormatForJson)
    public LocalDate validFrom() {
        return validFrom;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TramchesterConfig.DateFormatForJson)
    public LocalDate validUntil() {
        return validUntil;
    }

}
