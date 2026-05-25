package org.danceWithPlaywright.context;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Getter
@Setter
public class UiTestContext {
    private Playwright playwright;
    private Browser browser;
    private BrowserContext browserContext;
    private Page page;
}
