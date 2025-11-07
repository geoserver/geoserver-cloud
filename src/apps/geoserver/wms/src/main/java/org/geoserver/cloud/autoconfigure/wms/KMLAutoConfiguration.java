/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.wms;

import org.geoserver.cloud.wms.controller.kml.KMLIconsController;
import org.geoserver.cloud.wms.controller.kml.KMLReflectorController;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.configuration.core.kml.KMLConfiguration;
import org.geoserver.ows.Dispatcher;
import org.geoserver.wms.icons.IconService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @since 1.0
 * @see KMLConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "geoserver.wms.kml.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(MBStyleHandler.class)
@Import(KMLConfiguration.class)
public class KMLAutoConfiguration {

    @Bean
    KMLIconsController kmlIconsController(@Qualifier("kmlIconService") IconService kmlIconService) {
        return new KMLIconsController(kmlIconService);
    }

    @Bean
    KMLReflectorController kmlReflectorController(Dispatcher geoserverDispatcher) {
        return new KMLReflectorController(geoserverDispatcher);
    }
}
