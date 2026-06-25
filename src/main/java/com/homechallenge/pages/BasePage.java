package com.homechallenge.pages;

import com.homechallenge.config.AppConfig;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BasePage<T extends BasePage<T>> {
    protected final AndroidDriver driver;
    protected final WebDriverWait wait;
    protected final AppConfig config;
    private long lastForegroundRecoveryNanos;

    protected BasePage(AndroidDriver driver) {
        this.driver = driver;
        this.config = AppConfig.fromEnvironment();
        this.wait = new WebDriverWait(driver, config.waitTimeout());
        this.wait.ignoring(StaleElementReferenceException.class);
        this.wait.ignoring(WebDriverException.class);
    }

    public abstract T waitForLoaded();

    protected WebElement findByAccessibilityId(String accessibilityId) {
        return wait.until(currentDriver -> {
            dismissBlockingSystemDialogs();

            By[] locators = new By[]{
                    AppiumBy.accessibilityId(accessibilityId),
                    AppiumBy.id(accessibilityId),
                    AppiumBy.xpath("//*[@resource-id=" + xpathLiteral(accessibilityId)
                            + " or @content-desc=" + xpathLiteral(accessibilityId) + "]")
            };

            for (By locator : locators) {
                for (WebElement element : currentDriver.findElements(locator)) {
                    try {
                        if (element.isDisplayed()) {
                            return element;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // React Native can redraw between lookup and visibility checks.
                    }
                }
            }

            return null;
        });
    }

    protected void tapAccessibilityId(String accessibilityId) {
        findByAccessibilityId(accessibilityId).click();
    }

    protected void tapAccessibilityIdOrText(String accessibilityId, String text) {
        if (isVisible(AppiumBy.xpath("//*[@resource-id=" + xpathLiteral(accessibilityId)
                + " or @content-desc=" + xpathLiteral(accessibilityId) + "]"), Duration.ofSeconds(3))) {
            tapAccessibilityId(accessibilityId);
        } else {
            tapText(text);
        }
    }

    protected void typeByAccessibilityId(String accessibilityId, String value) {
        RuntimeException lastException = null;

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement field = findByAccessibilityId(accessibilityId);
                field.click();
                field.clear();
                field.sendKeys(value);
                hideKeyboardIfShown();
                return;
            } catch (TimeoutException exception) {
                lastException = exception;
                hideKeyboardIfShown();
            } catch (StaleElementReferenceException exception) {
                lastException = exception;
                sleepBriefly();
            }
        }

        throw lastException;
    }

    protected WebElement findText(String text) {
        return wait.until(currentDriver -> {
            dismissBlockingSystemDialogs();

            for (WebElement element : currentDriver.findElements(textLocator(text))) {
                try {
                    if (element.isDisplayed()) {
                        return element;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Keep polling until UiAutomator2 returns a fresh element.
                }
            }

            return null;
        });
    }

    protected void tapText(String text) {
        findText(text).click();
    }

    protected boolean isTextVisible(String text) {
        return isTextVisible(text, Duration.ofSeconds(5));
    }

    protected boolean isTextVisible(String text, Duration timeout) {
        return isVisible(textLocator(text), timeout);
    }

    protected boolean isTextContainingVisible(String text, Duration timeout) {
        return isVisible(containsTextLocator(text), timeout);
    }

    protected boolean isVisible(By locator, Duration timeout) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, timeout);
            shortWait.ignoring(StaleElementReferenceException.class);
            shortWait.ignoring(WebDriverException.class);
            shortWait.until(currentDriver -> {
                dismissBlockingSystemDialogs();

                for (WebElement element : currentDriver.findElements(locator)) {
                    try {
                        if (element.isDisplayed()) {
                            return element;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Keep polling until UiAutomator2 returns a fresh element.
                    }
                }

                return null;
            });
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    protected WebElement scrollToText(String text) {
        dismissBlockingSystemDialogs();
        String uiAutomatorText = escapeForUiAutomator(text);
        String command = "new UiScrollable(new UiSelector().scrollable(true))"
                + ".setMaxSearchSwipes(12).scrollTextIntoView(\"" + uiAutomatorText + "\")";
        driver.findElement(AppiumBy.androidUIAutomator(command));
        return findText(text);
    }

    protected void hideKeyboardIfShown() {
        try {
            driver.hideKeyboard();
        } catch (Exception ignored) {
            // Keyboard is already hidden.
        }
    }

    protected void dismissBlockingSystemDialogs() {
        tapFirstVisible(AppiumBy.id("android:id/aerr_wait"));
        recoverFromExternalSettingsIfNeeded();
    }

    protected By textLocator(String text) {
        String literal = xpathLiteral(text);
        return AppiumBy.xpath("//*[normalize-space(@text)=" + literal + " or @content-desc=" + literal + "]");
    }

    protected By containsTextLocator(String text) {
        String literal = xpathLiteral(text);
        return AppiumBy.xpath("//*[contains(@text, " + literal + ") or contains(@content-desc, " + literal + ")]");
    }

    protected static String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }

        String[] parts = value.split("'");
        StringBuilder concat = new StringBuilder("concat(");
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                concat.append(", \"'\", ");
            }
            concat.append("'").append(parts[index]).append("'");
        }
        concat.append(")");
        return concat.toString();
    }

    private String escapeForUiAutomator(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean tapFirstVisible(By locator) {
        try {
            for (WebElement element : driver.findElements(locator)) {
                if (element.isDisplayed()) {
                    element.click();
                    sleepBriefly();
                    return true;
                }
            }
        } catch (Exception ignored) {
            // The system dialog can disappear between lookup and click.
        }

        return false;
    }

    private void recoverFromExternalSettingsIfNeeded() {
        long now = System.nanoTime();
        if (now - lastForegroundRecoveryNanos < Duration.ofSeconds(5).toNanos()) {
            return;
        }
        lastForegroundRecoveryNanos = now;

        try {
            if ("com.google.android.inputmethod.latin".equals(driver.getCurrentPackage())) {
                driver.navigate().back();
                sleepBriefly();
                driver.activateApp(config.appPackage());
                sleepBriefly();
            }
        } catch (Exception ignored) {
            // Foreground package checks are best-effort recovery for slow software emulators.
        }
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
