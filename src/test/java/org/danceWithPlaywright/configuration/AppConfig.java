package org.danceWithPlaywright.configuration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@PropertySources ({
        @PropertySource("classpath:properties/application.properties")
})
@SpringBootApplication(scanBasePackages = "org.danceWithPlaywright")
public class AppConfig {
}