package com.homechallenge.plain;

import com.homechallenge.data.TestUsers;
import com.homechallenge.pages.HomePage;
import com.homechallenge.pages.ItemDetailsPage;
import com.homechallenge.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogExplorationTest extends BaseMobileTest {
    @Test
    @DisplayName("Scenario 3: scroll the catalog and open Twilight Glow details")
    void canFindTwilightGlowDeepInCatalog() {
        HomePage homePage = new LoginPage(driver)
                .waitForLoaded()
                .loginAs(TestUsers.VALID_EMAIL, TestUsers.VALID_PASSWORD);

        assertTrue(homePage.hasCatalogItem("Twilight Glow"), "Twilight Glow should be present in the catalog.");

        ItemDetailsPage detailsPage = homePage.openCatalogItem("Twilight Glow");
        assertTrue(detailsPage.hasDetailsFor("Twilight Glow"), "The details screen should match the selected item.");
    }
}
