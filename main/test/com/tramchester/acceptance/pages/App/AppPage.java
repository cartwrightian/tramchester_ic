package com.tramchester.acceptance.pages.App;

import com.tramchester.acceptance.pages.Page;
import com.tramchester.acceptance.pages.ProvidesDateInput;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.openqa.selenium.support.ui.ExpectedConditions.*;

public class AppPage extends Page {
    private static final String MODAL_COOKIE_CONSENT = "modal-cookieConsent";
    private static final String MODAL_DISCLAIMER = "modaldisclaimer";
    private static final String PLAN = "plan";

    private static final String START_GROUP = "startGroup";
    private static final String DESTINATION_GROUP = "destinationGroup";
    public static final String DISCLAIMER_AGREE_BUTTON = "disclaimerAgreeButton";
    public static final String COOKIE_AGREE_BUTTON = "cookieAgreeButton";

    private final ProvidesDateInput providesDateInput;
    private final Duration timeoutInSeconds;

    private static final String DATE_OUTPUT = "date";
    public static final String FROM_STOP = "startStop";
    private static final String TO_STOP = "destinationStop";
    private static final String TIME = "time";
    private static final String RESULTS = "results";

    public AppPage(WebDriver driver, ProvidesDateInput providesDateInput, Duration timeoutInSeconds) {
        super(driver);
        this.providesDateInput = providesDateInput;
        this.timeoutInSeconds = timeoutInSeconds;
    }

    public void load(String url) {
        driver.get(url);
    }

    public boolean waitForLocationSelectionsAvailable() {
        return waitForStops(FROM_STOP) && waitForStops(TO_STOP);
    }

    public boolean waitForStops(String stopId) {
        WebElement fromStopElement = createWait().until(presenceOfElementLocated(By.id(stopId)));

        createWait().until(presenceOfNestedElementLocatedBy(fromStopElement, By.className("stop")));

        return fromStopElement.isEnabled() && fromStopElement.isDisplayed();
    }

    public void planAJourney() {
        findAndClickElement(PLAN);
    }

    private void findAndClickElement(String elementId) {
        WebElement webElement = driver.findElement(By.id(elementId));

        Actions actions = moveToElement(webElement);
        actions.click().perform();
    }

    @NotNull
    private Actions moveToElement(WebElement webElement) {
        // sigh
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", webElement);
        Actions actions = new Actions(driver);
        actions.moveToElement(webElement).perform();
        return actions;
    }

    public void earlier() {
        findAndClickElement("earlierButton");
    }

    public void later() {
        findAndClickElement("laterButton");
    }

    public boolean searchEnabled() {
        WebElement planButton = findElementById(PLAN);
        createWait().until(elementToBeClickable(planButton));
        return planButton.isEnabled();
    }

    public void setStart(TramStations start) {
        setSelector(FROM_STOP, start);
    }

    public void setStartToMyLocation() {
        setSelectorToMyLocation(FROM_STOP);
    }

    public void setDest(TramStations destination) {
        setSelector(TO_STOP, destination);
    }

    private void setSelector(String selectorId, TramStations station) {
        WebElement dropDown = findElementById(selectorId); //driver.findElement(By.id(id));
        moveToElement(dropDown);
        createWait().until(elementToBeClickable(dropDown));

        // just this works for chrome but not firefox
        dropDown.sendKeys(station.getName());

    }

    private void setSelectorToMyLocation(String selectorId) {
        WebElement element = findElementById(selectorId); //driver.findElement(By.id(id));
        moveToElement(element);
        createWait().until(elementToBeClickable(element));

        Select selector = new Select(element);

        // TODO On chrome this seems to fail sometime, the element is selected but events don't fire as normal
        selector.selectByVisibleText("My Location");

    }

    public void setSpecificDate(LocalDate targetDate) {

        WebElement dateElement = findElementById("date");
        String keys = providesDateInput.createDateInput(targetDate);
        dateElement.sendKeys(keys);
    }

    public void setTime(final TramTime time) {
        WebElement element = getTimeElement();

        String input = providesDateInput.createTimeFormat(time.asLocalTime());

        element.sendKeys(input);

    }

    private WebElement getTimeElement() {
        waitForElement(TIME, timeoutInSeconds);
        return findElementById(TIME);
    }

    private WebDriverWait createWait() {
        return new WebDriverWait(driver, timeoutInSeconds);
    }

    private WebElement getDateElementOutput() {
        waitForElement(DATE_OUTPUT, timeoutInSeconds);
        return findElementById(DATE_OUTPUT);
    }

    public String getFromStop() {
        Select selector = new Select(findElementById(FROM_STOP));
        return selector.getFirstSelectedOption().getText().trim();
    }

    public String getToStop() {
        Select selector = new Select(driver.findElement(By.id(TO_STOP)));
        return selector.getFirstSelectedOption().getText().trim();
    }

    public String getTime() {
        return getTimeElement().getDomProperty("value");
    }

    public LocalDate getDate() {
        final WebElement dateElementOutput = getDateElementOutput();
        final String rawDate = dateElementOutput.getDomProperty("value");
        if (rawDate==null) {
           throw new org.openqa.selenium.NoSuchElementException("Failed to find value for " + dateElementOutput);
        }
        return LocalDate.parse(rawDate);
    }

    public boolean resultsClickable() {
        By locateResults = By.id((RESULTS));
        createWait().until(webDriver -> elementToBeClickable(locateResults).apply(webDriver));
        return true;
    }

    public List<TestResultSummaryRow> getResults() {
        final List<TestResultSummaryRow> results = new ArrayList<>();
        By resultsById = By.id(RESULTS);
        WebElement resultsDiv = new WebDriverWait(driver, timeoutInSeconds).
                until(elementToBeClickable(resultsById));

        WebElement tableBody = resultsDiv.findElement(By.tagName("tbody"));
        List<WebElement> rows = tableBody.findElements(By.tagName("tr")); //By.className("journeySummary"));
        rows.forEach(row -> results.add(new TestResultSummaryRow(row, tableBody)));

        return results;
    }

    public List<String> getRecentFromStops() {
        try {
            return getEnabledStopsByGroupName(START_GROUP + "Recent", "stop");
        }
        catch (TimeoutException notFound) {
            return new ArrayList<>();
        }
    }

    public List<String> getRecentToStops() {
        try {
            return getEnabledStopsByGroupName(DESTINATION_GROUP + "Recent", "stop");
        }
        catch (TimeoutException notFound) {
            return new ArrayList<>();
        }
    }

    public List<String> getAllStopsFromStops() {
        return getEnabledStopsByGroupName(START_GROUP + "AllStops", "stop");
    }

    public List<String> getAllStopsToStops() {
        return getEnabledStopsByGroupName(DESTINATION_GROUP + "AllStops", "stop");
    }

    public List<String> getNearestFromStops() {
        return getEnabledStopsByGroupName(START_GROUP + "NearestStops", "stop");
    }

    public List<String> getNearbyToStops() {
        return getEnabledStopsByGroupName(DESTINATION_GROUP + "Nearby", "MyLocation");
    }

    public List<String> getNearbyFromStops() {
        try {
            return getEnabledStopsByGroupName(START_GROUP + "Nearby", "MyLocation");
        }
        catch (TimeoutException notFound) {
            return new ArrayList<>();
        }
    }

    public List<String> getToStops() {
        By toStops = By.id(TO_STOP);
        WebElement elements = waitForClickableLocator(toStops);
        return getEnabledStopNames(elements, "stop");
    }

    private List<String> getEnabledStopNames(WebElement groupElement, final String className) {
        final By childLocator = By.className(className);
        createWait().until(ExpectedConditions.presenceOfNestedElementLocatedBy(groupElement, childLocator));
        final List<WebElement> stopElements = groupElement.findElements(childLocator);
        return stopElements.stream().
                filter(WebElement::isEnabled).
                map(WebElement::getText).
                map(String::trim).toList();
    }

    public boolean noResults() {
        try {
            waitForElement("noResults", timeoutInSeconds);
            return true;
        }
        catch (TimeoutException notFound) {
            return false;
        }
    }

    public void waitForClickable(WebElement element) {
        createWait().until(webDriver -> elementToBeClickable(element));
    }

    private WebElement waitForClickableLocator(By selector) {
        return createWait().until(webDriver -> elementToBeClickable(selector).apply(webDriver));
    }

    public boolean notesPresent() {
        return waitForCondition(presenceOfElementLocated(By.id("NotesList")));
    }

    public boolean hasWeekendMessage() {
        return waitForCondition(presenceOfElementLocated(By.id("Weekend")));
    }

    public boolean noWeekendMessage() {
        return waitForCondition(not(presenceOfElementLocated(By.id("Weekend"))));
    }

    public String getBuild() {
        return waitForAndGet(By.id("buildNumber"));
    }

    public String getVersion() {
        return waitForAndGet(By.id("dataVersion"));
    }

    private String waitForAndGet(By locator) {
        createWait().until(webDriver -> presenceOfElementLocated(locator));
        return driver.findElement(locator).getText();
    }

    public void displayDisclaimer() {
        WebDriverWait wait = createWait();
        By disclaimerButtonId = By.id("disclaimerButton");
        wait.until(driver -> presenceOfElementLocated(disclaimerButtonId));

        WebElement disclaimerButton = driver.findElement(disclaimerButtonId);
        moveToElement(disclaimerButton).click().perform();
    }

    public boolean waitForDisclaimerVisible() {
        return waitForModalToOpen(By.id(MODAL_DISCLAIMER), DISCLAIMER_AGREE_BUTTON);
    }

    public boolean waitForDisclaimerInvisible() {
        return waitForModalToClose(By.id(MODAL_DISCLAIMER));
    }

    public boolean waitForCookieAgreementVisible() {
        return waitForModalToOpen(By.id(MODAL_COOKIE_CONSENT), COOKIE_AGREE_BUTTON);
    }

    public boolean waitForCookieAgreementInvisible() {
        return waitForModalToClose(By.id(MODAL_COOKIE_CONSENT));
    }

    public void dismissDisclaimer() {
        okToModal(By.id(MODAL_DISCLAIMER), DISCLAIMER_AGREE_BUTTON);
    }

    public void agreeToCookies() {
        okToModal(By.id(MODAL_COOKIE_CONSENT), COOKIE_AGREE_BUTTON);
    }

    private boolean waitForModalToOpen(final By byId, final String buttonName) {
        createWait().until(webDriver -> elementToBeClickable(byId).apply(webDriver));
        WebElement diag = driver.findElement(byId);
        WebElement button = diag.findElement(By.id(buttonName));
        return waitForCondition(elementToBeClickable(button));
    }

    private boolean waitForModalToClose(By byId) {
        List<WebElement> elements = driver.findElements(byId);
        if (elements.isEmpty()) {
            return true;
        }
        if (elements.size()!=1) {
            throw new RuntimeException("UNexpected number of elements " + elements);
        }
        WebElement diag = elements.getFirst();
        return createWait().until(ExpectedConditions.invisibilityOf(diag));

    }

    private boolean waitForCondition(ExpectedCondition<?> expectedCondition) {
        try {
            createWait().until(webDriver -> expectedCondition);
            return true;
        } catch (TimeoutException notShown) {
            return false;
        }
    }

    private void okToModal(By locator, String buttonId) {
        WebElement modelDialogue = driver.findElement(locator);
        WebElement button = modelDialogue.findElement(By.id(buttonId));
        createWait().until(webDriver -> button.isDisplayed());
        createWait().until(webDriver -> button.isEnabled());
        moveToElement(button).click().perform();
        if (modelDialogue.isDisplayed()) {
            //createWait().until(webDriver -> !modelDialogue.isEnabled());
            createWait().until(ExpectedConditions.invisibilityOf(modelDialogue));
        }
    }

    public void selectNow() {
        WebElement nowButton = driver.findElement(By.id("nowButton"));
        nowButton.click();
    }

    public void selectToday() {
        WebElement todayButton = driver.findElement(By.id("todayButton"));
        todayButton.click();
    }

    public boolean getArriveBy() {
        WebElement arriveByElement = driver.findElement(By.id("arriveBy"));
        return arriveByElement.isSelected();
    }

    public void setArriveBy(boolean arriveBy) {
        WebElement arriveByElement = driver.findElement(By.id("arriveBy"));
        boolean currently = arriveByElement.isSelected();
        if (currently!=arriveBy) {
            moveToElement(arriveByElement).click().perform();
        }
    }

    private List<String> getEnabledStopsByGroupName(final String groupName, final String className) {
        final By id = By.id(groupName);
        final WebElement groupElement = waitForClickableLocator(id);
        return getEnabledStopNames(groupElement, className);
    }

    public boolean hasLocation() {
        return "true".equals(waitForAndGet(By.id("havepos")));
    }

    public boolean waitForReady() {
        // geo loc on firefox can be slow even when stubbing location via a file....
        final By plan = By.id(PLAN);
        final WebElement element = driver.findElement(plan);

        final WebDriverWait wait = new WebDriverWait(driver, timeoutInSeconds);
        wait.until(webDriver -> (element.isDisplayed() && element.isEnabled()));

        return element.isDisplayed() && element.isEnabled();
    }

    public boolean hasCookieNamed(String cookieName) {
        return driver.manage().getCookieNamed(cookieName)!=null;
    }

    public Cookie waitForCookie(String cookieName) {
        createWait().until(webDriver -> webDriver.manage().getCookieNamed(cookieName)!=null);
        return driver.manage().getCookieNamed(cookieName);
    }
}
