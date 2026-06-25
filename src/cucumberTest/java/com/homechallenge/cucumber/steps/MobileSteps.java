package com.homechallenge.cucumber.steps;

import com.homechallenge.cucumber.Hooks;
import com.homechallenge.data.TestUsers;
import com.homechallenge.pages.HomePage;
import com.homechallenge.pages.ItemDetailsPage;
import com.homechallenge.pages.LoginPage;
import com.homechallenge.pages.RegistrationSuccessPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MobileSteps {
    private LoginPage loginPage;
    private HomePage homePage;
    private ItemDetailsPage itemDetailsPage;
    private RegistrationSuccessPage registrationSuccessPage;

    @Given("I am on the login screen")
    public void iAmOnTheLoginScreen() {
        loginPage = new LoginPage(Hooks.driver()).waitForLoaded();
    }

    @When("I login with valid credentials")
    public void iLoginWithValidCredentials() {
        homePage = loginPage.loginAs(TestUsers.VALID_EMAIL, TestUsers.VALID_PASSWORD);
    }

    @When("I submit the login form without credentials")
    public void iSubmitTheLoginFormWithoutCredentials() {
        loginPage.tapLogin();
    }

    @When("I submit login with email {string} and password {string}")
    public void iSubmitLoginWithEmailAndPassword(String email, String password) {
        loginPage.submitInvalidLogin(email, password);
    }

    @When("I login with invalid credentials")
    public void iLoginWithInvalidCredentials() {
        loginPage.submitInvalidLogin("invalid@email.com", "wrong-password");
    }

    @Then("I should see the art gallery catalog")
    public void iShouldSeeTheArtGalleryCatalog() {
        assertTrue(homePage.isCatalogVisible(), "The art catalog should be visible.");
    }

    @Then("the login alert should say {string}")
    public void theLoginAlertShouldSay(String expectedMessage) {
        assertTrue(loginPage.alert().isMessageDisplayed(expectedMessage), "Unexpected login alert.");
    }

    @Given("I am authenticated in the catalog")
    public void iAmAuthenticatedInTheCatalog() {
        loginPage = new LoginPage(Hooks.driver()).waitForLoaded();
        homePage = loginPage.loginAs(TestUsers.VALID_EMAIL, TestUsers.VALID_PASSWORD);
    }

    @When("I open the catalog item {string}")
    public void iOpenTheCatalogItem(String title) {
        itemDetailsPage = homePage.openCatalogItem(title);
    }

    @Then("I should see item details for {string}")
    public void iShouldSeeItemDetailsFor(String title) {
        assertTrue(itemDetailsPage.hasDetailsFor(title), "The item details should match the selected catalog item.");
    }

    @When("I complete the registration form")
    public void iCompleteTheRegistrationForm() {
        registrationSuccessPage = loginPage
                .goToRegistration()
                .fillAccountInformation(TestUsers.uniqueRegistrationEmail(), "John", "Doe", "123")
                .continueToPersonalInformation()
                .fillAddressInformation("Main Street 123", "Buenos Aires", "1001")
                .chooseDefaultBirthDate()
                .acceptTermsAndConditions()
                .submitSignup();
    }

    @Then("I should reach the registration success screen")
    public void iShouldReachTheRegistrationSuccessScreen() {
        assertTrue(registrationSuccessPage.isSuccessScreenVisible(), "The registration success screen should be visible.");
    }
}
