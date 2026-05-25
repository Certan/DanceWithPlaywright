package org.danceWithPlaywright.infrastructure.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.danceWithPlaywright.infrastructure.enums.SupportedBrowsers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlayWrightManager {
    private final BrowserOptions browserOptions;

    @Autowired
    public PlayWrightManager(BrowserOptions browserOptions) {
        this.browserOptions = browserOptions;
    }

    public PlaywrightSession start(String browserType) {
        log.info("Starting Playwright with browser: {}", browserType);

        Playwright playwright = Playwright.create();

        SupportedBrowsers type = SupportedBrowsers.valueOf(browserType.toUpperCase());

        // Launch the correct browser engine
        Browser browser = switch (type) {
            case FIREFOX -> playwright.firefox().launch(browserOptions.buildLaunchOptions());
            case EDGE -> playwright.chromium().launch(browserOptions.buildLaunchOptions().setChannel("msedge"));
            default -> playwright.chromium().launch(browserOptions.buildLaunchOptions());
        };

        // BrowserContext = isolated session (fresh cookies, storage per scenario)
        BrowserContext browserContext = browser.newContext(browserOptions.buildContextOptions());
        browserContext.setDefaultTimeout(browserOptions.getDefaultTimeout());

        // Page = one browser tab
        Page page = browserContext.newPage();

        log.info("Playwright started - browser: {}, browserUrl configured in context", browserType);

        return new PlaywrightSession(playwright, browser, browserContext, page);
    }

    public void stop(PlaywrightSession session) {
        log.info("Stopping Playwright resources...");

        if (session == null) {
            log.info("No Playwright session found to stop");
            return;
        }

        Page page = session.page();
        BrowserContext browserContext = session.browserContext();
        Browser browser = session.browser();
        Playwright playwright = session.playwright();

        try {
            if (page != null) page.close();
        } catch (Exception e) {
            log.warn("Failed to close page: {}", e.getMessage());
        } finally {
            try {
                if (browserContext != null) browserContext.close();
            } catch (Exception e) {
                log.warn("Failed to close browser context: {}", e.getMessage());
            } finally {
                try {
                    if (browser != null) browser.close();
                } catch (Exception e) {
                    log.warn("Failed to close browser: {}", e.getMessage());
                }
                finally {
                    try {
                        if (playwright != null) playwright.close();
                    } catch (Exception e) {
                        log.warn("Failed to close Playwright: {}", e.getMessage());
                    }
                }
            }
        }


        log.info("Playwright resources stopped");
    }

}
