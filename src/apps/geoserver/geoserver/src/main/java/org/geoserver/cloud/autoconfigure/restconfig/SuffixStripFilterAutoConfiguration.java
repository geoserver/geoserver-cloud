/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.restconfig;

import org.geoserver.rest.SuffixStripFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * Provides the {@code suffixStripFilter} bean shared by the REST configuration and GWC services, replacing the
 * conflicting per-application definitions the two services used to declare independently.
 */
@AutoConfiguration
@Conditional(OnRestOrGwc.class)
public class SuffixStripFilterAutoConfiguration {

    @Bean
    SuffixStripFilter suffixStripFilter(ApplicationContext appContext) {
        return new UnifiedSuffixStripFilter(appContext);
    }
}
