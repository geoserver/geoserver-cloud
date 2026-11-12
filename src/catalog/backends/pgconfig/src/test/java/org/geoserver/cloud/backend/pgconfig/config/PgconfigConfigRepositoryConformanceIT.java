/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/*
 * /* (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;
import org.geoserver.catalog.plugin.requestcache.RequestCachingGeoServerFacade;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.ZstdCodec;
import org.geoserver.cloud.backend.pgconfig.catalog.PgconfigCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigConformanceTest;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.plugin.GeoServerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The conformance suite of the configuration over the pgconfig repository with the configuration cache, and without it
 * ({@link WithoutCache}).
 *
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
@Execution(value = ExecutionMode.CONCURRENT)
class PgconfigConfigRepositoryConformanceIT extends GeoServerConfigConformanceTest {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    /** The codecs of the caches of a test: a nested suite also runs the setup of this class, and opens two. */
    private final List<ZstdCodec> codecs = new ArrayList<>();

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @AfterEach
    void closeCodecs() {
        codecs.forEach(ZstdCodec::close);
        codecs.clear();
    }

    /** Mirrors the backend configuration: the workspaces of settings and services come from the catalog facade. */
    protected @Override GeoServer createGeoServer() {
        return newGeoServer(newPgconfigFacade());
    }

    private PgconfigGeoServerFacade newPgconfigFacade() {
        PgconfigConfigRepository repository = new PgconfigConfigRepository(db.getTemplate());
        repository.setEntityCache(newEntityCache());
        PgconfigGeoServerFacade facade = new PgconfigGeoServerFacade(repository);
        PgconfigCatalogFacade catalogFacade = new PgconfigCatalogFacade(db.getTemplate());
        facade.setWorkspaceResolver(ResolvingProxyResolver.of(catalogFacade));
        return facade;
    }

    private GeoServer newGeoServer(GeoServerFacade facade) {
        PgconfigBackendBuilder builder = new PgconfigBackendBuilder(db.getDataSource());
        Catalog catalog = builder.createCatalog();
        GeoServerImpl geoServer = new GeoServerImpl(facade);
        geoServer.setCatalog(catalog);
        return geoServer;
    }

    /** The configuration cache as the backend configures it, initialized as after the loader ran. */
    private EntityCache newEntityCache() {
        ZstdCodec codec = ZstdCodec.withBundledDictionary(ZstdCodec.DEFAULT_LEVEL);
        codecs.add(codec);
        long maximumWeight = 16L * 1024 * 1024;
        EntityCache cache = new EntityCache("gs-config", maximumWeight, Duration.ofSeconds(30), codec);
        cache.setInitialized(true);
        return cache;
    }

    @Test
    void testAddDuplicateServiceToWorkspace() {
        // Make a workspace
        WorkspaceInfo ws1 = geoServer.getCatalog().getFactory().createWorkspace();
        ws1.setName("TEST-WORKSPACE-1");
        geoServer.getCatalog().add(ws1);

        // Make a service for that workspace
        ServiceInfo service1 = createService();
        service1.setWorkspace(ws1);
        service1.setName("TEST-OWS");
        service1.setTitle("Service for WS1");
        geoServer.add(service1);

        ServiceInfo dupTypeAndWorkspace = createService();
        dupTypeAndWorkspace.setWorkspace(ws1);
        dupTypeAndWorkspace.setName("TEST-OWS");
        dupTypeAndWorkspace.setTitle("Service for WS1");

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> geoServer.add(dupTypeAndWorkspace));

        assertThat(ex.getMessage())
                .contains("service with name 'TEST-OWS' already exists in workspace 'TEST-WORKSPACE-1'");
    }

    /**
     * The suite through the cache of a request, as the backend configuration builds the configuration facade. Each test
     * runs within one request scope, as a request of the REST API or of the web UI reads and writes the configuration.
     */
    @Nested
    class WithinRequest extends GeoServerConfigConformanceTest {

        @Override
        protected GeoServer createGeoServer() {
            PgconfigGeoServerFacade facade = newPgconfigFacade();
            RequestCachingGeoServerFacade requestCaching = new RequestCachingGeoServerFacade(facade);
            return newGeoServer(requestCaching);
        }

        @BeforeEach
        void openRequest() {
            RequestCacheScope.open(RequestCacheScope.DEFAULT_MAX_ENTRIES);
        }

        @AfterEach
        void closeRequest() {
            RequestCacheScope.close();
        }
    }

    /**
     * The suite over a repository without a cache, as with the cache disabled. The repository reads the same way while
     * the cache is suspended, and within a transaction on its data source.
     */
    @Nested
    class WithoutCache extends GeoServerConfigConformanceTest {

        @Override
        protected GeoServer createGeoServer() {
            PgconfigBackendBuilder builder = new PgconfigBackendBuilder(db.getDataSource());
            Catalog catalog = builder.createCatalog();
            return builder.createGeoServer(catalog);
        }
    }
}
