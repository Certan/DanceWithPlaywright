package org.danceWithPlaywright.configuration;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest(classes = AppConfig.class)
public class CucumberSpringBootConfig {
}
