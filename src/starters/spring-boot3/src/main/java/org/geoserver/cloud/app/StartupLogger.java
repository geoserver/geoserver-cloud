/*
 * (c) 2020-2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.unit.DataSize;

/**
 * Logs basic application info at {@link ApplicationReadyEvent app startup}
 *
 * <p>Expects the following properties be present in the {@link Environment}:
 *
 * <pre>
 *  {@literal spring.application.name}
 *  {@literal info.instance-id}
 * </pre>
 *
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.app")
public class StartupLogger {

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent e) {
        ConfigurableEnvironment env = e.getApplicationContext().getEnvironment();

        String app = env.getProperty("spring.application.name");
        String instanceId = env.getProperty("info.instance-id");
        int cpus = Runtime.getRuntime().availableProcessors();
        String maxMem = maxMem();
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

    private String maxMem() {
        DataSize maxMemBytes = DataSize.ofBytes(Runtime.getRuntime().maxMemory());
        double value = maxMemBytes.toKilobytes() / 1024d;
        String unit = "MB";
        if (maxMemBytes.toGigabytes() > 0) {
            value = value / 1024d;
            unit = "GB";
        }
        return "%.2f %s".formatted(value, unit);
    }
}
