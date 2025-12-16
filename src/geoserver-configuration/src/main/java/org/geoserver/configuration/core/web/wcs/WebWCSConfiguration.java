/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.web.wcs;

import org.geoserver.configuration.core.web.demo.WPSRequestBuilderConfiguration;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Imports the transpiled configuration from {@code gs-web-wcs}, except for the {@code wcsRequestBuilder}
 * bean, which is transpiled to its own configuration class {@link WPSRequestBuilderConfiguration} to allow
 * disabling it in {@code WebDemosAutoConfiguration}.
 *
 * @see WebWCSConfiguration_Generated
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-web-wcs-.*!/applicationContext.xml",
        excludes = {"wcsRequestBuilder"})
@Import(WebWCSConfiguration_Generated.class)
public class WebWCSConfiguration {}
