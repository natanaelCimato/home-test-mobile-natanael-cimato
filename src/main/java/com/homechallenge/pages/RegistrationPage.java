package com.homechallenge.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;

import java.time.Duration;

public final class RegistrationPage extends BasePage<RegistrationPage> {
    private static final String EMAIL_FIELD = "emailField";
    private static final String FIRST_NAME_FIELD = "firstNameField";
    private static final String LAST_NAME_FIELD = "lastNameField";
    private static final String PASSWORD_FIELD = "passwordField";
    private static final String CONTINUE_BUTTON = "continueButton";

    public RegistrationPage(AndroidDriver driver) {
        super(driver);
    }

    @Override
    @Step("Wait for registration form")
    public RegistrationPage waitForLoaded() {
        findByAccessibilityId(EMAIL_FIELD);
        findByAccessibilityId(FIRST_NAME_FIELD);
        findByAccessibilityId(LAST_NAME_FIELD);
        findByAccessibilityId(PASSWORD_FIELD);
        return this;
    }

    @Step("Validate registration form is loaded")
    public boolean isLoaded() {
        return isVisible(AppiumBy.accessibilityId(EMAIL_FIELD), Duration.ofSeconds(10))
                && isVisible(AppiumBy.accessibilityId(FIRST_NAME_FIELD), Duration.ofSeconds(5))
                && isVisible(AppiumBy.accessibilityId(LAST_NAME_FIELD), Duration.ofSeconds(5))
                && isVisible(AppiumBy.accessibilityId(PASSWORD_FIELD), Duration.ofSeconds(5));
    }

    @Step("Fill registration account information")
    public RegistrationPage fillAccountInformation(String email, String firstName, String lastName, String password) {
        typeByAccessibilityId(EMAIL_FIELD, email);
        typeByAccessibilityId(FIRST_NAME_FIELD, firstName);
        typeByAccessibilityId(LAST_NAME_FIELD, lastName);
        hideKeyboardIfShown();
        typeByAccessibilityId(PASSWORD_FIELD, password);
        return this;
    }

    @Step("Continue to personal information")
    public PersonalInfoPage continueToPersonalInformation() {
        hideKeyboardIfShown();
        tapAccessibilityIdOrText(CONTINUE_BUTTON, "Continue");
        return new PersonalInfoPage(driver).waitForLoaded();
    }
}
