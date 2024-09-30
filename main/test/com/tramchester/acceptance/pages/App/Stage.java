package com.tramchester.acceptance.pages.App;

import com.tramchester.domain.time.TramTime;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class Stage {
    private final WebElement row;

    public Stage(WebElement row) {

        this.row = row;
    }

    public TramTime getDepartTime() {
        String fieldText = getFieldTextByClassId("departTime");
        fieldText = fieldText.replace(" +1d","+24").trim();

        return TramTime.parse(fieldText);
    }

    public String getAction() {
        return getFieldTextByClassId("action");
    }

    public String getActionStation() {
        return getFieldTextByClassId("actionStation");
    }

    public int getPlatform() {
        String platform = getFieldTextByClassId("platform");
        if (platform.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(platform);
    }

    public String getHeadsign() {
        return getFieldTextByClassId("headsign");
    }

    public String getRouteName() {
        return getFieldTextByClassId("lineClass");
    }

    public int getPassedStops() {
        return Integer.parseInt(getFieldTextByClassId("passedStops"));
    }

    private String getFieldTextByClassId(String fieldName) {
        return row.findElement(By.className(fieldName)).getText();
    }
}
