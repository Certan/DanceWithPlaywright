package org.danceWithPlaywright.runners;


import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.*;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.danceWithPlaywright")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@runUi")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty,summary,html:target/cucumber-reports/report.html,json:target/cucumber-reports/report.json,junit:target/cucumber-reports/report.xml,io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        )
public class CucumberUiRunnerTest {
}
