/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.geonode;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * @since 1.9
 */
@AutoConfiguration
@ComponentScan(basePackages = "org.geoserver.security.oauth2")
@ImportFilteredResource(
        // gs-sec-oauth2-geonode is all what's needed, gs-sec-oauth2-core and gs-sec-oauth2-web are
        // transitive but not required for this specific functionality
        "jar:gs-sec-oauth2-geonode-.*!/applicationContext.xml")
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.geonode")
public class GeoNodeOAuth2AutoConfiguration {

    GeoNodeOAuth2AutoConfiguration() {
        log.info("GeoNodeOAuth2AutoConfiguration loaded");
    }
}
