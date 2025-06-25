package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.App.AppPage;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public abstract class ProvidesDriver {
    private static final Logger logger = LoggerFactory.getLogger(ProvidesDriver.class);

    public abstract void init() throws IOException;

    public abstract AppPage getAppPage();

    protected abstract String getDriverName();

    private static final Path screenshotsDir = Path.of("build/reports/tests/screenshots/");

    protected void takeScreenShot(final String testName) {
        TakesScreenshot driver = getDriver();
        if (driver==null) {
            return;
        }
        try {
            byte[] bytes = driver.getScreenshotAs(OutputType.BYTES);

            if (!Files.exists(screenshotsDir)) {
                Files.createDirectories(screenshotsDir);
            }
            File target = screenshotsDir.resolve(safeFilename(testName)+".png").toFile();
            FileOutputStream output = new FileOutputStream(target);
            output.write(bytes);
            output.close();
        } catch (IOException | TimeoutException ioException) {
            logger.warn("Failed screenshot", ioException);
        }
    }

    private String safeFilename(final String testName) {
        int endOfTestName = testName.indexOf("(");
        return testName.substring(0, endOfTestName) + "_" + getDriverName();
    }

    protected abstract RemoteWebDriver getDriver();

    public void moveTo(WebElement webElement){
        new Actions(getDriver()).moveToElement(webElement).perform();
    }

    public void click(WebElement webElement) {
        webElement.click();
    }

    public abstract void quit();

    public void close() {
        RemoteWebDriver driver = getDriver();
        if (driver!=null) {
            driver.close();
        }
     }

    public void clearCookies() {
        getDriver().manage().deleteAllCookies();
        Set<Cookie> cookies = getDriver().manage().getCookies();
        if (!cookies.isEmpty()) {
            throw new RuntimeException("Cookies still present " + cookies);
        }
    }

}
