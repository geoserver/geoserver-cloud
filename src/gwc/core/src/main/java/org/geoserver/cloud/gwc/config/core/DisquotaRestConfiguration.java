/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.core;

import org.geowebcache.diskquota.rest.controller.DiskQuotaController;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Enables disk quota REST API if both {@link ConditionalOnDiskQuotaEnabled disk-quota} and {@link
 * ConditionalOnGeoWebCacheRestConfigEnabled res-config} are enabled.
 */
@Configuration
@ComponentScan(basePackageClasses = DiskQuotaController.class)
public class DisquotaRestConfiguration {}
