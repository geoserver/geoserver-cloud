/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.ZstdCodec;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.config.UpdateSequenceConformanceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The update sequence of a service instance with the configuration cache, as the backend configures it.
 *
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
@Execution(value = ExecutionMode.CONCURRENT)
@SuppressWarnings("java:S2187")
class PgconfigUpdateSequenceIT implements UpdateSequenceConformanceTest {

    private static final long CACHE_WEIGHT = 16L * 1024 * 1024;

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private ZstdCodec codec;
    private UpdateSequence sequence;
    private PgconfigGeoServerFacade facade;
    private GeoServer geoserver;

    @BeforeEach
    void init() {
        codec = ZstdCodec.withBundledDictionary(ZstdCodec.DEFAULT_LEVEL);
        EntityCache cache = new EntityCache("gs-config", CACHE_WEIGHT, Duration.ofSeconds(30), codec);
        cache.setInitialized(true);
        PgconfigConfigRepository repository = new PgconfigConfigRepository(db.getTemplate());
        repository.setEntityCache(cache);
        facade = new PgconfigGeoServerFacade(repository);
        geoserver = new GeoServerImpl(facade);
        sequence = new PgconfigUpdateSequence(db.getDataSource(), facade);
    }

    @AfterEach
    void closeCodec() {
        codec.close();
    }

    @Override
    public UpdateSequence getUpdataSequence() {
        return sequence;
    }

    @Override
    public GeoServer getGeoSever() {
        return geoserver;
    }

    // another service instance changes the global settings, and its event has not reached this instance yet
    @Test
    void nextValueKeepsOtherChange() {
        geoserver.setGlobal(geoserver.getFactory().createGlobal());
        geoserver.getGlobal();
        GeoServer otherInstance = new GeoServerImpl(new PgconfigGeoServerFacade(db.getTemplate()));
        GeoServerInfo changed = otherInstance.getGlobal();
        changed.getSettings().setTitle("changed elsewhere");
        otherInstance.save(changed);

        long next = sequence.nextValue();

        GeoServerInfo stored = otherInstance.getGlobal();
        assertThat(stored.getSettings().getTitle()).isEqualTo("changed elsewhere");
        assertThat(stored.getUpdateSequence()).isEqualTo(next);
        assertThat(geoserver.getGlobal().getSettings().getTitle()).isEqualTo("changed elsewhere");
    }
}
