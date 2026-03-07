/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.security;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.catalog.backend.core.GeoServerBackendAutoConfiguration;
import org.geoserver.cloud.security.GeoServerSecurityConfiguration;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = GeoServerBackendAutoConfiguration.class)
@Import(GeoServerSecurityConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.security")
public class GeoServerSecurityAutoConfiguration {

    /** @since 1.3, required since geoserver 2.23.2 */
    @Bean
    @ConditionalOnMissingBean
    XStreamPersisterFactory xstreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }
}
