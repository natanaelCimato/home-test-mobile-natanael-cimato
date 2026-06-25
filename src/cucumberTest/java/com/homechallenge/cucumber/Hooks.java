package com.homechallenge.cucumber;

import com.homechallenge.driver.DriverManager;
import com.homechallenge.reporting.MobileEvidence;
import io.appium.java_client.android.AndroidDriver;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;

public class Hooks {
    private static final ThreadLocal<AndroidDriver> DRIVER = new ThreadLocal<>();

    @Before(order = 0)
    public void startDriver() {
        DriverManager driverManager = DriverManager.getInstance();
        AndroidDriver driver = driverManager.getDriver();
        driverManager.resetApplicationState();
        DRIVER.set(driver);
    }

    @Before(order = 1)
    public void startEvidence(Scenario scenario) {
        MobileEvidence.addEnvironmentLabels(com.homechallenge.config.AppConfig.fromEnvironment());
        Allure.label("framework", "Cucumber");
        Allure.parameter("featureUri", scenario.getUri().toString());
        Allure.parameter("featureLine", scenario.getLine());
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
                + "Status: " + scenario.getStatus();

        MobileEvidence.attachScenarioDiagnostics(driver, scenarioContext);
        if (scenario.isFailed()) {
            MobileEvidence.attachFailureDiagnostics(driver, scenarioContext, null);
        }

        MobileEvidence.attachFinalScreenshot(driver, "final-screenshot - " + scenario.getName());
        MobileEvidence.stopVideo(driver, "screen-recording - " + scenario.getName());
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
        DRIVER.remove();
    }
}
