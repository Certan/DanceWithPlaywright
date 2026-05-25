package org.danceWithPlaywright.pages.admin.login;

import com.microsoft.playwright.Page;
import org.danceWithPlaywright.pages.BasePage;

public class AdminLoginPage extends BasePage {

    private static final String USERNAME_INPUT = "[name='username']";
    private static final String PASSWORD_INPUT = "[name='password']";
    private static final String LOGIN_BUTTON = "[type='submit']";
    private static final String INVALID_CREDENTIALS_MESSAGE = "text='Invalid credentials'";

    public AdminLoginPage(Page page) {
        super(page);
    }

    public void fillUsername(String username) {
        fill(USERNAME_INPUT, username);
    }

    public void fillPassword(String password) {
        fill(PASSWORD_INPUT, password);
    }

    public void fillCredentials(String username, String password) {
        fill(USERNAME_INPUT, username);
        fill(PASSWORD_INPUT, password);
    }

    public void clickLoginButton() {
        click(LOGIN_BUTTON);
    }

    public void login(String username, String password) {
        fillCredentials(username, password);
        clickLoginButton();
    }

    public boolean isErrorMessageVisible() {
        return isVisible(INVALID_CREDENTIALS_MESSAGE);
    }

    public String getErrorMessage() {
        return getText(INVALID_CREDENTIALS_MESSAGE);
    }
}
