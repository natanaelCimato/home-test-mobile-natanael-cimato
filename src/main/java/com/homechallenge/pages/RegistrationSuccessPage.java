package com.homechallenge.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;

import java.time.Duration;

public final class RegistrationSuccessPage extends BasePage<RegistrationSuccessPage> {
    private static final String GO_LOGIN_BUTTON = "goLoginButton";

    public RegistrationSuccessPage(AndroidDriver driver) {
        super(driver);
    }

    @Override
    @Step("Wait for registration success screen")
    public RegistrationSuccessPage waitForLoaded() {
        findText("Your user has been created.");
        return this;
    }

    @Step("Validate registration success screen")
    public boolean isSuccessScreenVisible() {
        boolean successMessageVisible = isTextVisible("Your user has been created.", Duration.ofSeconds(10))
                || isTextContainingVisible("Please check your email", Duration.ofSeconds(5))
                || isVisible(AppiumBy.accessibilityId("title"), Duration.ofSeconds(3));

        boolean loginActionVisible = isVisible(AppiumBy.accessibilityId(GO_LOGIN_BUTTON), Duration.ofSeconds(2))
                || isTextVisible("Go to Login", Duration.ofSeconds(2))
                || isVisible(AppiumBy.accessibilityId("Go to Login"), Duration.ofSeconds(2));

        return successMessageVisible && loginActionVisible;
    }
}
