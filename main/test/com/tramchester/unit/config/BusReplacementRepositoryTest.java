package com.tramchester.unit.config;

import com.tramchester.config.BusReplacementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class BusReplacementRepositoryTest {

    private BusReplacementRepository repository;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = new BusReplacementRepository();
        repository.start();
    }

    @Test
    void shouldHaveSomeReplacementBuses() {
        assertFalse(repository.hasReplacementBuses());
    }

//    @Test
//    void shouldHaveExpectedReplacementBus() {
//        TramDate when = TestEnv.testDay();
//        assertTrue(repository.isReplacement(KnownTramRoute.getBusEcclesToMediaCity(when).getId()));
//    }

}
