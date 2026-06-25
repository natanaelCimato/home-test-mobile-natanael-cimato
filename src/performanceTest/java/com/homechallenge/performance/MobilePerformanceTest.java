package com.homechallenge.performance;

import com.homechallenge.data.TestUsers;
import com.homechallenge.pages.HomePage;
import com.homechallenge.pages.ItemDetailsPage;
import com.homechallenge.pages.LoginPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MobilePerformanceTest extends BasePerformanceTest {
    private static final String DEEP_CATALOG_ITEM = "Twilight Glow";

    private PerformanceSettings settings;
    private PerformanceMeter meter;

    @BeforeEach
    void setUpPerformanceMeter() {
        settings = PerformanceSettings.fromEnvironment();
        meter = new PerformanceMeter(settings);
    }

    @Test
    @DisplayName("Performance: login submit reaches catalog within budget")
    void loginSubmitCompletesWithinBudget() {
        PerformanceBudget budget = settings.loginSubmitBudget();
        PerformanceResult result = meter.measure(budget.operation(), () -> {
            resetApplication();
            LoginPage loginPage = new LoginPage(driver)
                    .waitForLoaded()
                    .enterEmail(TestUsers.VALID_EMAIL)
                    .enterPassword(TestUsers.VALID_PASSWORD);

            return () -> {
                loginPage.tapLogin();
                assertTrue(new HomePage(driver).waitForLoaded().isCatalogVisible(), "The catalog should be visible.");
            };
        });

        PerformanceReporter.publish(result, settings, budget, driver);
        PerformanceReporter.assertWithinHardBudget(result, budget);
    }

    @Test
    @DisplayName("Performance: deep catalog item opens within budget")
    void deepCatalogItemOpensWithinBudget() {
        PerformanceBudget budget = settings.catalogItemOpenBudget();
        PerformanceResult result = meter.measure(budget.operation() + "-" + DEEP_CATALOG_ITEM, () -> {
            resetApplication();
            HomePage homePage = loginToCatalog();

            return () -> {
                ItemDetailsPage detailsPage = homePage.openCatalogItem(DEEP_CATALOG_ITEM);
                assertTrue(detailsPage.hasDetailsFor(DEEP_CATALOG_ITEM), "The target item details should be visible.");
            };
        });

        PerformanceReporter.publish(result, settings, budget, driver);
        PerformanceReporter.assertWithinHardBudget(result, budget);
    }

    private HomePage loginToCatalog() {
        return new LoginPage(driver)
                .waitForLoaded()
                .loginAs(TestUsers.VALID_EMAIL, TestUsers.VALID_PASSWORD);
    }
}
