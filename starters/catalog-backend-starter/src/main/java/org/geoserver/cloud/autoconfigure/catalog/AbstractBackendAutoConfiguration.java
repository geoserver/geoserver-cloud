/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class AbstractBackendAutoConfiguration {

    public @PostConstruct void log() {
        log.info("Processing geoserver config backend with {}", getClass().getSimpleName());
    }
}
