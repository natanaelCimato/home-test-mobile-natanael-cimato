package com.homechallenge.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public final class NativeAlert {
    private final AndroidDriver driver;

    public NativeAlert(AndroidDriver driver) {
        this.driver = driver;
    }

    @Step("Validate native alert contains: {expectedMessage}")
    public boolean isMessageDisplayed(String expectedMessage) {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(10)).until(currentDriver -> {
                try {
                    String alertText = currentDriver.switchTo().alert().getText();
                    if (alertText != null && alertText.contains(expectedMessage)) {
                        return true;
                    }
                } catch (NoAlertPresentException ignored) {
                    // Fall through to native text lookup.
                }

                return !currentDriver.findElements(containsTextLocator(expectedMessage)).isEmpty();
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
            if (!driver.findElements(AppiumBy.xpath("//*[@text=" + BasePage.xpathLiteral(buttonText) + "]")).isEmpty()) {
                driver.findElement(AppiumBy.xpath("//*[@text=" + BasePage.xpathLiteral(buttonText) + "]")).click();
                return;
            }
        }
    }

    private By containsTextLocator(String expectedMessage) {
        String literal = BasePage.xpathLiteral(expectedMessage);
        return AppiumBy.xpath("//*[contains(@text, " + literal + ") or contains(@content-desc, " + literal + ")]");
    }
}
