/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(classes = ControlFlowAutoConfiguration.class)
@Slf4j
class ControlFlowAutoConfigurationIT {

    @Autowired
    ControlFlowConfigurationProperties config;

    @Autowired
    ApplicationContext context;

    @Test
    void testControlFlowAppContextInitializer() {
        String cores = context.getEnvironment().getProperty("cpu.cores");
        assertThat(cores).isEqualTo(Runtime.getRuntime().availableProcessors() + "");
    }

    /**
     * See {@literal src/test/resources/application.yml}
     */
    @Test
    void testResolvedProperties() {
        Properties props = config.getProperties();
        Properties resolved = config.resolvedProperties();
        log.info("control-flow unresolved props: {}", props);
        log.info("control-flow   resolved props: {}", resolved);

        int cores = Runtime.getRuntime().availableProcessors();
        String coresTimes2 = String.valueOf(2 * cores);
        String coresTimes4 = String.valueOf(4 * cores);
        String halfCores = String.valueOf(cores / 2);

        assertThat(resolved.getProperty("ows.global")).isEqualTo(coresTimes2);
        assertThat(resolved.getProperty("ows.wms")).isEqualTo(String.valueOf(cores));
        assertThat(resolved.getProperty("ows.wms.getmap")).isEqualTo(halfCores);
        assertThat(resolved.getProperty("ows.gwc")).isEqualTo(coresTimes4);
        assertThat(resolved.getProperty("ows.wfs.getfeature.application/msexcel"))
                .isEqualTo("2");

        assertThat(resolved.getProperty("timeout")).isEqualTo("10");
        assertThat(resolved.getProperty("user")).isEqualTo(String.valueOf(cores));
        assertThat(resolved.getProperty("user.ows.wps.execute")).isEqualTo("1000/d;30s");

        assertThat(resolved.getProperty("ip")).isEqualTo("6");
        assertThat(resolved.getProperty("ip.10.0.0.1")).isEqualTo(String.valueOf(3 * cores));
        assertThat(resolved.getProperty("ip.blacklist")).isEqualTo("192.168.0.7, 192.168.0.8");
    }
}
