package com.tramchester.unit.config;

import com.tramchester.config.BusReplacementRepository;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BusReplacementRepositoryTest {

    private BusReplacementRepository repositry;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repositry = new BusReplacementRepository();
        repositry.start();
    }

    @Test
    void shouldHaveSomeReplacementBuses() {
        assertTrue(repositry.hasReplacementBuses());
    }

    @Test
    void shouldHaveExpectedReplacementBus() {
        assertTrue(repositry.isReplacement(KnownTramRoute.BusEcclesToMediaCity.getId()));
    }

}
