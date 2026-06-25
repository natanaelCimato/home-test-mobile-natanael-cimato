package com.homechallenge.performance;

import com.homechallenge.data.TestUsers;
import com.homechallenge.pages.HomePage;
import com.homechallenge.pages.ItemDetailsPage;
import com.homechallenge.pages.LoginPage;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MobilePerformanceTest extends BasePerformanceTest {
    @Test
    @DisplayName("Performance: valid login reaches catalog within budget")
    void validLoginCompletesWithinBudget() {
        Duration elapsed = measure(() -> new LoginPage(driver)
                .waitForLoaded()
                .loginAs(TestUsers.VALID_EMAIL, TestUsers.VALID_PASSWORD));

        assertWithinBudget("login", elapsed, budget("performance.loginBudgetSeconds", "PERF_LOGIN_BUDGET_SECONDS", 600));
    }

    @Test
    @DisplayName("Performance: deep catalog item opens within budget")
    void deepCatalogItemOpensWithinBudget() {
        HomePage homePage = new LoginPage(driver)
                .waitForLoaded()
                .loginAs(TestUsers.VALID_EMAIL, TestUsers.VALID_PASSWORD);

        Duration elapsed = measure(() -> homePage.openCatalogItem("Twilight Glow"));
        ItemDetailsPage detailsPage = new ItemDetailsPage(driver).waitForLoaded("Twilight Glow");

        assertTrue(detailsPage.hasDetailsFor("Twilight Glow"), "The target item details should be visible.");
        assertWithinBudget("catalog item open", elapsed, budget("performance.catalogBudgetSeconds", "PERF_CATALOG_BUDGET_SECONDS", 420));
    }

    private static <T> Duration measure(Supplier<T> action) {
        Instant start = Instant.now();
        action.get();
        return Duration.between(start, Instant.now());
    }

    private static void assertWithinBudget(String operation, Duration elapsed, Duration budget) {
        Allure.parameter(operation + " elapsed seconds", elapsed.toSeconds());
        Allure.parameter(operation + " budget seconds", budget.toSeconds());

        assertTrue(
                elapsed.compareTo(budget) <= 0,
                operation + " took " + elapsed.toSeconds() + "s and exceeded the " + budget.toSeconds() + "s budget."
        );
    }

    private static Duration budget(String systemProperty, String environmentVariable, long defaultSeconds) {
        String propertyValue = System.getProperty(systemProperty);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return Duration.ofSeconds(Long.parseLong(propertyValue));
        }

        String environmentValue = System.getenv(environmentVariable);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return Duration.ofSeconds(Long.parseLong(environmentValue));
        }

        return Duration.ofSeconds(defaultSeconds);
    }
}
