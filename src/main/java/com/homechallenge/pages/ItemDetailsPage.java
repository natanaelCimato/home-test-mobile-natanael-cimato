package com.homechallenge.pages;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;

import java.time.Duration;

public final class ItemDetailsPage extends BasePage<ItemDetailsPage> {
    private String expectedTitle;

    public ItemDetailsPage(AndroidDriver driver) {
        super(driver);
    }

    @Override
    @Step("Wait for item details screen")
    public ItemDetailsPage waitForLoaded() {
        if (expectedTitle != null) {
            findText(expectedTitle);
        }
        return this;
    }

    @Step("Wait for item details title: {title}")
    public ItemDetailsPage waitForLoaded(String title) {
        this.expectedTitle = title;
        return waitForLoaded();
    }

    @Step("Validate item details for: {title}")
    public boolean hasDetailsFor(String title) {
        return isTextVisible(title, Duration.ofSeconds(10))
                && isTextVisible("by \"Wolfy\"")
                && isTextVisible("11/10/1992")
                && isTextContainingVisible("Donec in placerat lorem.", Duration.ofSeconds(5));
    }
}
