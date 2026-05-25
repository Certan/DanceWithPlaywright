package org.danceWithPlaywright.hooks;

import com.microsoft.playwright.Page;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.danceWithPlaywright.context.UiTestContext;
import org.danceWithPlaywright.infrastructure.browser.PlayWrightManager;
import org.danceWithPlaywright.infrastructure.browser.PlaywrightSession;

@Slf4j
@RequiredArgsConstructor
public class AfterHooks {

    private final UiTestContext uiTestContext;
    private final PlayWrightManager playWrightManager;

    @After(order = 0, value = "@ui")
    public void tearDownUiTest(Scenario scenario) {
        // ── 1. Capture evidence if the scenario failed ──
        if (scenario.isFailed()) {
            captureUiFailureEvidence(scenario);
        }
        // ── 2. Always shut down Playwright ──
        PlaywrightSession session = new PlaywrightSession(
                uiTestContext.getPlaywright(),
                uiTestContext.getBrowser(),
                uiTestContext.getBrowserContext(),
                uiTestContext.getPage());

        playWrightManager.stop(session);

        uiTestContext.setPage(null);
        uiTestContext.setBrowserContext(null);
        uiTestContext.setBrowser(null);
        uiTestContext.setPlaywright(null);

        log.info("Finished scenario: [{}] - status: {}", scenario.getName(), scenario.getStatus());
    }

    private void captureUiFailureEvidence(Scenario scenario) {
        Page page = uiTestContext.getPage();
        if (page == null) {
            log.warn("Failed to capture evidence - Page is null for scenario: {}", scenario.getName());
            return;
        }

        try {
            // ── Full-page screenshot attached directly to the Cucumber HTML report
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            scenario.attach(screenshot, "image/png", "failure-screenshot");

            // ── Current URL tells you exactly where the test died
            scenario.attach(page.url().getBytes(), "text/plain", "failure-url");

            log.error("x Scenario FAILED: [{}] - URL: {}", scenario.getName(), page.url());
        } catch (Exception e) {
            log.error("Failed to capture failure evidence for scenario: {} - error: {}", scenario.getName(), e.getMessage());
        }
    }
}
