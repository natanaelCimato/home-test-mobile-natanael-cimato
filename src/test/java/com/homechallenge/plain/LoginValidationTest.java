package com.homechallenge.plain;

import com.homechallenge.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginValidationTest extends BaseMobileTest {
    @ParameterizedTest(name = "Scenario 2: incomplete credentials are blocked [{index}]")
    @MethodSource("incompleteCredentialCases")
    void incompleteCredentialsShowRequiredFieldsAlert(String email, String password) {
        LoginPage loginPage = new LoginPage(driver)
                .waitForLoaded()
                .submitInvalidLogin(email, password);

        assertTrue(
                loginPage.alert().isMessageDisplayed("Please complete both fields"),
                "The app should show the missing fields alert for incomplete credentials."
        );
    }

    @Test
    @DisplayName("Scenario 2: invalid credentials are blocked")
    void invalidCredentialsShowErrorAlert() {
        LoginPage loginPage = new LoginPage(driver)
                .waitForLoaded()
                .submitInvalidLogin("invalid@email.com", "wrong-password");

        assertTrue(
                loginPage.alert().isMessageDisplayed("Invalid user or password"),
                "The app should show the invalid credentials alert."
        );
    }

    private static Stream<Arguments> incompleteCredentialCases() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("johndoe@email.com", ""),
                Arguments.of("", "123")
        );
    }
}
