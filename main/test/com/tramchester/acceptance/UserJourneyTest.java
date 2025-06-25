package com.tramchester.acceptance;

import com.tramchester.acceptance.infra.DriverFactory;
import com.tramchester.acceptance.infra.ProvidesChromeDriver;
import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.acceptance.infra.ProvidesFirefoxDriver;
import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserJourneyTest {
    private static DriverFactory driverFactory;

    protected static void createFactory(LatLong location) {
        if (driverFactory!=null) {
            throw new RuntimeException("Factory already created");
        }
        final Duration timeout = TestEnv.isCircleci() ? Duration.ofSeconds(15) : Duration.ofSeconds(5);
        driverFactory = new DriverFactory(location, timeout);
    }

    protected static void closeFactory() {
        driverFactory.close();
        driverFactory.quit();
        driverFactory = null;
    }

    protected AppPage prepare(final ProvidesDriver providesDriver, final String url) throws IOException {
        providesDriver.init();
        providesDriver.clearCookies();

        final AppPage appPage = providesDriver.getAppPage();
        appPage.load(url);

        assertTrue(appPage.waitForCookieAgreementVisible(), "cookie agreement visible");
        appPage.agreeToCookies();
        assertTrue(appPage.waitForCookieAgreementInvisible(), "wait for cookie agreement to close");
        assertTrue(appPage.waitForReady(), "app ready");
        assertTrue(appPage.waitForLocationSelectionsAvailable(), "stops appeared");

        return appPage;
    }

    public static Stream<ProvidesDriver> getProviderCommon() {

        // Headless Chrome on CI BOX is ignoring locale which breaks many acceptance tests
        // https://bugs.chromium.org/p/chromium/issues/detail?id=755338

        List<String> names = new ArrayList<>();
        names.add("firefox");
        // chromedriver install is broken on circle ci 7/05/2024
        if (!(driverFactory.isGeoEnabled() || TestEnv.isCircleci())) {
            names.add("chrome");
        }
        return names.stream().map(browserName -> driverFactory.get(browserName));
    }

    protected void takeScreenshotsFor(TestInfo testInfo) {
        String displayName = testInfo.getDisplayName();

        if (displayName.contains(ProvidesFirefoxDriver.Name)) {
            driverFactory.takeScreenshotFor(ProvidesFirefoxDriver.Name, displayName);
        } else if (displayName.contains(ProvidesChromeDriver.Name)) {
            driverFactory.takeScreenshotFor(ProvidesChromeDriver.Name, displayName);
        }
    }

}
