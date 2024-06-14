/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.extension;

import org.geoserver.cloud.autoconfigure.web.extension.graticule.GraticuleAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.extension.importer.ImporterAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    ImporterAutoConfiguration.class, //
    GraticuleAutoConfiguration.class
})
public class ExtensionsAutoConfiguration {}
