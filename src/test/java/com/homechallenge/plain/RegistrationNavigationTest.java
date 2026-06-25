package com.homechallenge.plain;

import com.homechallenge.pages.LoginPage;
import com.homechallenge.pages.RegistrationPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationNavigationTest extends BaseMobileTest {
    @Test
    @DisplayName("Registration: login screen opens the registration form")
    void loginScreenCanNavigateToRegistrationForm() {
        RegistrationPage registrationPage = new LoginPage(driver)
                .waitForLoaded()
                .goToRegistration();

        assertTrue(registrationPage.isLoaded(), "The registration form should show all account fields.");
    }
}
