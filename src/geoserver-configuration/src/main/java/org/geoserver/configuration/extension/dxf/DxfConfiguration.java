/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.dxf;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportFilteredResource("jar:gs-dxf-core-.*!/applicationContext.xml#name=.*")
public class DxfConfiguration {}
