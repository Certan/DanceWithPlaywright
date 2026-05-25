package org.danceWithPlaywright.steps;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.danceWithPlaywright.context.UiTestContext;
import org.danceWithPlaywright.infrastructure.enums.UiCommons;
import org.danceWithPlaywright.infrastructure.enums.UiPagePaths;
import org.danceWithPlaywright.steps.helpers.UiStepHelper;

@Slf4j
@RequiredArgsConstructor
public class UiStepDef {

    private final UiTestContext uiTestContext;
    private final UiStepHelper uiStepHelper;

    @ParameterType(".*")
    public UiPagePaths uiPage(String value) {
        return UiPagePaths.valueOf(value.toUpperCase());
    }

    @ParameterType(".*")
    public UiCommons uiCommons(String value) {
        return UiCommons.valueOf(value.toUpperCase());
    }

    @ParameterType(".*")
    public UiPagePaths uiPagePath(String value) {return UiPagePaths.valueOf(value.toUpperCase());}

    @Given("the user is on the {uiPage} page")
    public void navigateToTheTargetPage(UiPagePaths targetPage) {
        log.info("Navigating to page: [{}] -> {}", targetPage.name(), targetPage.getPath());
        uiTestContext.getPage().navigate(targetPage.getPath());
    }

    @When("the user successfully logs in as {uiCommons}")
    public void logInAs(UiCommons role) {
        log.info("Logging in as: [{}] ", role.name());
        uiTestContext.getPage().waitForLoadState();
        uiStepHelper.loginWithPreDefinedCredentials(role);
        log.info("Logged in as: [{}] ", role.name());
    }

    @When("the {uiCommons} logs in with username: {word} and password: {word}")
    public void logInWithUsernameAndPassword(UiCommons role, String username, String password) {
        log.info("Logging in with username: [{}] and password: [{}]", username, password);
        uiTestContext.getPage().waitForLoadState();
        uiStepHelper.loginWithGivenUsernameAndPassword(role, username, password);
    }

    @Then("the user gets redirected to the {uiPagePath} page")
    public void getsRedirectedToTheDashboardPage(UiPagePaths targetPage) {
        log.info("Verifying user is redirected to page: [{}] -> {}", targetPage.name(), targetPage.getPath());
        uiTestContext.getPage().waitForURL(targetPage.getPath());
        log.info("User has been redirected to page: [{}] -> {}", targetPage.name(), targetPage.getPath());
    }

    @And("the {uiCommons} submits the login form")
    public void submitTheLoginForm(UiCommons role) {
        log.info("Submitting login form for role: [{}] ", role.name());
        uiStepHelper.submitLoginForm(role);
        log.info("Login form submitted for role: [{}] ", role.name());
    }

    @Then("the {uiCommons} login attempt should fail with error message {string}")
    public void loginFailsWithErrorMessage(UiCommons role, String errorMessage) {
        log.info("Verifying login attempt failed with error message: [{}]", errorMessage);
        uiStepHelper.validateLoginErrorMessage(role, errorMessage);
        log.info("Login attempt failed with error message: [{}]", errorMessage);
    }
}
