package com.homechallenge.driver;

import com.homechallenge.config.AppConfig;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class DriverManager {
    private static final Duration APP_FOREGROUND_TIMEOUT = Duration.ofSeconds(15);
    private static final int RESET_ATTEMPTS = 2;

    private static volatile DriverManager instance;

    private final AppConfig config;
    private AndroidDriver driver;

    private DriverManager() {
        this.config = AppConfig.fromEnvironment();
        Runtime.getRuntime().addShutdownHook(new Thread(this::quitDriver));
    }

    public static DriverManager getInstance() {
        DriverManager currentInstance = instance;
        if (currentInstance == null) {
            synchronized (DriverManager.class) {
                currentInstance = instance;
                if (currentInstance == null) {
                    currentInstance = new DriverManager();
                    instance = currentInstance;
                }
            }
        }
        return currentInstance;
    }

    public synchronized AndroidDriver getDriver() {
        if (driver == null || !isSessionAlive(driver)) {
            driver = DriverFactory.createDriver(config);
        }
        return driver;
    }

    public AppConfig config() {
        return config;
    }

    public synchronized AndroidDriver currentDriver() {
        return driver;
    }

    public synchronized AndroidDriver resetApplicationState() {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= RESET_ATTEMPTS; attempt++) {
            AndroidDriver currentDriver = getDriver();

            try {
                terminateApplicationIfRunning(currentDriver);
                clearApplicationData(currentDriver);
                currentDriver.activateApp(config.appPackage());
                waitForApplicationForeground(currentDriver);
                return currentDriver;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                quitDriver();
            }
        }

        throw new IllegalStateException(
                "Could not reset Android application state after " + RESET_ATTEMPTS + " attempts.",
                lastFailure
        );
    }

    public synchronized void quitDriver() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
                // Driver session already ended.
            } finally {
                driver = null;
            }
        }
    }

    private boolean isSessionAlive(AndroidDriver currentDriver) {
        try {
            return Objects.nonNull(currentDriver.getSessionId());
        } catch (Exception exception) {
            return false;
        }
    }

    private void terminateApplicationIfRunning(AndroidDriver currentDriver) {
        try {
            currentDriver.terminateApp(config.appPackage());
        } catch (WebDriverException ignored) {
            // The app can already be stopped while the Appium session remains healthy.
        }
    }

    private void clearApplicationData(AndroidDriver currentDriver) {
        Object result = currentDriver.executeScript("mobile: shell", Map.of(
                "command", "pm",
                "args", List.of("clear", config.appPackage()),
                "timeout", 120000
        ));

        if (result != null && result.toString().toLowerCase(Locale.ROOT).contains("fail")) {
            throw new IllegalStateException("Android pm clear failed for " + config.appPackage() + ": " + result);
        }
    }

    private void waitForApplicationForeground(AndroidDriver currentDriver) {
        try {
            new WebDriverWait(currentDriver, APP_FOREGROUND_TIMEOUT)
                    .until(ignored -> config.appPackage().equals(currentDriver.getCurrentPackage()));
        } catch (TimeoutException exception) {
            throw new IllegalStateException(
                    "Application did not reach foreground package " + config.appPackage()
                            + " within " + APP_FOREGROUND_TIMEOUT.toSeconds() + " seconds.",
                    exception
            );
        }
    }
}
