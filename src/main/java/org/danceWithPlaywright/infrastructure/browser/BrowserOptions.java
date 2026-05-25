package org.danceWithPlaywright.infrastructure.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Translates application.properties browser settings into a
 * Playwright LaunchOptions object. Spring injects all values via @Value.
 *
 * Why @Component? So Spring manages this bean and can inject it
 * wherever it's needed — no manual 'new BrowserOptions()' anywhere.
 */

@Slf4j
@Component
public class BrowserOptions {
    @Value("${browser.headless:true}")
    private boolean headless;

    @Value("${browser.slow-mo:0}")
    private int slowMo;

    @Value("${browser.channel:}")
    private String channel;

    @Value("${browser.viewport.width:1920}")
    private int viewportWidth;

    @Value("${browser.viewport.height:1080}")
    private int viewportHeight;

    @Value("${app.base-url}")
    private String baseUrl;

    @Getter
    @Value("${app.default-timeout:30000}")
    private double defaultTimeout;

    public BrowserType.LaunchOptions buildLaunchOptions() {
        var options = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(slowMo);
        if (channel != null && !channel.isBlank()) {
            options.setChannel(channel);
        }

        log.info("Browser options - headless: {}, slowMo: {}, channel: '{}' ", headless, slowMo, channel);

        return options;
    }

    /**
     * Builds BrowserContext options (viewport + base URL).
     * Every new browser tab (Page) inherits these settings.
     */

    public Browser.NewContextOptions buildContextOptions() {
        return new Browser.NewContextOptions()
                .setViewportSize(viewportWidth, viewportHeight)
                .setBaseURL(baseUrl);
    }

}
