/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.web.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.unit.DataSize;

@Slf4j
@SpringBootApplication
public class WebUIApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(WebUIApplication.class, args);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent e) {
        ConfigurableEnvironment env = e.getApplicationContext().getEnvironment();

        String app = env.getProperty("spring.application.name");
        String instanceId = env.getProperty("server.instance-id");
        int cpus = Runtime.getRuntime().availableProcessors();
        String maxMem;
        {
            DataSize maxMemBytes = DataSize.ofBytes(Runtime.getRuntime().maxMemory());
            double value = maxMemBytes.toKilobytes() / 1024d;
            String unit = "MB";
            if (maxMemBytes.toGigabytes() > 0) {
                value = value / 1024d;
                unit = "GB";
            }
            maxMem = String.format("%.2f %s", value, unit);
        }
        log.info(
                "{} ready. Instance-id: {}, cpus: {}, max memory: {}",
                app,
                instanceId,
                cpus,
                maxMem);

        String nodeOpts = System.getProperty("GEOSERVER_NODE_OPTS");
        if (null == nodeOpts) {
            nodeOpts = String.format("id:%s;color:FF0000", instanceId);
            System.setProperty("GEOSERVER_NODE_OPTS", nodeOpts);
        }
    }
}
