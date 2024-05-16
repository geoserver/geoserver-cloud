/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.core;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * @since 1.8.2
 */
@Slf4j
public class WebUIContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // run once for the webapp context, ignore the actuator context
        if (!(applicationContext instanceof GenericWebApplicationContext)) {
            return;
        }
        setHomePageSelectionMode();
    }

    private void setHomePageSelectionMode() {

        String systemProp = System.getProperty("GeoServerHomePage.selectionMode");
        if (StringUtils.hasText(systemProp)) {
            log.info(
                    "GeoServerHomePage.selectionMode set to '{}' through system property",
                    systemProp);
        } else {
            String selectionMode = "TEXT";
            log.info("GeoServerHomePage.selectionMode set to '{}' as default value", selectionMode);
            System.setProperty("GeoServerHomePage.selectionMode", selectionMode);
        }
    }
}
