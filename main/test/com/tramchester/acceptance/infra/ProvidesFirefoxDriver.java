package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.ProvidesDateInput;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProvidesFirefoxDriver extends ProvidesDesktopDriver {

    public final static String Name = "firefox";
    private final boolean enableGeo;
    private final Path locationStubJSON = Paths.get("geofile.json");

    private final DesiredCapabilities capabilities;
    private ProvidesDateInput providesDateInput;

    public ProvidesFirefoxDriver(boolean enableGeo) {

        capabilities = createCapabilities();
        this.enableGeo = enableGeo;
    }

    @Override
    public void init() {
        if (driver==null) {
            providesDateInput = new ProvidesFirefoxDateInput();
            Path firefoxPath = TestEnv.getPathFromEnv("FIREFOX_PATH");
            if (firefoxPath != null) {
                System.setProperty(FirefoxDriver.SystemProperty.BROWSER_BINARY, firefoxPath.toString());
            }
            Path geckoDriverPath = TestEnv.getPathFromEnv("GECKODRIVER_PATH");
            if (geckoDriverPath != null) {
                System.setProperty("webdriver.gecko.driver", geckoDriverPath.toString());
            }

            if (!enableGeo) {
                FirefoxProfile geoDisabled = new FirefoxProfile();
                geoDisabled.setPreference("geo.enabled", false);
                geoDisabled.setPreference("geo.provider.use_corelocation", false);
                geoDisabled.setPreference("geo.prompt.testing", false);
                geoDisabled.setPreference("geo.prompt.testing.allow", false);

                capabilities.setCapability(FirefoxDriver.PROFILE, geoDisabled);
            }

            FirefoxOptions firefoxOptions = new FirefoxOptions(capabilities);

            // allow disabling of headless more via env var
            if (System.getenv(TestEnv.DISABLE_HEADLESS_ENV_VAR)==null) {
                firefoxOptions.setHeadless(true);
            }

            driver = new FirefoxDriver(firefoxOptions);
        }
    }

    @Override
    public AppPage getAppPage() {
        return new AppPage(driver, providesDateInput);
    }

    @Override
    public void setStubbedLocation(LatLong location) throws IOException {

        createGeoFile(location);

        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("geo.prompt.testing", true);
        profile.setPreference("geo.prompt.testing.allow", true);
        String locationURL = "file://" + locationStubJSON.toAbsolutePath();
        profile.setPreference("geo.wifi.uri", locationURL); // OLD
        profile.setPreference("geo.provider.network.url", locationURL); // NEW
        capabilities.setBrowserName("firefox");

        capabilities.setCapability(FirefoxDriver.PROFILE, profile);
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
                "geo=" + enableGeo +
                '}';
    }
}
