package com.tramchester.acceptance.infra;

import com.tramchester.domain.presentation.LatLong;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DriverFactory {

    private final Map<String, ProvidesDriver> drivers;
    private final LatLong location;
    private final Duration timeout;

    public DriverFactory(LatLong location, Duration timeout) {
        this.location = location;
        this.timeout = timeout;
        drivers = new HashMap<>();
    }

    public ProvidesDriver get(final String browserName) {
        if (drivers.containsKey(browserName)) {
            return drivers.get(browserName);
        }
        final ProvidesDriver driver = create(location, browserName);
        drivers.put(browserName, driver);
        return driver;
    }

    private ProvidesDriver create(final LatLong location, final String browserName) {
        return switch (browserName) {
            case ProvidesFirefoxDriver.Name -> new ProvidesFirefoxDriver(location, timeout);
            case ProvidesChromeDriver.Name -> new ProvidesChromeDriver(location, timeout);
            default -> throw new RuntimeException("Unknown browser " + browserName);
        };
    }

    public void close() {
        drivers.values().forEach(ProvidesDriver::close);
    }

    public void quit() {
        drivers.values().forEach(ProvidesDriver::quit);
        drivers.clear();
    }

    public void takeScreenshotFor(String name, String testName) {
        drivers.get(name).takeScreenShot(testName);
    }

    public boolean isGeoEnabled() {
        return location.isValid();
    }
}
