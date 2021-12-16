/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wps;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.util.unit.DataSize;

@Slf4j
@SpringBootApplication
@EnableRetry
public class WpsApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(WpsApplication.class, args);
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
                "{} ready. Instance-id: {}, cpus: {}, max memory: {}. Running as {}({}:{})",
                app,
                instanceId,
                cpus,
                maxMem,
                env.getProperty("user.name"),
                env.getProperty("user.id"),
                env.getProperty("user.gid"));
    }
}
