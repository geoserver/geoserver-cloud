/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.integration.jdbcconfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.integration.catalog.IntegrationTestConfiguration;
import org.geoserver.config.GeoServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest(classes = IntegrationTestConfiguration.class, properties = {//
        "geoserver.backend.jdbcconfig.enabled=true"//
        , "geoserver.backend.jdbcconfig.datasource.minimumIdle=1"//
        , "geoserver.backend.jdbcconfig.datasource.maximumPoolSize=2"//
        // 250ms is the minimum accepted by hikari
        , "geoserver.backend.jdbcconfig.datasource.connectionTimeout=250"//
        , "logging.level.org.geoserver.cloud.autoconfigure.bus=ERROR"//
        , "logging.level.org.geoserver.cloud.integration.jdbcconfig=debug"//
        , "logging.level.org.geoserver.jdbcconfig=info"//
})
@Slf4j
class JDBCConfigCatalogConcurrencyIT {

    private @Autowired @Qualifier("catalogFacade") ExtendedCatalogFacade jdbcCatalogFacade;
    private @Autowired @Qualifier("rawCatalog") Catalog rawCatalog;
    private @Autowired GeoServer geoServer;

    private CatalogTestData data;


    // public static @BeforeClass void oneTimeSetup() {
    // GeoServerExtensionsHelper.setIsSpringContext(false);
    // if (null == GeoServerExtensions.bean("sldHandler"))
    // GeoServerExtensionsHelper.singleton("sldHandler", new SLDHandler(), StyleHandler.class);
    // }

    @BeforeEach
    public void setUp() throws Exception {
        data = CatalogTestData.empty(() -> rawCatalog, () -> geoServer).initialize();
        data.deleteAll();
    }

    public @BeforeEach void prepare() {
        data.deleteAll(rawCatalog);
        jdbcCatalogFacade.dispose(); // disposes internal caches
    }

    @Test void catalogConcurrency_1() {
        data.addObjects();
        concurrencyTest(1);
    }

    @Test void catalogConcurrency_4() {
        data.addObjects();
        concurrencyTest(4);
    }

    @Test void catalogConcurrency_16() {
        data.addObjects();
        concurrencyTest(16);
    }

    @Test void catalogConcurrency_32() {
        data.addObjects();
        concurrencyTest(32);
    }

    private void concurrencyTest(int threads) {
        log.info("Running concurrency test with {} threads...", threads);
        final int reps = 10;
        Collection<Callable<Void>> tasks = IntStream.rangeClosed(1, threads)//
                .mapToObj(i -> (Callable<Void>) () -> {
                    for(int rep = 0; rep < reps; rep++)
                        query();
                    return null;
                }).toList();

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("thread %d [0-" + (threads - 1) + "]").build();
        ExecutorService executor = Executors.newFixedThreadPool(threads, threadFactory);
        try {
            executor.invokeAll(tasks);
            executor.shutdown();
            executor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdownNow();
        }
    }

    private void query() {
        //log.info("Querying catalog objects");
        assertNotNull(rawCatalog.getWorkspace(data.workspaceA.getId()));
        assertNotNull(rawCatalog.getWorkspace(data.workspaceB.getId()));
        assertNotNull(rawCatalog.getWorkspace(data.workspaceC.getId()));

        assertNotNull(rawCatalog.getStore(data.dataStoreA.getId(), DataStoreInfo.class));
        assertNotNull(rawCatalog.getStore(data.dataStoreB.getId(), DataStoreInfo.class));
        assertNotNull(rawCatalog.getStore(data.dataStoreC.getId(), DataStoreInfo.class));

        assertNotNull(rawCatalog.getStore(data.coverageStoreA.getId(), CoverageStoreInfo.class));
        assertNotNull(rawCatalog.getStore(data.wmsStoreA.getId(), WMSStoreInfo.class));
        assertNotNull(rawCatalog.getStore(data.wmtsStoreA.getId(), WMTSStoreInfo.class));

        assertNotNull(rawCatalog.getResource(data.featureTypeA.getId(), FeatureTypeInfo.class));
        assertNotNull(rawCatalog.getResource(data.coverageA.getId(), CoverageInfo.class));
        assertNotNull(rawCatalog.getResource(data.wmsLayerA.getId(), WMSLayerInfo.class));
        assertNotNull(rawCatalog.getResource(data.wmtsLayerA.getId(), WMTSLayerInfo.class));

        assertNotNull(rawCatalog.getStyle(data.style1.getId()));
        assertNotNull(rawCatalog.getStyle(data.style2.getId()));
        assertNotNull(rawCatalog.getLayer(data.layerFeatureTypeA.getId()));
        assertNotNull(rawCatalog.getLayerGroup(data.layerGroup1.getId()));
        
        rawCatalog.getNamespaces();
        rawCatalog.getWorkspaces();
        rawCatalog.getLayers();
        rawCatalog.getStores(StoreInfo.class);
        rawCatalog.getResources(ResourceInfo.class);
        rawCatalog.getStyles();
    }
}


