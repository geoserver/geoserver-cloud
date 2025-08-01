/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.rest;

import org.geoserver.configuration.core.web.WebRestConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @see WebRestConfiguration
 */
@Configuration(proxyBeanMethods = false)
@Import(WebRestConfiguration.class)
public class WebRestAutoConfiguration {}
