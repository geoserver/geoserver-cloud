/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.test.it.datadir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static java.time.Duration.ofSeconds;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.faker.CatalogFaker;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@Slf4j(topic = "it.datadir")
@SpringBootTest(classes = DockerComposeConfiguration.class)
@ActiveProfiles({"datadir", "it"})
@TestPropertySource(
        properties = { //
            /*
             * The config directory for this app. It's the same symlink at the project's root used in
             * the docker-compose file for the TestContainers' DockerComposeContainer
             */
            "spring.config.additional-location=file:./config/", //
        })
public class DataAccessRulesIT extends DockerComposeTest {

    private @Autowired Catalog catalog;
    private @Autowired GeoServer geoserver;
    private @Autowired GeoServerDataDirectory datadir;

    private @Autowired DataAccessRuleDAO dataAccessRuleDAO;
    private @Autowired UpdateSequence updateSequence;

    @Test
    public void contextLoads() throws InterruptedException {
        log.info("running test from datadir {}", datadir.get("").dir().getAbsolutePath());
        assertNotNull(catalog);
        assertNotNull(geoserver);

        final long initial = updateSequence.currValue();

        CatalogFaker faker = new CatalogFaker(catalog, geoserver);

        WorkspaceInfo ws = faker.workspaceInfo();
        NamespaceInfo ns = faker.namespace(ws.getName());
        catalog.add(ws);
        catalog.add(ns);

        long expected = this.updateSequence.currValue();
        assertThat(expected).isNotEqualTo(initial);

        await().atMost(ofSeconds(5))
                .untilAsserted(
                        () ->
                                assertEquals(
                                        expected,
                                        getServiceUpdateSequence("wfs").getReal(),
                                        "Canonical update sequence value mismatch"));

        await().atMost(ofSeconds(5))
                .untilAsserted(
                        () ->
                                assertEquals(
                                        expected,
                                        getServiceUpdateSequence("wfs").getObserved(),
                                        "Observed update sequence value mismatch"));
    }
}
