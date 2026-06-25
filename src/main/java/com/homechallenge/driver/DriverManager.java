package com.homechallenge.driver;

import com.homechallenge.config.AppConfig;
import io.appium.java_client.android.AndroidDriver;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DriverManager {
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
            driver = DriverFactory.createDriver();
        }
        return driver;
    }

    public synchronized AndroidDriver currentDriver() {
        return driver;
    }

    public synchronized void resetApplicationState() {
        AndroidDriver currentDriver = getDriver();

        try {
            currentDriver.terminateApp(config.appPackage());
        } catch (Exception ignored) {
            // The app can already be stopped before the first scenario.
        }

        clearApplicationData(currentDriver);
        currentDriver.activateApp(config.appPackage());
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

    private void clearApplicationData(AndroidDriver currentDriver) {
        try {
            currentDriver.executeScript("mobile: shell", Map.of(
                    "command", "pm",
                    "args", List.of("clear", config.appPackage()),
                    "timeout", 120000
            ));
        } catch (Exception ignored) {
            // Keeping the existing session is more important than failing before a test starts.
        }
    }
}
