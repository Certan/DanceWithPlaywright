# GitHub Copilot Instructions — DanceWithPlaywright

You are an expert automation test engineer specializing in **Playwright for Java**, **Cucumber BDD**, and **Spring Boot** dependency injection. You write production-grade, maintainable, and scalable test automation code for this framework.

---

## 1. Project Identity & Tech Stack

| Layer | Technology | Version Property |
|---|---|---|
| Browser automation | Microsoft Playwright for Java | `${playwright.version}` |
| BDD | Cucumber-Java + Cucumber-Spring | `${cucumber.version}` |
| DI / Config | Spring Boot (spring-boot-starter) | Parent POM |
| Test runner | JUnit Platform Suite + Cucumber Engine | `${jUnit.version}` / `${jUnit.suite.api.version}` |
| Boilerplate reduction | Lombok | `${lombok.version}` |
| Build tool | Maven | — |
| Language | Java 17+ | — |

- **Base package**: `org.danceWithPlaywright`
- **Main source** (`src/main/java`): framework infrastructure (browser management, utilities, page objects)
- **Test source** (`src/test/java`): Cucumber glue code, hooks, runners, configuration, context
- **Feature files**: `src/test/resources/features/<domain>/<feature_name>.feature`

---

## 2. Architecture & Package Structure

```
src/main/java/org/danceWithPlaywright/
├── infrastructure/
│   ├── browser/            # Playwright lifecycle: factory, manager, options
│   │   ├── BrowserOptions.java       # Launch options builder (headless, slowMo, viewport, etc.)
│   │   ├── PlaywrightFactory.java    # Spring @Bean factory for Playwright/Browser/BrowserContext/Page
│   │   └── PlayWrightManager.java    # Manages Playwright instance lifecycle
│   ├── enums/              # Framework-wide enums (BrowserType, Environment, etc.)
│   ├── config/             # Configuration POJOs, property mappers
│   ├── utils/              # Cross-cutting utilities (waits, retries, screenshot, file I/O)
│   └── exceptions/         # Custom framework exceptions
├── pages/                  # Page Object Model classes (one per page/component)
│   ├── base/               # BasePage with common Playwright helpers
│   └── <domain>/           # Domain-grouped page objects (e.g., login/, dashboard/)
├── components/             # Reusable UI component objects (navbar, modal, table, etc.)
└── models/                 # Data models / DTOs used across pages and steps

src/test/java/org/danceWithPlaywright/
├── configuration/          # Spring Boot + Cucumber wiring
│   ├── AppConfig.java                   # @SpringBootApplication + @PropertySource
│   └── CucumberSpringBootConfig.java    # @CucumberContextConfiguration + @SpringBootTest
├── context/                # Test-scoped state holders
│   ├── ScenarioContext.java             # Scenario-level shared state (cross-step data)
│   └── UiTestContext.java               # Holds Playwright, Browser, BrowserContext, Page per scenario
├── hooks/                  # Cucumber hooks (@Before, @After, @BeforeAll, @AfterAll)
│   ├── BeforeHooks.java
│   └── AfterHooks.java
├── runners/                # JUnit Platform Suite runners
│   └── CucumberUiRunner.java
├── steps/                  # Cucumber step definition classes, grouped by domain
│   └── <domain>/           # e.g., login/, checkout/
└── helpers/                # Test-only utilities (test data builders, API shortcuts)

src/test/resources/
├── features/
│   └── <domain>/           # .feature files grouped by business domain
└── properties/
    └── application.properties
```

### Rules

- **Never place page objects in `src/test`**. Page objects are framework assets — they belong in `src/main/java/org/danceWithPlaywright/pages/`.
- **Never place Cucumber glue code (steps, hooks, runners, configuration) in `src/main`**. They belong in `src/test/java`.
- Step definition classes live under `org.danceWithPlaywright.steps` (the configured glue path).
- Feature files are organized by business domain under `src/test/resources/features/`.
- When creating a new domain area, create matching directories in `pages/<domain>/`, `steps/<domain>/`, and `features/<domain>/`.

---

## 3. Playwright Lifecycle & Browser Management

### Core Principles

1. **One `Playwright` instance per test scenario** — created in `@Before` hook, closed in `@After` hook.
2. **`UiTestContext`** is the single source of truth for the current `Playwright`, `Browser`, `BrowserContext`, and `Page` objects. It must be Spring-managed with **scenario scope** (prototype or custom scope).
3. **`PlaywrightFactory`** is a Spring `@Component` or `@Configuration` class responsible for constructing Playwright objects with the proper options.
4. **`PlayWrightManager`** orchestrates start/stop using `PlaywrightFactory` and populates `UiTestContext`.
5. **`BrowserOptions`** encapsulates `com.microsoft.playwright.BrowserType.LaunchOptions` configuration (headless mode, slowMo, args, proxy, downloads path). It reads values from `application.properties` via Spring `@Value` or `@ConfigurationProperties`.

### Browser Selection Pattern

```java
// Use the framework's own BrowserType enum — NOT com.microsoft.playwright.BrowserType directly in business logic
switch (browserTypeEnum) {
    case CHROMIUM -> playwright.chromium();
    case FIREFOX  -> playwright.firefox();
    case EDGE     -> playwright.chromium(); // Edge runs on Chromium channel "msedge"
}
```

- When the browser is `EDGE`, pass `new LaunchOptions().setChannel("msedge")`.
- Default browser is `CHROMIUM` unless overridden by property or environment variable.

### BrowserOptions Implementation Pattern

`BrowserOptions` must be a Spring `@Component` that:

1. Reads all browser-related configuration from `application.properties` using `@Value`.
2. Exposes a method like `buildLaunchOptions()` that returns a fully configured `com.microsoft.playwright.BrowserType.LaunchOptions`.
3. Handles the `EDGE` channel case transparently — callers should not need to know about channel logic.
4. Supports overriding values via system properties for CI flexibility.

```java
@Component
@Slf4j
public class BrowserOptions {
    @Value("${browser.headless:true}")
    private boolean headless;

    @Value("${browser.slow-mo:0}")
    private double slowMo;

    @Value("${browser.channel:}")
    private String channel;

    /**
     * Builds Playwright LaunchOptions from externalized configuration.
     * Uses fully qualified Playwright BrowserType to avoid collision with
     * the framework's own org.danceWithPlaywright.infrastructure.enums.SupportedBrowsers enum.
     */
    public com.microsoft.playwright.BrowserType.LaunchOptions buildLaunchOptions() {
        var options = new com.microsoft.playwright.BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(slowMo);
        if (channel != null && !channel.isBlank()) {
            options.setChannel(channel);
        }
        log.info("Browser launch options: headless={}, slowMo={}, channel={}", headless, slowMo, channel);
        return options;
    }
}
```

### BrowserContext Configuration Pattern

When creating a `BrowserContext`, always configure:

1. **Viewport** from properties (`browser.viewport.width`, `browser.viewport.height`).
2. **Base URL** from `app.base-url` so `page.navigate("/path")` resolves relative URLs automatically.
3. **Tracing** if `trace.enabled=true` — start tracing immediately after context creation.
4. **Default timeout** from `app.default-timeout`.

```java
Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
    .setViewportSize(viewportWidth, viewportHeight)
    .setBaseURL(baseUrl);
BrowserContext context = browser.newContext(contextOptions);
context.setDefaultTimeout(defaultTimeout);
```

### Mandatory Cleanup

- **Always** close resources in reverse order: `Page` → `BrowserContext` → `Browser` → `Playwright` in the `@After` hook.
- Use try/finally or try-with-resources patterns. Never rely on garbage collection.
- On test failure, capture a screenshot and attach it to the Cucumber scenario **before** closing the page.
- Each resource close should be wrapped individually so a failure to close one does not prevent closing others:

```java
// Correct teardown pattern
try {
    if (page != null) page.close();
} catch (Exception e) {
    log.warn("Failed to close page", e);
} finally {
    try {
        if (browserContext != null) browserContext.close();
    } catch (Exception e) {
        log.warn("Failed to close browser context", e);
    } finally {
        try {
            if (browser != null) browser.close();
        } catch (Exception e) {
            log.warn("Failed to close browser", e);
        } finally {
            if (playwright != null) playwright.close();
        }
    }
}
```

---

## 4. Page Object Model (POM) Conventions

### BasePage

Create a `BasePage` in `src/main/java/org/danceWithPlaywright/pages/base/BasePage.java`:

- Accepts `Page` via constructor injection (not field injection).
- Stores the `Page` reference as a `protected final` field so subclasses can access it directly when needed but cannot reassign it.
- Provides helper methods wrapping Playwright operations: `click`, `fill`, `getText`, `isVisible`, `waitForSelector`, `navigateTo`, `selectOption`, `screenshot`.
- All waits must use Playwright's **built-in auto-waiting** — do NOT add explicit `Thread.sleep()` anywhere.
- If a custom wait is truly necessary, use `page.waitForSelector()`, `page.waitForURL()`, `page.waitForResponse()`, or `Locator.waitFor()`.

```java
package org.danceWithPlaywright.pages.base;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BasePage {
    protected final Page page;

    protected BasePage(Page page) {
        this.page = page;
    }

    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        page.navigate(url);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    protected void click(String selector) {
        page.locator(selector).click();
    }

    protected void fill(String selector, String value) {
        page.locator(selector).fill(value);
    }

    protected String getText(String selector) {
        return page.locator(selector).textContent();
    }

    protected boolean isVisible(String selector) {
        return page.locator(selector).isVisible();
    }

    protected void waitForSelector(String selector) {
        page.locator(selector).waitFor();
    }

    protected void selectOption(String selector, String value) {
        page.locator(selector).selectOption(value);
    }

    protected byte[] screenshot() {
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
    }

    protected Locator locator(String selector) {
        return page.locator(selector);
    }
}
```

### Page Object Rules

1. **One class per page or significant UI component.** Name it `<PageName>Page.java` (e.g., `LoginPage.java`, `DashboardPage.java`).
2. **Locators** are `private static final` fields of type `String`, defined at the top of the class in a dedicated "Locators" section.
3. **Prefer Playwright's user-facing locators**: `page.getByRole()`, `page.getByText()`, `page.getByLabel()`, `page.getByPlaceholder()`, `page.getByTestId()`. Use CSS/XPath only as a last resort.
4. **Methods represent user actions**, not raw DOM operations. Name them as verbs: `login(user, pass)`, `addToCart(item)`, `getErrorMessage()`.
5. **Return types**: action methods return `void` or the next `Page Object` for fluent chaining. Query methods return `String`, `boolean`, `List<String>`, etc.
6. **No assertions in page objects.** Assertions belong exclusively in step definitions or dedicated assertion helpers.
7. **No Cucumber dependencies in page objects.** Page objects must be framework-agnostic.
8. **Use Lombok `@Getter` only when necessary**; avoid `@Setter` on page objects (they should be immutable after construction).
9. **Constructor signature**: always accept `Page` as the only constructor parameter and pass it to `super(page)`.
10. **Locator preference hierarchy** (use the first one that works reliably):
    - `getByRole()` — most resilient to DOM changes
    - `getByTestId()` — explicit and stable if the app supports `data-testid`
    - `getByLabel()` / `getByPlaceholder()` — good for form elements
    - `getByText()` — acceptable for unique, stable text
    - CSS selector — fallback when semantic locators are not viable
    - XPath — absolute last resort; never use positional XPath like `//div[3]/span[2]`

### Example Skeleton

```java
package org.danceWithPlaywright.pages.login;

import com.microsoft.playwright.Page;
import org.danceWithPlaywright.pages.base.BasePage;

public class LoginPage extends BasePage {
    // — Locators —
    private static final String USERNAME_INPUT = "[data-testid='username']";
    private static final String PASSWORD_INPUT = "[data-testid='password']";
    private static final String LOGIN_BUTTON   = "[data-testid='login-btn']";
    private static final String ERROR_MESSAGE  = "[data-testid='error-msg']";

    public LoginPage(Page page) {
        super(page);
    }

    public void navigateTo() {
        navigateTo("/login");
    }

    public void login(String username, String password) {
        fill(USERNAME_INPUT, username);
        fill(PASSWORD_INPUT, password);
        click(LOGIN_BUTTON);
    }

    public String getErrorMessage() {
        return getText(ERROR_MESSAGE);
    }

    public boolean isErrorMessageVisible() {
        return isVisible(ERROR_MESSAGE);
    }
}
```

---

## 5. Component Objects

For reusable UI fragments (navigation bar, sidebar, modals, data tables):

- Place in `src/main/java/org/danceWithPlaywright/components/`.
- Name as `<Component>Component.java` (e.g., `NavBarComponent.java`, `ConfirmationModalComponent.java`).
- Accept `Page` or parent `Locator` via constructor.
- Page objects **compose** component objects — never inherit from them.
- Components should extend `BasePage` only if they need the full page context; otherwise, accept a `Locator` representing their root element for better encapsulation.

### Component with Locator Root Pattern

```java
package org.danceWithPlaywright.components;

import com.microsoft.playwright.Locator;
import java.util.List;

public class DataTableComponent {
    private final Locator root;

    public DataTableComponent(Locator tableRoot) {
        this.root = tableRoot;
    }

    public List<String> getColumnValues(String columnName) {
        return root.locator("td." + columnName).allTextContents();
    }

    public int getRowCount() {
        return root.locator("tbody tr").count();
    }
}
```

### Composition in Page Objects

```java
public class DashboardPage extends BasePage {
    private final NavBarComponent navBar;

    public DashboardPage(Page page) {
        super(page);
        this.navBar = new NavBarComponent(page);
    }

    public NavBarComponent getNavBar() {
        return navBar;
    }
}
```

---

## 6. Cucumber / BDD Conventions

### Feature Files

- Written in **Gherkin**.
- File name: `snake_case.feature`.
- One `Feature` per file. Scenarios grouped by business behavior.
- Tag hierarchy: `@ui` (all UI tests), `@<domain>` (e.g., `@login`, `@checkout`), `@smoke`, `@regression`, `@wip`.
- Always include `@ui` on UI scenarios (the runner filters by it).
- Use `Scenario Outline` + `Examples` for data-driven tests.
- Steps must be **declarative and business-readable**. Avoid implementation details (no CSS selectors, no "click button" language).
- Use `Background` for common preconditions shared across all scenarios in a feature.
- Keep scenarios **independent** — each one must be runnable in isolation without depending on another scenario's side effects.

#### Good Gherkin Example

```gherkin
@ui @login
Feature: User login

  Background:
    Given the user is on the login page

  @smoke
  Scenario: Successful login with valid credentials
    When the user logs in with valid credentials
    Then the dashboard page is displayed

  @regression
  Scenario: Login fails with invalid password
    When the user logs in with an invalid password
    Then an authentication error message is displayed

  @regression
  Scenario Outline: Login validation messages
    When the user submits the login form with "<username>" and "<password>"
    Then the validation message "<message>" is displayed

    Examples:
      | username | password | message                              |
      |          | pass123  | Username is required                 |
      | user1    |          | Password is required                 |
      |          |          | Username and Password are required   |
```

#### Bad Gherkin — NEVER Write This

```gherkin
# ❌ Imperative, implementation-coupled steps
Scenario: Login
  Given I open the browser
  And I navigate to "https://example.com/login"
  When I type "admin" into the "#username" field
  And I type "pass" into the "#password" field
  And I click the "#login-btn" button
  Then the element "#dashboard-title" should contain "Welcome"
```

### Step Definitions

- One step definition class per domain: `LoginSteps.java`, `CheckoutSteps.java`.
- Place in `org.danceWithPlaywright.steps.<domain>`.
- Step classes are Spring-managed beans — use `@Autowired` or constructor injection.
- Inject `UiTestContext` (or `ScenarioContext`) to obtain the current `Page`.
- Instantiate page objects locally in steps using the `Page` from context. Do **not** make page objects Spring beans.
- Keep steps thin: delegate business logic to page objects, assertions to JUnit Jupiter / AssertJ.
- Use `@ParameterType` for custom Cucumber parameter converters.
- Step methods should be descriptive and match the Gherkin step exactly (no regex unless necessary).

#### Step Definition Pattern

```java
package org.danceWithPlaywright.steps.login;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.danceWithPlaywright.context.UiTestContext;
import org.danceWithPlaywright.pages.login.LoginPage;
import org.danceWithPlaywright.pages.dashboard.DashboardPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginSteps {

    @Autowired
    private UiTestContext uiTestContext;

    // Test credentials injected from application.properties — never hardcoded
    @Value("${test-data.valid-username}")
    private String validUsername;

    @Value("${test-data.valid-password}")
    private String validPassword;

    @Given("the user is on the login page")
    public void theUserIsOnTheLoginPage() {
        new LoginPage(uiTestContext.getPage()).navigateTo();
    }

    @When("the user logs in with valid credentials")
    public void theUserLogsInWithValidCredentials() {
        new LoginPage(uiTestContext.getPage()).login(validUsername, validPassword);
    }

    @Then("the dashboard page is displayed")
    public void theDashboardPageIsDisplayed() {
        assertTrue(new DashboardPage(uiTestContext.getPage()).isDashboardVisible());
    }
}
```

### Hooks

- **`BeforeHooks`**: annotated with `@Before`, initializes Playwright/Browser/Page via `PlayWrightManager`, stores in `UiTestContext`.
- **`AfterHooks`**: annotated with `@After`, captures screenshot on failure, tears down Playwright resources.
- Hooks must be in the glue package or a sub-package so Cucumber discovers them.
- Hooks should be Spring beans (annotated with `@Component` or within component-scanned packages).
- Use Cucumber's `@Before(order = N)` / `@After(order = N)` for ordering when multiple hooks exist.
- The glue path in `CucumberUiRunner` must include packages containing hooks. Currently the glue is `org.danceWithPlaywright.steps` — ensure hooks are discoverable by also including `org.danceWithPlaywright.hooks` or by broadening the glue to `org.danceWithPlaywright`.

#### Hook Example

> **Note:** The current `PlayWrightManager.start(String browserType)` accepts a browser type parameter and only holds `playwright` and `browser` fields (no getters, no `BrowserContext`/`Page`). Before using this pattern, `PlayWrightManager` must be enhanced to: (1) become a Spring `@Component`, (2) expose getters via Lombok `@Getter`, (3) create `BrowserContext` and `Page` internally, and (4) optionally read the browser type from `@Value` so the hook can call a no-arg `start()`. The target design:

```java
package org.danceWithPlaywright.hooks;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.extern.slf4j.Slf4j;
import org.danceWithPlaywright.context.UiTestContext;
import org.danceWithPlaywright.infrastructure.browser.PlayWrightManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BeforeHooks {

    private final PlayWrightManager playWrightManager;
    private final UiTestContext uiTestContext;

    // @Value cannot be used on final fields — inject via field, not constructor
    @Value("${browser.type:CHROMIUM}")
    private String browserType;

    @Autowired
    public BeforeHooks(PlayWrightManager playWrightManager, UiTestContext uiTestContext) {
        this.playWrightManager = playWrightManager;
        this.uiTestContext = uiTestContext;
    }

    @Before(order = 0)
    public void setUp(Scenario scenario) {
        log.info("Starting scenario: {}", scenario.getName());
        playWrightManager.start(browserType);
        // After PlayWrightManager is enhanced with @Getter and BrowserContext/Page creation:
        uiTestContext.setPlaywright(playWrightManager.getPlaywright());
        uiTestContext.setBrowser(playWrightManager.getBrowser());
        uiTestContext.setBrowserContext(playWrightManager.getBrowserContext());
        uiTestContext.setPage(playWrightManager.getPage());
    }
}
```

---

## 7. Spring Boot & Dependency Injection

- `AppConfig` is the `@SpringBootApplication` entry point, loading `application.properties`.
- `CucumberSpringBootConfig` bridges Cucumber to Spring with `@CucumberContextConfiguration` + `@SpringBootTest`.
- All infrastructure classes (`PlaywrightFactory`, `PlayWrightManager`, `BrowserOptions`) should be `@Component` / `@Service` / `@Configuration` and wired by Spring.
- `UiTestContext` and `ScenarioContext` should be Spring beans with **prototype** or **cucumber-glue** scope so each scenario gets a fresh instance.
- Use `@Value("${property.key}")` or `@ConfigurationProperties(prefix = "...")` to inject configuration values.
- Never use `new` to create Spring-managed dependencies — always inject them.

### Scope Configuration

```java
package org.danceWithPlaywright.configuration;

import org.danceWithPlaywright.context.ScenarioContext;
import org.danceWithPlaywright.context.UiTestContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ScopeConfig {

    @Bean
    @Scope("cucumber-glue") // Each Cucumber scenario gets a fresh instance
    public UiTestContext uiTestContext() {
        return new UiTestContext();
    }

    @Bean
    @Scope("cucumber-glue")
    public ScenarioContext scenarioContext() {
        return new ScenarioContext();
    }
}
```

### Component Scanning

`AppConfig` must scan both `src/main` and `src/test` packages. The current `AppConfig` uses `@SpringBootApplication` without `scanBasePackages`, which defaults to scanning only `org.danceWithPlaywright.configuration` and sub-packages — this will **not** discover `@Component` classes in `src/main` (e.g., `BrowserOptions`, `PlayWrightManager`). Add `scanBasePackages` to fix this:

```java
package org.danceWithPlaywright.configuration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@SpringBootApplication(scanBasePackages = "org.danceWithPlaywright")
@PropertySources({
    @PropertySource("classpath:properties/application.properties")
})
public class AppConfig {
}
```

---

## 8. Configuration & Properties

`application.properties` drives runtime behavior:

```properties
# Browser
browser.type=CHROMIUM
browser.headless=true
browser.slow-mo=0
browser.viewport.width=1920
browser.viewport.height=1080
browser.channel=

# Application
app.base-url=https://example.com
app.default-timeout=30000

# Traces & Screenshots
trace.enabled=false
screenshot.on-failure=true
screenshot.path=target/screenshots
```

- **All configurable values** (URLs, timeouts, browser options) go into `application.properties`. Never hardcode them.
- Support override via system properties / environment variables for CI.
- Use Spring's property resolution order: system properties > environment variables > `application.properties`.
- Use `:` syntax for default values in `@Value`: `@Value("${browser.headless:true}")`.

---

## 9. Code Style & Standards

### General

- **Java 17+** features encouraged: records, sealed classes, text blocks, pattern matching, switch expressions.
- **Immutability first**: prefer `final` fields, unmodifiable collections, records for DTOs.
- **Lombok**: Use `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j` where appropriate. Avoid `@Data` on mutable infrastructure classes.
- **Logging**: Use SLF4J (`@Slf4j` from Lombok). Log lifecycle events (browser start/stop, navigation, test status). Never use `System.out.println`.
- **Null safety**: Use `Optional` for potentially absent values. Never return `null` from public methods — throw a meaningful exception or return `Optional.empty()`.
- **No wildcard imports**: Always use explicit imports.
- **Access modifiers**: Default to `private`, expose only what is necessary. Package-private for test helpers.

### Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Page object class | `<PageName>Page` | `LoginPage`, `DashboardPage` |
| Component class | `<Name>Component` | `NavBarComponent` |
| Step definition class | `<Domain>Steps` | `LoginSteps` |
| Feature file | `snake_case.feature` | `user_logs_in.feature` |
| Step methods | camelCase, descriptive | `theUserLogsInWithValidCredentials()` |
| Enum values | UPPER_SNAKE_CASE | `CHROMIUM`, `FIREFOX` |
| Properties | dot.separated.kebab | `browser.slow-mo` |
| Constants (locators) | UPPER_SNAKE_CASE | `LOGIN_BUTTON` |
| Test data builder | `<Entity>Builder` | `UserBuilder` |
| Custom exception | `<Descriptive>Exception` | `PageNavigationException` |
| Utility class | `<Purpose>Utils` | `ScreenshotUtils` |

### Method Organization Within a Class

Maintain consistent ordering inside every class:

1. Static final constants (locators for page objects)
2. Instance fields
3. Constructor(s)
4. Public methods (business actions / API)
5. Protected methods (template/hook methods)
6. Private methods (internal helpers)

---

## 10. Error Handling & Resilience

- Wrap Playwright calls that may fail with meaningful custom exceptions (e.g., `PageNavigationException`, `ElementNotFoundException`).
- Custom exceptions extend `RuntimeException` and live in `infrastructure/exceptions/`.
- On test failure in `@After` hook: capture screenshot, capture page URL, capture console logs, attach all to Cucumber scenario via `scenario.attach(...)`.
- Never silently swallow exceptions. Log and re-throw with context.

### Custom Exception Pattern

```java
package org.danceWithPlaywright.infrastructure.exceptions;

public class PageNavigationException extends RuntimeException {
    public PageNavigationException(String url, Throwable cause) {
        super("Failed to navigate to: " + url, cause);
    }
}
```

### Failure Attachment Pattern

```java
package org.danceWithPlaywright.hooks;

import com.microsoft.playwright.Page;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.danceWithPlaywright.context.UiTestContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AfterHooks {

    private final UiTestContext uiTestContext;

    @After(order = 100) // High order = runs early in the @After chain
    public void captureFailureEvidence(Scenario scenario) {
        if (scenario.isFailed() && uiTestContext.getPage() != null) {
            Page page = uiTestContext.getPage();
            // Screenshot
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            scenario.attach(screenshot, "image/png", "failure-screenshot");
            // Page URL
            scenario.attach(page.url().getBytes(), "text/plain", "failure-url");
            log.error("Scenario '{}' failed on URL: {}", scenario.getName(), page.url());
        }
    }
}
```

---

## 11. Traces, Screenshots & Reporting

- Support Playwright **tracing** (`browserContext.tracing().start()` / `.stop()`) toggled by `trace.enabled` property.
- Screenshots: use `page.screenshot()` with full-page option. Save to `target/screenshots/` with scenario-name timestamp.
- Attach screenshots and traces to Cucumber reports using `scenario.attach(byte[], mediaType, name)`.
- Cucumber plugins configured in runner: `pretty`, `summary`, `html:target/cucumber-reports/report.html`, `json:target/cucumber-reports/report.json`.

### Tracing Pattern

```java
// Required imports for tracing pattern:
// import com.microsoft.playwright.Tracing;
// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Paths;

// In BeforeHooks — after creating BrowserContext
if (traceEnabled) {
    browserContext.tracing().start(new Tracing.StartOptions()
        .setScreenshots(true)
        .setSnapshots(true)
        .setSources(false));
}

// In AfterHooks — before closing BrowserContext
if (traceEnabled) {
    String tracePath = String.format("target/traces/%s-%d.zip",
        scenario.getName().replaceAll("\\s+", "_"),
        System.currentTimeMillis());
    browserContext.tracing().stop(new Tracing.StopOptions()
        .setPath(Paths.get(tracePath)));
    try {
        scenario.attach(Files.readAllBytes(Paths.get(tracePath)), "application/zip", "trace");
    } catch (IOException e) {
        log.warn("Failed to attach trace file: {}", tracePath, e);
    }
}
```

---

## 12. ScenarioContext Usage Patterns

`ScenarioContext` is a scenario-scoped container for sharing state between step definition classes within the same scenario. Use it to pass data across domains without coupling step classes directly.

### Rules

1. Use `ScenarioContext` only for **cross-domain** data sharing (e.g., an order ID created in checkout steps and verified in confirmation steps).
2. Store values as typed entries — avoid raw `Map<String, Object>`. Use dedicated fields or a type-safe container.
3. Never use `ScenarioContext` as a dumping ground for everything. If data is only used within a single step class, keep it local.

### Pattern

> **Note:** The current `ScenarioContext` class only has a `private UiTestContext uiEnvironmentManager` field with no annotations, no Spring wiring, and no getters/setters. It must be refactored to match the target design below — adding Lombok annotations and Spring scope configuration. The `uiEnvironmentManager` field should be removed since `UiTestContext` is already its own separate bean.

```java
package org.danceWithPlaywright.context;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Scope("cucumber-glue")
@Component
public class ScenarioContext {
    private String currentUsername;
    private String lastErrorMessage;
    private Map<String, String> customData = new HashMap<>();
}
```

---

## 13. Test Data Management

### Principles

1. **No hardcoded credentials or test data in step definitions**. Use properties, test data builders, or data files.
2. For simple data: use `application.properties` with a `test-data.*` prefix.
3. For complex data: create builder classes in `src/test/java/org/danceWithPlaywright/helpers/`.
4. For data-driven tests: use Cucumber `Examples` tables.
5. For large data sets: use JSON/CSV files under `src/test/resources/testdata/` and load via utility.

### Builder Pattern

```java
package org.danceWithPlaywright.helpers;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserTestData {
    @Builder.Default
    private String username = "testuser";
    @Builder.Default
    private String password = "TestPass123!";
    @Builder.Default
    private String email = "testuser@example.com";
}
```

---

## 14. API Shortcut Helpers (Test Acceleration)

For tests that need pre-conditions set up via API (e.g., create a user, seed data) before the UI scenario:

- Place in `src/test/java/org/danceWithPlaywright/helpers/`.
- Use Playwright's built-in `APIRequestContext` for HTTP calls — do NOT add RestAssured or other HTTP libraries unless explicitly requested.
- Call these from `@Before` hooks or `Given` steps.
- Always clean up API-created data in `@After` hooks.

```java
package org.danceWithPlaywright.helpers;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.options.RequestOptions;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Slf4j
public class ApiHelper {
    private final APIRequestContext request;

    public ApiHelper(APIRequestContext request) {
        this.request = request;
    }

    public String createTestUser(String username) {
        var response = request.post("/api/users",
            RequestOptions.create().setData(Map.of("username", username)));
        log.info("Created test user: {} — status: {}", username, response.status());
        return response.text();
    }
}
```

---

## 15. Anti-Patterns — NEVER Do These

1. **`Thread.sleep()`** — Use Playwright's auto-waiting or explicit wait APIs.
2. **Hardcoded URLs, credentials, or selectors in step definitions** — Use properties and page objects.
3. **Assertions in page objects** — Page objects return data; steps assert.
4. **God step definition classes** — Split by domain. One class per business area.
5. **Static mutable state** — Use Spring-scoped beans and `UiTestContext`.
6. **Sharing browser/page across scenarios** — Each scenario gets a fresh context.
7. **Catching and ignoring exceptions** — Always log, re-throw, or handle meaningfully.
8. **Raw `Playwright` API calls in step definitions** — Always go through page objects.
9. **Imperative Gherkin** (e.g., "Click the button", "Enter text in field") — Write declarative, behavior-focused steps.
10. **Storing page objects as Spring beans** — Create them locally in steps using `Page` from `UiTestContext`.
11. **Positional XPath** (e.g., `//div[3]/span[2]`) — Extremely brittle. Use semantic locators.
12. **Multiple assertions per step** — Each `Then` step should assert one behavior. Multiple related checks are acceptable if they verify the same logical thing.
13. **Returning `null` from page object methods** — Throw `ElementNotFoundException` or return `Optional`.
14. **Using `@Data` on `UiTestContext`** — Use explicit `@Getter`/`@Setter` to control mutability.
15. **Creating Playwright objects outside `PlayWrightManager`** — All lifecycle management goes through the manager.

---

## 16. Testing Guidance for Copilot

When generating **new test scenarios or step definitions**:

1. Ask: *"Which domain does this belong to?"* — create files in the matching `<domain>/` folder.
2. Ask: *"Does a page object already exist for this page?"* — reuse it or create a new one.
3. Always add `@ui` tag to UI scenarios.
4. Always inject `UiTestContext` into step classes and obtain `Page` from it.
5. Always instantiate page objects as local variables: `var loginPage = new LoginPage(uiTestContext.getPage());`.
6. Write steps that are reusable and composable across scenarios.
7. Validate that the Cucumber runner's glue path covers the new step class package.

When generating **infrastructure code**:

1. Make it a Spring bean (`@Component`, `@Service`, `@Configuration`).
2. Use constructor injection (with `@RequiredArgsConstructor` from Lombok).
3. Externalize all magic values to `application.properties`.
4. Add SLF4J logging for lifecycle events.
5. Write Javadoc for public APIs.

When generating **page objects**:

1. Extend `BasePage`.
2. Accept only `Page` in constructor.
3. Use `private static final String` for all locators.
4. Name methods as user actions (verbs), not DOM operations.
5. Return data for verification — never assert.
6. Compose components, never inherit them.
7. Keep the class focused — one page = one class.

---

## 17. File Generation Checklist

When asked to create a new feature end-to-end, generate **all** of the following:

- [ ] Feature file: `src/test/resources/features/<domain>/<feature>.feature`
- [ ] Step definitions: `src/test/java/org/danceWithPlaywright/steps/<domain>/<Domain>Steps.java`
- [ ] Page object(s): `src/main/java/org/danceWithPlaywright/pages/<domain>/<PageName>Page.java`
- [ ] Component objects (if new reusable UI fragments): `src/main/java/org/danceWithPlaywright/components/<Name>Component.java`
- [ ] Any new properties in `application.properties`
- [ ] Update `CucumberUiRunner` glue/tag configuration if the new domain requires it.

---

## 18. Dependencies & Build

- **Never add a dependency without specifying its version** (use `<properties>` for version management).
- **`cucumber-java` must have `<scope>test</scope>`** — the current `pom.xml` declares it as `compile` scope, which incorrectly makes Cucumber annotations available in `src/main`. Fix this by changing the scope to `test`. Page objects and infrastructure code must never depend on Cucumber.
- **Playwright browsers must be installed** before first run: `mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"`.
- **Maven Surefire/Failsafe**: Configure Failsafe for integration tests if separating unit from UI tests.
- Respect the existing dependency versions in `pom.xml`. Do not upgrade without explicit request.

---

## 19. CI/CD Considerations

- All tests must be runnable in **headless mode** by default (`browser.headless=true`).
- Browser type and base URL must be overridable via system properties: `-Dbrowser.type=FIREFOX -Dapp.base-url=https://staging.example.com`.
- Test results (Cucumber JSON/HTML reports, screenshots, traces) must be written under `target/` for CI artifact collection.
- Exit code must reflect test pass/fail status.

---

## 20. Decision Trees for Copilot

### "Where does this file go?"

```
Is it a page object?
  └─ YES → src/main/java/org/danceWithPlaywright/pages/<domain>/
Is it a reusable UI component?
  └─ YES → src/main/java/org/danceWithPlaywright/components/
Is it infrastructure (browser, config, utils)?
  └─ YES → src/main/java/org/danceWithPlaywright/infrastructure/<subpackage>/
Is it a step definition?
  └─ YES → src/test/java/org/danceWithPlaywright/steps/<domain>/
Is it a hook?
  └─ YES → src/test/java/org/danceWithPlaywright/hooks/
Is it a test helper or builder?
  └─ YES → src/test/java/org/danceWithPlaywright/helpers/
Is it a feature file?
  └─ YES → src/test/resources/features/<domain>/
Is it a data model / DTO?
  └─ YES → src/main/java/org/danceWithPlaywright/models/
Is it a custom exception?
  └─ YES → src/main/java/org/danceWithPlaywright/infrastructure/exceptions/
```

### "Should I create a new class or modify an existing one?"

```
Does a page object for this page already exist?
  └─ YES → Add methods to the existing page object.
  └─ NO  → Create a new page object in the correct domain package.

Does a step class for this domain already exist?
  └─ YES → Add steps to the existing class.
  └─ NO  → Create a new <Domain>Steps.java.

Is this a new UI component used on multiple pages?
  └─ YES → Create a Component class.
  └─ NO  → Keep it as private methods in the page object that uses it.
```

### "Which locator strategy should I use?"

```
Does the element have a data-testid?
  └─ YES → page.getByTestId("...")
  └─ NO  ↓

Does the element have a clear accessible role (button, link, heading)?
  └─ YES → page.getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName("..."))
  └─ NO  ↓

Is it a form element with a visible label?
  └─ YES → page.getByLabel("...")
  └─ NO  ↓

Does it have a placeholder?
  └─ YES → page.getByPlaceholder("...")
  └─ NO  ↓

Does it have unique, stable visible text?
  └─ YES → page.getByText("...")
  └─ NO  ↓

Use a CSS selector as last resort.
  └─ NEVER use positional XPath.
```

---

*These instructions ensure every piece of code Copilot generates aligns with the DanceWithPlaywright framework's architecture, conventions, and quality standards.*
