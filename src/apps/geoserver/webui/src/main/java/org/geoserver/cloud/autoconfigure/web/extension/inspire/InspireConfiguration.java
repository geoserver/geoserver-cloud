/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.extension.inspire;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = true)
@ImportFilteredResource({ //
    "jar:gs-inspire-.*!/applicationContext.xml#name=^(inspire.*AdminPanel|InsiperExtension|languageCallback|inspireDirManager).*$" //
})
public class InspireConfiguration {}
