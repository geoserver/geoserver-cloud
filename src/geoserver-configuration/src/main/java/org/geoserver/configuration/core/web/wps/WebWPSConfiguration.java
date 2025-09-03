/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.web.wps;

import org.geoserver.configuration.core.web.demo.WCSRequestBuilderConfiguration;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Imports the transpiled configuration from {@code gs-web-wps}, except for the {@code wpsRequestBuilder}
 * bean, which is transpiled to its own configuration class {@link WCSRequestBuilderConfiguration} to allow
 * disabling it in {@code WebDemosAutoConfiguration}.
 *
 * @see WebWPSConfiguration_Generated
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-web-wps-.*!/applicationContext.xml",
        excludes = {"wpsRequestBuilder"})
@Import(WebWPSConfiguration_Generated.class)
public class WebWPSConfiguration {}
