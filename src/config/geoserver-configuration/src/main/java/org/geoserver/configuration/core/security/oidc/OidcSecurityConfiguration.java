/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.security.oidc;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

/** Loads GeoServer's OIDC security provider configuration into each Cloud service. */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-sec-oidc-core-.*!/applicationContext.xml",
        excludes = {
            "oidcHttpSecurityConfiguration",
            "oidcObjectPostProcessorConfiguration",
            "oidcAuthenticationConfiguration",
            "oauth2LoginAuthenticationProvider"
        })
@Import(OidcSecurityConfiguration_Generated.class)
@ImportResource("classpath:org/geoserver/configuration/core/security/oidc/oidcHttpSecurityConfiguration.xml")
public class OidcSecurityConfiguration {}
