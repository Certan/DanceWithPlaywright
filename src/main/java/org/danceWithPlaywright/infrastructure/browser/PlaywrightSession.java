package org.danceWithPlaywright.infrastructure.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Immutable holder for Playwright resources created for a single UI scenario.
 */
public record PlaywrightSession(
        Playwright playwright,
        Browser browser,
        BrowserContext browserContext,
        Page page
) {
}

