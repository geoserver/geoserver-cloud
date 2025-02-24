/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.extension.importer;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = true)
@ImportFilteredResource({
    "jar:gs-importer-core-.*!/applicationContext.xml",
    "jar:gs-importer-web-.*!/applicationContext.xml"
})
public class ImporterConfiguration {}
