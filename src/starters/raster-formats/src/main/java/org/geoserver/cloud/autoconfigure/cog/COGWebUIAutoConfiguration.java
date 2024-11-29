/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.cog;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.store.cog.panel.CogRasterEditPanel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/** Auto configuration to enable the COG customized store panel when the web-ui is present. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({GeoServerApplication.class, CogRasterEditPanel.class})
@ImportResource(
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = {"jar:gs-cog-.*!/applicationContext.xml#name=" + COGWebUIAutoConfiguration.INCLUDE_WEBUI_BEANS})
public class COGWebUIAutoConfiguration {

    static final String WEBUI_BEAN_NAMES = "COGGeoTIFFExclusionFilter|CogGeotiffStorePanel|CogSettingsPanel";

    static final String INCLUDE_WEBUI_BEANS = "^(" + WEBUI_BEAN_NAMES + ").*$";
}
