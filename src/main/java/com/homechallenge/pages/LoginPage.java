package com.homechallenge.pages;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;

public final class LoginPage extends BasePage<LoginPage> {
    private static final String EMAIL_FIELD = "emailField";
    private static final String PASSWORD_FIELD = "passwordField";
    private static final String LOGIN_BUTTON = "Login";
    private static final String REGISTER_BUTTON = "registerButton";

    public LoginPage(AndroidDriver driver) {
        super(driver);
    }

    @Override
    @Step("Wait for login screen")
    public LoginPage waitForLoaded() {
        findByAccessibilityId(EMAIL_FIELD);
        findByAccessibilityId(PASSWORD_FIELD);
        return this;
    }

    @Step("Enter login email")
    public LoginPage enterEmail(String email) {
        typeByAccessibilityId(EMAIL_FIELD, email);
        return this;
    }

    @Step("Enter login password")
    public LoginPage enterPassword(String password) {
        typeByAccessibilityId(PASSWORD_FIELD, password);
        return this;
    }

    @Step("Login with valid credentials")
    public HomePage loginAs(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        tapLogin();
        return new HomePage(driver).waitForLoaded();
    }

    @Step("Submit login expecting validation")
    public LoginPage submitInvalidLogin(String email, String password) {
        if (!email.isBlank()) {
            enterEmail(email);
        }
        if (!password.isBlank()) {
            enterPassword(password);
        }
        tapLogin();
        return this;
    }

    @Step("Tap Login")
    public LoginPage tapLogin() {
        hideKeyboardIfShown();
        tapAccessibilityId(LOGIN_BUTTON);
        return this;
    }

    @Step("Open registration form")
    public RegistrationPage goToRegistration() {
        tapAccessibilityId(REGISTER_BUTTON);
        return new RegistrationPage(driver).waitForLoaded();
    }

    public NativeAlert alert() {
        return new NativeAlert(driver);
    }
}
