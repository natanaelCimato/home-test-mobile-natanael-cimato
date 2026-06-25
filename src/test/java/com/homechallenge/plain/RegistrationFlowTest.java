package com.homechallenge.plain;

import com.homechallenge.data.TestUsers;
import com.homechallenge.pages.RegistrationSuccessPage;
import com.homechallenge.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationFlowTest extends BaseMobileTest {
    @Test
    @DisplayName("Scenario 4: create a new user account")
    void userCanCompleteRegistrationFlow() {
        RegistrationSuccessPage successPage = new LoginPage(driver)
                .waitForLoaded()
                .goToRegistration()
                .fillAccountInformation(TestUsers.uniqueRegistrationEmail(), "John", "Doe", "123")
                .continueToPersonalInformation()
                .fillAddressInformation("Main Street 123", "Buenos Aires", "1001")
                .chooseDefaultBirthDate()
                .acceptTermsAndConditions()
                .submitSignup();

        assertTrue(successPage.isSuccessScreenVisible(), "The registration success screen should be displayed.");
    }
}
