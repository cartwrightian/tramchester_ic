package com.tramchester.acceptance.infra;


import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.logging.Level;

public abstract class ProvidesDesktopDriver extends ProvidesDriver {

    protected WebDriver driver = null;

    @Deprecated
    protected DesiredCapabilities createCapabilities() {
        DesiredCapabilities caps = new DesiredCapabilities();

        LoggingPreferences loggingPrefs = new LoggingPreferences();

        loggingPrefs.enable(LogType.BROWSER, Level.SEVERE);
        loggingPrefs.enable(LogType.DRIVER, Level.SEVERE);

//        caps.setCapability(CapabilityType.LOGGING_PREFS, loggingPrefs);

        return caps;
    }

    @Override
    protected RemoteWebDriver getDriver() {
        return (RemoteWebDriver) driver;
    }


}
