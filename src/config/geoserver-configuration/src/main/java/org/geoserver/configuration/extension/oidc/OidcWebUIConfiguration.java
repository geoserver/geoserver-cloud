/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.oidc;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Transpiled Spring configuration for the GeoServer OIDC extension's web UI context (bean definitions from
 * {@code gs-sec-oidc-web}'s {@code applicationContext.xml}).
 *
 * <p>Registers the authentication filter panel info, the per-provider login form info beans (Google, GitHub, Microsoft,
 * generic OIDC) and the login button manager.
 *
 * @since 3.0.3
 * @see OidcCoreConfiguration
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(locations = "jar:gs-sec-oidc-web-.*!/applicationContext.xml")
@Import({OidcCoreConfiguration.class, OidcWebUIConfiguration_Generated.class})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class OidcWebUIConfiguration {}
