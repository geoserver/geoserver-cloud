/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wms.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.wms.controller.kml.KMLIconsController;
import org.geoserver.cloud.wms.controller.kml.KMLReflectorController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
class KMLAutoConfigurationTest {

    private @Autowired ConfigurableApplicationContext context;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {}

    @Test
    void testControllers() {
        expecteBean("kmlIconsController", KMLIconsController.class);
        expecteBean("kmlReflectorController", KMLReflectorController.class);
    }

    private void expecteBean(String name, Class<?> type) {
        assertThat(context.getBean(name)).isInstanceOf(type);
    }
}
