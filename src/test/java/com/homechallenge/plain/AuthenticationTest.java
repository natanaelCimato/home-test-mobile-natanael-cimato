package com.homechallenge.plain;

import com.homechallenge.data.TestUsers;
import com.homechallenge.pages.HomePage;
import com.homechallenge.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationTest extends BaseMobileTest {
    @Test
    @DisplayName("Scenario 1: valid credentials authenticate and open the catalog")
    void validUserCanLoginToGalleryCatalog() {
        HomePage homePage = new LoginPage(driver)
                .waitForLoaded()
                .loginAs(TestUsers.VALID_EMAIL, TestUsers.VALID_PASSWORD);

        assertTrue(homePage.isCatalogVisible(), "The art catalog should be visible after login.");
    }
}
