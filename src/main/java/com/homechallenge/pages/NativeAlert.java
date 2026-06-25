package com.homechallenge.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public final class NativeAlert {
    private static final Duration ALERT_TIMEOUT = Duration.ofSeconds(8);

    private final AndroidDriver driver;

    public NativeAlert(AndroidDriver driver) {
        this.driver = driver;
    }

    @Step("Validate native alert contains: {expectedMessage}")
    public boolean isMessageDisplayed(String expectedMessage) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, ALERT_TIMEOUT);
            wait.ignoring(StaleElementReferenceException.class);

            return wait.until(currentDriver -> {
                try {
                    String alertText = currentDriver.switchTo().alert().getText();
                    if (alertText != null && alertText.contains(expectedMessage)) {
                        return true;
                    }
                } catch (NoAlertPresentException ignored) {
                    // Fall through to native text lookup.
                }

                return firstDisplayed(containsTextLocator(expectedMessage)) != null;
            });
        } catch (TimeoutException exception) {
            return false;
        }
    }

    @Step("Accept native alert if present")
    public void acceptIfPresent() {
        try {
            driver.switchTo().alert().accept();
            return;
        } catch (NoAlertPresentException ignored) {
            // Fall back to finding the Android dialog button.
        }

        for (String buttonText : new String[]{"OK", "Ok", "Aceptar"}) {
            WebElement button = firstDisplayed(AppiumBy.xpath("//*[@text=" + BasePage.xpathLiteral(buttonText) + "]"));
            if (button != null) {
                button.click();
                return;
            }
        }
    }

    private By containsTextLocator(String expectedMessage) {
        String literal = BasePage.xpathLiteral(expectedMessage);
        return AppiumBy.xpath("//*[contains(@text, " + literal + ") or contains(@content-desc, " + literal + ")]");
    }

    private WebElement firstDisplayed(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed()) {
                    return element;
                }
            } catch (StaleElementReferenceException ignored) {
                // Native dialogs can redraw while Appium is reading the hierarchy.
            }
        }

        return null;
    }
}
