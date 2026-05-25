package org.danceWithPlaywright.steps.helpers;

import lombok.extern.slf4j.Slf4j;
import org.danceWithPlaywright.context.UiTestContext;
import org.danceWithPlaywright.infrastructure.enums.UiCommons;
import org.danceWithPlaywright.pages.admin.login.AdminLoginPage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@Component
@Scope("cucumber-glue")
public class UiStepHelper {
    private final UiTestContext uiTestContext;

    public UiStepHelper(UiTestContext uiTestContext) {
        this.uiTestContext = uiTestContext;
    }

    @Value("${test-data.valid-admin-username}")
    private String validAdminUsername;

    @Value("${test-data.valid-admin-password}")
    private String validAdminPassword;

    private AdminLoginPage adminLogInPage() {
        return new AdminLoginPage(uiTestContext.getPage());
    }

    public void loginWithPreDefinedCredentials(UiCommons role) {
        loginWithGivenUsernameAndPassword(role, validAdminUsername, validAdminPassword);
    }

    public void loginWithGivenUsernameAndPassword(UiCommons role, String username, String password) {
        switch (role) {
            case ADMIN -> adminLogInPage().login(username, password);
            case USER ->
                    throw new UnsupportedOperationException("User login with username and password not implemented yet");
            default -> throw new IllegalArgumentException("Unsupported role: " + role.name());
        }
    }

    public void submitLoginForm(UiCommons role) {
        switch (role) {
            case ADMIN -> adminLogInPage().clickLoginButton();
            case USER -> throw new UnsupportedOperationException("User login form submission not implemented yet");
            default -> throw new IllegalArgumentException("Unsupported role: " + role.name());
        }
    }

    public void validateLoginErrorMessage(UiCommons role, String errorMessage) {
        switch (role) {
            case ADMIN: {
                assertTrue(adminLogInPage().isErrorMessageVisible());
                assertEquals(errorMessage, adminLogInPage().getErrorMessage());
            }
            break;
            case USER: {
                throw new UnsupportedOperationException("User login error message validation not implemented yet");
            }
            default:
                throw new IllegalArgumentException("Unsupported role: " + role.name());
        }
    }
}
