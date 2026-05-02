/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.core;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * Enables disk quota Wicket components.
 *
 * @since 2.28.3.1
 */
@Configuration
@ImportFilteredResource("jar:gs-web-gwc-.*!/applicationContext.xml#name=diskQuotaMenuPage")
public class DisquotaWebUIConfiguration {}
