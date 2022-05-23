/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.core;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

@Slf4j(topic = "org.geoserver.cloud.autoconfigure.web")
public abstract class AbstractWebUIAutoConfiguration {

    protected abstract String getConfigPrefix();

    public @PostConstruct void log() {
        log.info(getConfigPrefix() + " enabled");
    }
}
