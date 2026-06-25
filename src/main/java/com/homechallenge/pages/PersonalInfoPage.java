package com.homechallenge.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;

import java.time.Duration;

public final class PersonalInfoPage extends BasePage<PersonalInfoPage> {
    private static final String ADDRESS_INPUT = "addressInput";
    private static final String CITY_INPUT = "cityInput";
    private static final String ZIP_INPUT = "zipInput";
    private static final String OPEN_DATE_PICKER = "openDatePicker";
    private static final String TERMS = "termConditions";
    private static final String SIGNUP_BUTTON = "signupButton";

    public PersonalInfoPage(AndroidDriver driver) {
        super(driver);
    }

    @Override
    @Step("Wait for personal information form")
    public PersonalInfoPage waitForLoaded() {
        findByAccessibilityId(ADDRESS_INPUT);
        findByAccessibilityId(CITY_INPUT);
        findByAccessibilityId(ZIP_INPUT);
        return this;
    }

    @Step("Fill personal address information")
    public PersonalInfoPage fillAddressInformation(String address, String city, String zipCode) {
        typeByAccessibilityId(ADDRESS_INPUT, address);
        typeByAccessibilityId(CITY_INPUT, city);
        typeByAccessibilityId(ZIP_INPUT, zipCode);
        return this;
    }

    @Step("Choose default birth date")
    public PersonalInfoPage chooseDefaultBirthDate() {
        hideKeyboardIfShown();
        tapAccessibilityId(OPEN_DATE_PICKER);

        if (isTextVisible("OK", Duration.ofSeconds(5))) {
            tapText("OK");
        } else if (isTextVisible("Confirm", Duration.ofSeconds(5))) {
            tapText("Confirm");
        } else {
            driver.navigate().back();
        }

        return this;
    }

    @Step("Accept terms and conditions")
    public PersonalInfoPage acceptTermsAndConditions() {
        if (!isVisible(AppiumBy.accessibilityId(TERMS), Duration.ofSeconds(3))) {
            try {
                scrollToText("Agree to terms and conditions.");
            } catch (RuntimeException ignored) {
                // The checkbox can already be reachable even if the label is not scrollable.
            }
        }
        tapAccessibilityId(TERMS);
        return this;
    }

    @Step("Submit registration")
    public RegistrationSuccessPage submitSignup() {
        if (!isVisible(AppiumBy.accessibilityId(SIGNUP_BUTTON), Duration.ofSeconds(3))) {
            try {
                scrollToText("Signup!");
            } catch (RuntimeException ignored) {
                // The button is normally visible on the registration form.
            }
        }
        tapAccessibilityIdOrText(SIGNUP_BUTTON, "Signup!");
        return new RegistrationSuccessPage(driver).waitForLoaded();
    }
}
