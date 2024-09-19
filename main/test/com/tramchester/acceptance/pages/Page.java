package com.tramchester.acceptance.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class Page {
    protected final WebDriver driver;
    private static final Duration timeOut = Duration.ofSeconds(4);

    public Page(WebDriver driver) {
        this.driver = driver;
    }

    protected WebElement waitForElement(String elementId, Duration timeout) {
        return waitForElement(By.id(elementId), timeout);
    }

    protected WebElement waitForElement(By select, Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        wait.until(webDriver ->  driver.findElement(select));
        return driver.findElement(select);
    }

    protected WebElement findElementById(String id) {
        return waitForElement(id, timeOut);
    }

    public String getExpectedBuildNumberFromEnv() {
        // prefer release number if set
        String releaseNumber = System.getenv("RELEASE_NUMBER");
        if (releaseNumber!=null) {
            return releaseNumber;
        }
        String build = System.getenv("CIRCLE_BUILD_NUM");
        if (build!=null) {
            return build;
        }
        // 0 for dev machines
        return "0";
    }

}
