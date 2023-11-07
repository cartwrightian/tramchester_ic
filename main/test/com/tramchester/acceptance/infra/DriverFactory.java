package com.tramchester.acceptance.infra;

import com.tramchester.domain.presentation.LatLong;

import java.util.HashMap;
import java.util.Map;

public class DriverFactory {

    private final Map<String, ProvidesDriver> drivers;
    private final LatLong location;

    // Map Name -> Driver Instance
    public DriverFactory(LatLong location) {
        this.location = location;
        drivers = new HashMap<>();
    }

    public ProvidesDriver get(String browserName) {
        if (drivers.containsKey(browserName)) {
            return drivers.get(browserName);
        }
        ProvidesDriver driver = create(location, browserName);
        drivers.put(browserName, driver);
        return driver;
    }

    private ProvidesDriver create(LatLong location, String browserName) {
        return switch (browserName) {
            case ProvidesFirefoxDriver.Name -> new ProvidesFirefoxDriver(location);
            case ProvidesChromeDriver.Name -> new ProvidesChromeDriver(location);
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
