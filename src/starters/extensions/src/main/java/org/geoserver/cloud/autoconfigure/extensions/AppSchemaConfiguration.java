/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @since 1.0
 */
@Configuration
@Import(
        value = {
            AppSchemaConfiguration.EnabledWebUI.class,
            AppSchemaConfiguration.EnabledWFS.class,
            AppSchemaConfiguration.Disabled.class
        })
class AppSchemaConfiguration {

    @Configuration
    @ConditionalOnBean(name = "geoServer")
    @ConditionalOnClass(name = "org.geoserver.cloud.web.app.WebUIApplication")
    @ConditionalOnProperty(name = "geoserver.extension.appschema.enabled", havingValue = "true", matchIfMissing = false)
    @ImportFilteredResource("jar:gs-app-schema-core-.*!/applicationContext.xml#name=(appSchemaExtension)")
    static class EnabledWebUI {}

    @Configuration
    @ConditionalOnClass(name = "org.geoserver.cloud.wfs.app.WfsApplication")
    @ConditionalOnProperty(name = "geoserver.extension.appschema.enabled", havingValue = "true", matchIfMissing = false)
    @ComponentScan(basePackages = "org.geoserver.complex")
    static class EnabledWFS {}

    @Configuration
    @ConditionalOnBean(name = "geoServer")
    @ConditionalOnProperty(
            name = "geoserver.extension.appschema.enabled",
            havingValue = "false",
            matchIfMissing = false)
    static class Disabled {

        // the app-schema extension still works as the geotools is
        // not using Spring and classes are still on classpath
        // TODO "really" disable the geotools data factory SPI mechanism? if possible?

        @Bean
        ModuleStatus appSchemaDisabledModuleStatus() {
            ModuleStatusImpl mod = new ModuleStatusImpl(
                    "gs-app-schema-core", "App Schema Core Extension", "App Schema Core extension");
            mod.setEnabled(false);
            mod.setMessage(
                    "App schema extension disabled through config property geoserver.extension.appschema.enabled=false");
            return mod;
        }
    }
}
