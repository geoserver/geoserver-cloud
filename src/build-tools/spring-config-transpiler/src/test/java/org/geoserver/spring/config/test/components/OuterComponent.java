/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.test.components;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/** Component with a nested @Configuration class for testing that member classes are skipped by GENERATE mode. */
@Component
public class OuterComponent {

    @Configuration
    static class InnerConfiguration {
        @Bean
        String innerBean() {
            return "inner";
        }
    }
}
