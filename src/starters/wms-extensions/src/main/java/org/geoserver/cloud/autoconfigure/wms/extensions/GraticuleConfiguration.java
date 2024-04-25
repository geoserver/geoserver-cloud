/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */

package org.geoserver.cloud.autoconfigure.wms.extensions;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
public class GraticuleConfiguration {

    @Configuration
    @ConditionalOnProperty(
            prefix = "geoserver.wms.graticule",
            name = "enabled",
            matchIfMissing = true)
    @ImportResource( //
            reader = FilteringXmlBeanDefinitionReader.class, //
            locations = {"jar:gs-graticule-.*!/applicationContext.xml"})
    static class Enabled {}

    @Configuration
    @ConditionalOnProperty(
            prefix = "geoserver.wms.graticule",
            name = "enabled",
            havingValue = "false")
    static class Disabled {

        @Bean
        ModuleStatus graticuleDisabledModuleStatus() {
            ModuleStatusImpl mod = new ModuleStatusImpl();
            mod.setAvailable(true);
            mod.setEnabled(false);
            mod.setMessage(
                    "Graticule module disabled through config property geoserver.wms.graticule.enabled=false");
            mod.setComponent("GeoServer Graticule");
            mod.setModule("gs-graticule");
            mod.setName("Graticule");
            return mod;
        }
    }
}
