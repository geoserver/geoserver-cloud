/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms.extensions;

import org.apache.batik.svggen.StyleHandler;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.community.css.web.CssHandler;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geotools.styling.css.CssParser;
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
@Import(value = {CssStylingConfiguration.Enabled.class, CssStylingConfiguration.Disabled.class})
class CssStylingConfiguration {

    @Configuration
    @ConditionalOnBean(name = "sldHandler")
    @ConditionalOnProperty(name = "geoserver.styling.css.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(CssHandler.class)
    @ImportResource( //
            reader = FilteringXmlBeanDefinitionReader.class, //
            locations = {"jar:gs-css-.*!/applicationContext.xml"})
    static class Enabled {}

    /**
     * {@link CssHandler} is both a {@link StyleHandler} and a {@link ModuleStatus}. This config
     * engages when css is disabled and provides a {@link ModuleStatus} with {@link
     * ModuleStatus#isEnabled() == false}
     *
     * @since 1.0
     */
    @Configuration
    @ConditionalOnBean(name = "sldHandler")
    @ConditionalOnProperty(name = "geoserver.styling.css.enabled", havingValue = "false", matchIfMissing = false)
    static class Disabled {

        @Bean
        ModuleStatus cssDisabledModuleStatus() {
            ModuleStatusImpl mod = new ModuleStatusImpl();
            mod.setAvailable(true);
            mod.setEnabled(false);
            mod.setMessage("CSS module disabled through config property geoserver.styling.css.enabled=false");
            mod.setComponent("GeoServer CSS Styling");
            mod.setModule("gs-css");
            mod.setName("CSS");
            Version v = GeoTools.getVersion(CssParser.class);
            mod.setVersion(v == null ? null : v.toString());
            return mod;
        }
    }
}
