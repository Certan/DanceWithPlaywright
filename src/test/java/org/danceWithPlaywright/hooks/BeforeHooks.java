package org.danceWithPlaywright.hooks;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.danceWithPlaywright.context.UiTestContext;
import org.danceWithPlaywright.infrastructure.browser.PlayWrightManager;
import org.danceWithPlaywright.infrastructure.browser.PlaywrightSession;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@RequiredArgsConstructor
public class BeforeHooks {

    private final PlayWrightManager playWrightManager;
    private final UiTestContext uiTestContext;

    @Value("${browser.type:CHROMIUM}")
    private String browserType;


    @Before(order = 0, value = "@ui")
    public void setUpUiTest(Scenario scenario) {
        log.info("Starting scenario: [{}]", scenario.getName());

        PlaywrightSession session = playWrightManager.start(browserType);

        uiTestContext.setPlaywright(session.playwright());
        uiTestContext.setBrowser(session.browser());
        uiTestContext.setBrowserContext(session.browserContext());
        uiTestContext.setPage(session.page());

        if (uiTestContext.getPage() == null) {
            throw new IllegalStateException("Playwright page was not initialized for scenario: " + scenario.getName());
        }


        log.info("Browser ready for scenario: [{}]", scenario.getName());
    }
}
