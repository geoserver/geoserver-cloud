/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.restconfig;

import org.geoserver.rest.RestConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * Registers GeoServer's {@link RestConfiguration} early enough for Spring Boot to hand Spring MVC over to it.
 *
 * <p>{@link RestConfiguration} is a {@link WebMvcConfigurationSupport} subclass, the equivalent of
 * {@code @EnableWebMvc}: it owns the message converters that render REST responses (XStream, Freemarker, the style
 * readers and writers) and GeoServer's suffix-aware handler mapping, which resolves path extensions such as
 * {@code /rest/styles/line.json}. Spring supports a single {@code WebMvcConfigurationSupport} per application context,
 * and {@link WebMvcAutoConfiguration} declares {@code @ConditionalOnMissingBean(WebMvcConfigurationSupport.class)} to
 * step aside for one. That condition is evaluated very early, hence the explicit ordering: without it Spring Boot's own
 * MVC configuration claims the handler adapter and the handler mapping, and GeoServer's converters never reach them.
 *
 * <p>Both the REST configuration service and GWC answer over this infrastructure, hence the shared condition. Any
 * further configuration needing {@link RestConfiguration} should let this class register it rather than importing it
 * directly, which would reintroduce the ordering problem.
 */
@AutoConfiguration(before = WebMvcAutoConfiguration.class)
@Conditional(OnRestOrGwc.class)
@Import(RestConfiguration.class)
public class GeoServerRestFrameworkAutoConfiguration {}
