package org.danceWithPlaywright.pages.admin.PIM;

import com.microsoft.playwright.Page;
import org.danceWithPlaywright.core.contracts.Navigable;
import org.danceWithPlaywright.infrastructure.enums.UiPagePaths;
import org.danceWithPlaywright.pages.BasePage;

public class AddEmployeePage extends BasePage implements Navigable {

    private static final String FIRST_NAME = "input[name='firstName']";
    private static final String MIDDLE_NAME = "input[name='middleName']";
    private static final String LAST_NAME = "input[name='lastName']";


    public AddEmployeePage(Page page) {
        super(page);
    }

    @Override
    public void navigateTo() {
        navigateTo(UiPagePaths.PIM_ADD_EMPLOYEE.getPath());
    }
}
