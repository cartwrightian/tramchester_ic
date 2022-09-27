package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.ProvidesDateInput;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.testSupport.TestEnv;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.Location;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static org.openqa.selenium.chrome.ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY;
import static org.openqa.selenium.chrome.ChromeDriverService.CHROME_DRIVER_VERBOSE_LOG_PROPERTY;


// Fix binary verification issue
// https://timonweb.com/misc/fixing-error-chromedriver-cannot-be-opened-because-the-developer-cannot-be-verified-unable-to-launch-the-chrome-browser-on-mac-os/

public class ProvidesChromeDriver extends ProvidesDesktopDriver {
    public final static String Name = "chrome";
    private final DesiredCapabilities capabilities;
    private final ChromeOptions chromeOptions;
    private final boolean enableGeo;

    private final ProvidesDateInput providesDateInput;
    private LatLong location;

    public ProvidesChromeDriver(boolean enableGeo) {
        this.enableGeo = enableGeo;

        Path chromedriverPath = TestEnv.getPathFromEnv("CHROMEDRIVER_PATH");
        if (chromedriverPath!=null) {
            System.setProperty(CHROME_DRIVER_EXE_PROPERTY, chromedriverPath.toString());
        }
        System.setProperty(CHROME_DRIVER_VERBOSE_LOG_PROPERTY,"false");

        capabilities = createCapabilities();
        chromeOptions = new ChromeOptions();

        setGeoLocation(enableGeo, chromeOptions);
        if (enableGeo) {
            // geolocation fails on headless chrome, bug raised https://bugs.chromium.org/p/chromium/issues/detail?id=834808
            chromeOptions.setHeadless(false);
            // exception on set location otherwise
            chromeOptions.setExperimentalOption("w3c",false);
            //chromeOptions.setExperimentalOption("geolocation", true);
        } else {
            chromeOptions.setHeadless(true);
        }

        providesDateInput = new ProvidesChromeDateInput();
    }

    private void setGeoLocation(boolean enableGeo, ChromeOptions chromeOptions) {
        int option = enableGeo ? 1 : 2;
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("geolocation", option);  // older version of chrome

        // newer versions (>103 at least) need this
        prefs.put("googlegeolocationaccess.enabled", true);

        chromeOptions.setExperimentalOption("prefs", prefs);
    }

    @Override
    public void init() {

        if (driver == null) {

            //chromeOptions.merge(capabilities);
            capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);

            ChromeDriver chromeDriver = new ChromeDriver(chromeOptions);
            chromeDriver.setLogLevel(Level.SEVERE);

            driver = chromeDriver;

            if (location != null) {
                chromeDriver.setLocation(new Location(location.getLat(), location.getLon(), 0));
            }
        }
     }

    @Override
    public void quit() {
        if (driver!=null) {
            driver.quit();
            driver=null;
        }
    }

    @Override
    public AppPage getAppPage() {
        return new AppPage(driver, providesDateInput);
    }

    @Override
    public void setStubbedLocation(LatLong location) {
        this.location = location;
    }

    @Override
    protected String getDriverName() {
        return Name;
    }

    @Override
    public String toString() {
        return Name+"{" +
                "geo=" + enableGeo +
                '}';
    }

}
