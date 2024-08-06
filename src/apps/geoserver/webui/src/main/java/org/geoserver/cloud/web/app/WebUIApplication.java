/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.web.app;

import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class WebUIApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(WebUIApplication.class, args);
        } catch (RuntimeException e) {
            try {
                LoggerFactory.getLogger(WebUIApplication.class).error("Application run failed", e);
            } finally {
                System.exit(-1);
            }
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent e) {
        ConfigurableEnvironment env = e.getApplicationContext().getEnvironment();
        final String instanceId = env.getProperty("info.instance-id");
        String nodeOpts = System.getProperty("GEOSERVER_NODE_OPTS");
        if (null == nodeOpts) {
            nodeOpts = "id:%s;color:FF0000".formatted(instanceId);
            System.setProperty("GEOSERVER_NODE_OPTS", nodeOpts);
        }
    }
}
