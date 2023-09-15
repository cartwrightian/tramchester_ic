package com.tramchester.domain;

import java.time.LocalDate;

public interface DateRangeAndVersion {
    String getVersion();

    LocalDate validFrom();

    LocalDate validUntil();
}
