package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.ProvidesDateInput;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.logging.Level;

public class ProvidesFirefoxDriver extends ProvidesDesktopDriver {

    public final static String Name = "firefox";
    private final LatLong location;
    private final Path locationStubJSON = Paths.get("geofile.json");

    private ProvidesDateInput providesDateInput;
    private final Duration timeout;

    public ProvidesFirefoxDriver(LatLong location, Duration timeout) {
        this.location = location;
        this.timeout = timeout;
    }

    @Override
    public void init() throws IOException {
        if (driver==null) {
            providesDateInput = new ProvidesFirefoxDateInput();
            final Path firefoxPath = TestEnv.getPathFromEnv("FIREFOX_PATH");
            if (firefoxPath != null) {
                System.setProperty(FirefoxDriver.SystemProperty.BROWSER_BINARY, firefoxPath.toString());
            }
            final Path geckoDriverPath = TestEnv.getPathFromEnv("GECKODRIVER_PATH");
            if (geckoDriverPath != null) {
                System.setProperty("webdriver.gecko.driver", geckoDriverPath.toString());
            }

            final FirefoxProfile firefoxProfile = new FirefoxProfile();

            // This does not seem to work at all when it comes time input, only setting of LANG seems to matter
            // https://firefox-source-docs.mozilla.org/intl/locale.html
//            firefoxProfile.setPreference("intl.locale.requested", "en-GB");
//            firefoxProfile.setPreference("intl.accept_languages", "en-GB");

            if (location.isValid()) {
                createGeoFile(location);
                firefoxProfile.setPreference("geo.prompt.testing", true);
                firefoxProfile.setPreference("geo.prompt.testing.allow", true);
                String locationURL = "file://" + locationStubJSON.toAbsolutePath();
                firefoxProfile.setPreference("geo.wifi.uri", locationURL); // OLD
                firefoxProfile.setPreference("geo.provider.network.url", locationURL); // NEW
            }
            else {
                firefoxProfile.setPreference("geo.enabled", false);
                firefoxProfile.setPreference("geo.provider.use_corelocation", false);
                firefoxProfile.setPreference("geo.prompt.testing", false);
                firefoxProfile.setPreference("geo.prompt.testing.allow", false);
            }

            final FirefoxOptions firefoxOptions = new FirefoxOptions();
            firefoxOptions.setProfile(firefoxProfile);

            // allow disabling of headless more via env var
            if (System.getenv(TestEnv.DISABLE_HEADLESS_ENV_VAR)==null) {
                firefoxOptions.addArguments("-headless");
            }

            final FirefoxDriver firefoxDriver = new FirefoxDriver(firefoxOptions);
            firefoxDriver.setLogLevel(Level.SEVERE);

            driver = firefoxDriver;
        }
    }

    @Override
    public AppPage getAppPage() {
        return new AppPage(driver, providesDateInput, timeout);
    }

    @Override
    protected String getDriverName() {
        return Name;
    }

    private void createGeoFile(LatLong place) throws IOException {
        Files.deleteIfExists(locationStubJSON);

        String json = "{\n" +
                "    \"status\": \"OK\",\n" +
                "    \"accuracy\": 10.0,\n" +
                "    \"location\": {\n" +
                "        \"lat\": " +place.getLat() + ",\n" +
                "        \"lng\": " +place.getLon()+"\n" +
                "     }\n" +
                "}";

        try {
            FileUtils.writeStringToFile(locationStubJSON.toFile(), json, Charset.defaultCharset());
        } catch (IOException e) {
            // this is asserted later
        }
    }

    // element click() unrealible on geckodriver.....
    @Override
    public void click(WebElement webElement) {
        JavascriptExecutor executor = (JavascriptExecutor)driver;
        executor.executeScript("arguments[0].click();", webElement);
    }

    @Override
    public void quit() {
        try {
            if (driver != null) {
                driver.quit();
                driver = null;
            }
        }
        catch (NoSuchSessionException quitMustHaveAlreadyClosedTheSession) {
            driver=null;
        }
    }

    @Override
    public String toString() {
        return Name+"{" +
                "location=" + location +
                '}';
    }
}
