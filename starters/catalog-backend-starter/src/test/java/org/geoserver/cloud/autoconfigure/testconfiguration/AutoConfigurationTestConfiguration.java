package org.geoserver.cloud.autoconfigure.testconfiguration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration(
    exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class}
)
public class AutoConfigurationTestConfiguration {}
