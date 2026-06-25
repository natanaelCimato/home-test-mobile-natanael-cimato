package com.homechallenge.junit;

import com.homechallenge.config.AppConfig;
import com.homechallenge.driver.DriverManager;
import com.homechallenge.reporting.MobileEvidence;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class MobileEvidenceExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    @Override
    public void beforeTestExecution(ExtensionContext context) {
        AppConfig config = DriverManager.getInstance().config();
        MobileEvidence.addEnvironmentLabels(config);
        Allure.label("framework", "JUnit 5");
        Allure.parameter("testClass", context.getRequiredTestClass().getName());
        Allure.parameter("testMethod", context.getRequiredTestMethod().getName());

        AndroidDriver driver = DriverManager.getInstance().currentDriver();
        MobileEvidence.startVideo(driver, context.getDisplayName());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        AndroidDriver driver = DriverManager.getInstance().currentDriver();
        String displayName = context.getDisplayName();
        boolean failed = context.getExecutionException().isPresent();

        context.getExecutionException().ifPresent(throwable ->
                MobileEvidence.attachFailureDiagnostics(driver, displayName, throwable));

        if (MobileEvidence.shouldAttachFinalScreenshot(failed)) {
            MobileEvidence.attachFinalScreenshot(driver, "final-screenshot - " + displayName);
        }
        MobileEvidence.stopVideo(driver, "screen-recording - " + displayName);
    }
}
