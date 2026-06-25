package com.homechallenge.cucumber;

import com.homechallenge.config.AppConfig;
import com.homechallenge.driver.DriverManager;
import com.homechallenge.reporting.MobileEvidence;
import io.appium.java_client.android.AndroidDriver;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

public class Hooks {
    private static final ThreadLocal<AndroidDriver> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<AppConfig> CONFIG = new ThreadLocal<>();

    @Before(order = 0)
    public void startDriver() {
        DriverManager driverManager = DriverManager.getInstance();
        AndroidDriver driver = driverManager.resetApplicationState();
        DRIVER.set(driver);
    }

    @Before(order = 1)
    public void startEvidence(Scenario scenario) {
        CONFIG.set(DriverManager.getInstance().config());
        MobileEvidence.startVideo(driver(), scenario.getName());
    }

    @After(order = 1)
    public void collectEvidence(Scenario scenario) {
        AndroidDriver driver = DRIVER.get();
        String scenarioContext = "Scenario: " + scenario.getName()
                + System.lineSeparator()
                + "URI: " + scenario.getUri()
                + System.lineSeparator()
                + "Line: " + scenario.getLine()
                + System.lineSeparator()
                + "Status: " + scenario.getStatus()
                + System.lineSeparator()
                + "Framework: Cucumber"
                + System.lineSeparator()
                + MobileEvidence.environmentSummary(CONFIG.get());

        MobileEvidence.attachScenarioDiagnostics(driver, scenarioContext);
        if (scenario.isFailed()) {
            MobileEvidence.attachFailureDiagnostics(driver, scenarioContext, null);
        }

        if (MobileEvidence.shouldAttachFinalScreenshot(scenario.isFailed())) {
            MobileEvidence.attachFinalScreenshot(driver, "final-screenshot - " + scenario.getName());
        }
        MobileEvidence.stopVideo(driver, "screen-recording - " + scenario.getName());
    }

    @After(order = 0)
    public void clearScenarioContext() {
        DRIVER.remove();
        CONFIG.remove();
    }

    public static AndroidDriver driver() {
        AndroidDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("The Appium driver was not created for this scenario.");
        }
        return driver;
    }

    @AfterAll
    public static void quitDriver() {
        DriverManager.getInstance().quitDriver();
    }
}
