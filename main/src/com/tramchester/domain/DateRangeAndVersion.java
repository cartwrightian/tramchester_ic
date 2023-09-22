package com.tramchester.domain;

import java.time.LocalDate;

public interface DateRangeAndVersion {
    String version();

    LocalDate validFrom();

    LocalDate validUntil();
}
