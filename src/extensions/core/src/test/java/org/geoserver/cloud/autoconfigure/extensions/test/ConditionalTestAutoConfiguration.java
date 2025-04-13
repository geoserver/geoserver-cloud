/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.test;

import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerREST;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWCS;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWFS;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWMS;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWPS;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test auto-configuration that registers beans conditionally for each GeoServer service.
 *
 * <p>
 * This auto-configuration is intended for testing the conditional annotations
 * in each service application. It creates simple marker beans that should
 * only be registered in their corresponding service context.
 *
 * <p>
 * To use this in a service application test:
 *
 * <pre>
 * &#64;SpringBootTest
 * public class ConditionalActivationTest {
 *
 *   &#64;Autowired
 *   private ApplicationContext context;
 *
 *   &#64;Test
 *   void verifyOnlyRightConditionalBeanExists() {
 *     // This should exist in this service
 *     assertThat(context.containsBean("wmsConditionalBean")).isTrue();
 *
 *     // These should not exist in this service
 *     assertThat(context.containsBean("wfsConditionalBean")).isFalse();
 *     assertThat(context.containsBean("wcsConditionalBean")).isFalse();
 *     // etc.
 *   }
 * }
 * </pre>
 */
@AutoConfiguration
public class ConditionalTestAutoConfiguration {

    /**
     * Bean that should only be created in WMS service applications.
     */
    @Bean("wmsConditionalBean")
    @ConditionalOnGeoServerWMS
    ConditionalTestBean wmsConditionalBean() {
        return new ConditionalTestBean("WMS");
    }

    /**
     * Bean that should only be created in WFS service applications.
     */
    @Bean("wfsConditionalBean")
    @ConditionalOnGeoServerWFS
    ConditionalTestBean wfsConditionalBean() {
        return new ConditionalTestBean("WFS");
    }

    /**
     * Bean that should only be created in WCS service applications.
     */
    @Bean("wcsConditionalBean")
    @ConditionalOnGeoServerWCS
    ConditionalTestBean wcsConditionalBean() {
        return new ConditionalTestBean("WCS");
    }

    /**
     * Bean that should only be created in WPS service applications.
     */
    @Bean("wpsConditionalBean")
    @ConditionalOnGeoServerWPS
    ConditionalTestBean wpsConditionalBean() {
        return new ConditionalTestBean("WPS");
    }

    /**
     * Bean that should only be created in REST service applications.
     */
    @Bean("restConditionalBean")
    @ConditionalOnGeoServerREST
    ConditionalTestBean restConditionalBean() {
        return new ConditionalTestBean("REST");
    }

    /**
     * Bean that should only be created in Web UI applications.
     */
    @Bean("webUiConditionalBean")
    @ConditionalOnGeoServerWebUI
    ConditionalTestBean webUiConditionalBean() {
        return new ConditionalTestBean("WebUI");
    }

    /**
     * Simple marker bean for conditional testing.
     */
    public static class ConditionalTestBean {
        private final String serviceName;

        public ConditionalTestBean(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getServiceName() {
            return serviceName;
        }

        @Override
        public String toString() {
            return "ConditionalTestBean[" + serviceName + "]";
        }
    }
}
