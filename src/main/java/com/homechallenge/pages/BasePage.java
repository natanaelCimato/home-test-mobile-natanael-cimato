package com.homechallenge.pages;

import com.homechallenge.config.AppConfig;
import com.homechallenge.driver.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.function.Supplier;

public abstract class BasePage<T extends BasePage<T>> {
    protected final AndroidDriver driver;
    protected final WebDriverWait wait;
    protected final AppConfig config;
    private long lastForegroundRecoveryNanos;

    protected BasePage(AndroidDriver driver) {
        this.driver = driver;
        this.config = DriverManager.getInstance().config();
        this.wait = createWait(config.waitTimeout());
    }

    public abstract T waitForLoaded();

    protected WebElement findByAccessibilityId(String accessibilityId) {
        return findByAccessibilityId(accessibilityId, config.waitTimeout());
    }

    protected WebElement findByAccessibilityId(String accessibilityId, Duration timeout) {
        By[] locators = accessibilityLocators(accessibilityId);

        return createWait(timeout).until(currentDriver -> {
            dismissBlockingSystemDialogs();
            return firstDisplayed(currentDriver, locators);
        });
    }

    protected void tapAccessibilityId(String accessibilityId) {
        clickWithRetry(() -> findByAccessibilityId(accessibilityId));
    }

    protected void tapAccessibilityIdOrText(String accessibilityId, String text) {
        if (tapAccessibilityIdIfVisible(accessibilityId, Duration.ofSeconds(2))) {
            return;
        }

        tapText(text);
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
            } catch (TimeoutException
                     | StaleElementReferenceException
                     | InvalidElementStateException exception) {
                lastException = exception;
                sleepBriefly();
                hideKeyboardIfShown();
            }
        }

        throw lastException;
    }

    protected WebElement findText(String text) {
        By locator = textLocator(text);

        return wait.until(currentDriver -> {
            dismissBlockingSystemDialogs();
            return firstDisplayed(currentDriver, locator);
        });
    }

    protected void tapText(String text) {
        clickWithRetry(() -> findText(text));
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
            createWait(timeout).until(currentDriver -> {
                dismissBlockingSystemDialogs();
                return firstDisplayed(currentDriver, locator);
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
            WebElement element = firstDisplayed(driver, locator);
            if (element != null) {
                element.click();
                sleepBriefly();
                return true;
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

    private WebDriverWait createWait(Duration timeout) {
        WebDriverWait driverWait = new WebDriverWait(driver, timeout);
        driverWait.ignoring(StaleElementReferenceException.class, NoSuchElementException.class);
        return driverWait;
    }

    private By[] accessibilityLocators(String accessibilityId) {
        String literal = xpathLiteral(accessibilityId);
        return new By[]{
                AppiumBy.accessibilityId(accessibilityId),
                AppiumBy.id(accessibilityId),
                AppiumBy.xpath("//*[@resource-id=" + literal + " or @content-desc=" + literal + "]")
        };
    }

    private WebElement firstDisplayed(SearchContext context, By locator) {
        for (WebElement element : context.findElements(locator)) {
            try {
                if (element.isDisplayed()) {
                    return element;
                }
            } catch (StaleElementReferenceException ignored) {
                // React Native can redraw between lookup and visibility checks.
            }
        }

        return null;
    }

    private WebElement firstDisplayed(SearchContext context, By[] locators) {
        for (By locator : locators) {
            WebElement element = firstDisplayed(context, locator);
            if (element != null) {
                return element;
            }
        }

        return null;
    }

    private boolean tapAccessibilityIdIfVisible(String accessibilityId, Duration timeout) {
        try {
            clickWithRetry(() -> findByAccessibilityId(accessibilityId, timeout));
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private void clickWithRetry(Supplier<WebElement> elementSupplier) {
        RuntimeException lastException = null;

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                elementSupplier.get().click();
                return;
            } catch (StaleElementReferenceException
                     | ElementNotInteractableException exception) {
                lastException = exception;
                sleepBriefly();
            }
        }

        throw lastException;
    }
}
