package org.danceWithPlaywright.configuration;

import org.danceWithPlaywright.context.ScenarioContext;
import org.danceWithPlaywright.context.UiTestContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/***Defines Spring bean scopes for test context objects.
 * * "cucumber-glue" scope = one fresh instance per Cucumber scenario.
 * * This guarantees complete isolation — no shared state between tests.
 * * Why not @Scope on the class directly? * UiTestContext and ScenarioContext live in src/test and are not
 * * @Component-annotated classes (they're plain POJOs).
 * * We declare them as @Bean here so Spring manages their lifecycle.
 ***/
@Configuration
public class ScopeConfig {

    @Bean
    @Scope("cucumber-glue")
    public UiTestContext uiTestContext() {
        return new UiTestContext();
    }

    @Bean
    @Scope("cucumber-glue")
    public ScenarioContext scenarioContext() {
        return new ScenarioContext();
    }
}
