/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.servlet;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.core.GeoServerMainAutoConfiguration;
import org.geoserver.cloud.config.servlet.GeoServerServletContextConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = GeoServerMainAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnProperty(prefix = "geoserver.servlet", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(GeoServerServletContextConfiguration.class)
@Slf4j
public class GeoServerServletContextAutoConfiguration {

    @AutoConfiguration
    @SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
    @ConditionalOnProperty(
            prefix = "geoserver.servlet",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = false)
    public static class Disabled {
        public @PostConstruct void log() {
            log.info("GeoServer servlet-context auto-configuration disabled explicitly through config property");
        }
    }
}
