/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

/**
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
class PgsqlWorkspaceRepositoryTest {

    @Container static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    PgsqlWorkspaceRepository repo;

    @BeforeEach
    void setUp() {
        container.setUp();
        repo = new PgsqlWorkspaceRepository(container.getTemplate());
    }

    @AfterEach
    void tearDown() {
        container.tearDown();
    }

    @Test
    void testAdd() {
        WorkspaceInfoImpl info = new WorkspaceInfoImpl();
        info.setId("ws1");
        info.setName("ws1");
        repo.add(info);
        Optional<WorkspaceInfo> found = repo.findById(info.getId(), repo.getContentType());
        assertThat(found).isPresent();
    }
}
