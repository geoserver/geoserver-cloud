/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms.extensions;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

/**
 * @since 1.0
 */
@Configuration
@Import(
        value = {
            MapBoxStylingConfiguration.Enabled.class,
            MapBoxStylingConfiguration.Disabled.class
        })
class MapBoxStylingConfiguration {

    @Configuration
    @ConditionalOnBean(name = "sldHandler") // sldHandler is MBStyleHandler's constructor arg
    @ConditionalOnProperty(
            name = "geoserver.styling.mapbox.enabled",
            havingValue = "true",
            matchIfMissing = true)
    @ConditionalOnClass(MBStyleHandler.class)
    @ImportResource( //
            reader = FilteringXmlBeanDefinitionReader.class, //
            locations = {"jar:gs-mbstyle-.*!/applicationContext.xml"})
    static class Enabled {}

    @Configuration
    @ConditionalOnProperty(
            name = "geoserver.styling.mapbox.enabled",
            havingValue = "false",
            matchIfMissing = false)
    @ConditionalOnClass(MBStyleHandler.class)
    static class Disabled {

        public @Bean(name = "MBStyleExtension") ModuleStatus mbStyleDisabledModuleStatus() {
            ModuleStatusImpl mod = new ModuleStatusImpl();
            mod.setAvailable(true);
            mod.setEnabled(false);
            mod.setMessage(
                    "MapBox Styling module disabled through config property geoserver.styling.mapbox.enabled=false");
            mod.setComponent("MBStyle plugin");
            mod.setModule("gs-mbstyle");
            mod.setName("MBStyle Extension");
            Version v = GeoTools.getVersion(MBStyleHandler.class);
            mod.setVersion(v == null ? null : v.toString());
            return mod;
        }
    }
}
