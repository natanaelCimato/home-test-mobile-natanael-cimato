package com.homechallenge.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public final class HomePage extends BasePage<HomePage> {
    private static final String ITEMS_LIST = "itemsList";

    public HomePage(AndroidDriver driver) {
        super(driver);
    }

    @Override
    @Step("Wait for art gallery catalog")
    public HomePage waitForLoaded() {
        findByAccessibilityId(ITEMS_LIST);
        return this;
    }

    @Step("Validate catalog is visible")
    public boolean isCatalogVisible() {
        return isVisible(AppiumBy.accessibilityId(ITEMS_LIST), Duration.ofSeconds(10));
    }

    @Step("Find catalog item: {title}")
    public boolean hasCatalogItem(String title) {
        try {
            scrollToCatalogItem(title);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Step("Open catalog item: {title}")
    public ItemDetailsPage openCatalogItem(String title) {
        WebElement item = scrollToCatalogItem(title);
        item.click();
        return new ItemDetailsPage(driver).waitForLoaded(title);
    }

    private WebElement scrollToCatalogItem(String title) {
        return scrollToText(title);
    }
}
