/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.ConfigInfoKeys;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup;
import org.geoserver.cloud.backend.pgconfig.cache.ZstdCodec;
import org.geoserver.cloud.backend.pgconfig.support.LogCapture;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.backend.pgconfig.support.StatementCounter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSInfoImpl;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The configuration repository with an entity cache. The fixtures are written through a configuration without a cache,
 * and the repository under test has a cache with the zstd codec.
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigConfigRepositoryIT {

    private static final long ONE_MIB = 1024 * 1024;

    private static final String CONFIG_LOGGER = "org.geoserver.cloud.backend.pgconfig.config";

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private CatalogPlugin catalog;
    private GeoServer geoServer;
    private StatementCounter counter;
    private ZstdCodec codec;
    private EntityCache cache;
    private PgconfigConfigRepository repository;
    private WorkspaceInfo wsa;
    private String settingsId;
    private String globalWmsId;
    private String wsaWmsId;

    @BeforeEach
    void setUp() {
        PgconfigBackendBuilder builder = new PgconfigBackendBuilder(db.getDataSource());
        catalog = builder.createCatalog();
        geoServer = builder.createGeoServer(catalog);
        geoServer.setGlobal(geoServer.getFactory().createGlobal());
        wsa = addWorkspace("wsa");
        settingsId = addSettings(wsa).getId();
        globalWmsId = addService(newWms("WMS", null)).getId();
        wsaWmsId = addService(newWms("wsa-wms", wsa)).getId();

        counter = new StatementCounter(db.getDataSource());
        codec = ZstdCodec.withBundledDictionary(ZstdCodec.DEFAULT_LEVEL);
        cache = new EntityCache("gs-config", ONE_MIB, Duration.ofSeconds(30), codec);
        cache.setInitialized(true);
        repository = new PgconfigConfigRepository(new JdbcTemplate(counter.dataSource()));
        repository.setEntityCache(cache);
    }

    @AfterEach
    void closeCodec() {
        codec.close();
    }

    @Test
    void globalReadTwiceRunsOneQuery() {
        repository.getGlobal();
        Optional<GeoServerInfo> global = repository.getGlobal();

        assertThat(counter.count()).isEqualTo(1);
        assertThat(global).isPresent();
    }

    @Test
    void settingsByWorkspaceCached() {
        repository.getSettingsByWorkspace(wsa);
        counter.reset();

        Optional<SettingsInfo> found = repository.getSettingsByWorkspace(wsa);

        assertThat(counter.count()).isZero();
        assertThat(found).map(SettingsInfo::getId).contains(settingsId);
    }

    @Test
    void serviceByWorkspaceAndClassCached() {
        repository.getServiceByWorkspace(wsa, WMSInfo.class);
        counter.reset();

        Optional<WMSInfo> found = repository.getServiceByWorkspace(wsa, WMSInfo.class);

        assertThat(counter.count()).isZero();
        assertThat(found).map(WMSInfo::getId).contains(wsaWmsId);
    }

    @Test
    void setGlobalEvicts() {
        GeoServerInfo global = repository.getGlobal().orElseThrow();
        global.setFeatureTypeCacheSize(42);

        repository.setGlobal(global);

        assertThat(repository.getGlobal())
                .map(GeoServerInfo::getFeatureTypeCacheSize)
                .contains(42);
    }

    @Test
    void serviceUpdateEvicts() {
        WMSInfo wms = repository.getServiceByWorkspace(wsa, WMSInfo.class).orElseThrow();
        repository.getServiceById(wsaWmsId, WMSInfo.class);

        repository.update(wms, new Patch().with("title", "changed"));

        assertThat(repository.getServiceByWorkspace(wsa, WMSInfo.class))
                .map(WMSInfo::getTitle)
                .contains("changed");
        assertThat(repository.getServiceById(wsaWmsId, WMSInfo.class))
                .map(WMSInfo::getTitle)
                .contains("changed");
    }

    @Test
    void settingsMissIsRemembered() {
        WorkspaceInfo wsb = addWorkspace("wsb");
        counter.reset();

        repository.getSettingsByWorkspace(wsb);
        repository.getSettingsByWorkspace(wsb);

        assertThat(counter.count()).isEqualTo(1);
    }

    // a miss stored for the workspace would hide the settings added to it
    @Test
    void settingsAddEvictsMiss() {
        WorkspaceInfo wsb = addWorkspace("wsb");
        assertThat(repository.getSettingsByWorkspace(wsb)).isEmpty();
        SettingsInfo settings = newSettings("SettingsInfo-wsb", wsb);

        repository.add(settings);

        assertThat(repository.getSettingsByWorkspace(wsb))
                .map(SettingsInfo::getId)
                .contains("SettingsInfo-wsb");
    }

    // the lookup names the interface of the service, and the service added is an instance of its implementation
    @Test
    void serviceAddEvictsClassMiss() {
        WorkspaceInfo wsb = addWorkspace("wsb");
        assertThat(repository.getServiceByWorkspace(wsb, WMSInfo.class)).isEmpty();
        WMSInfoImpl wms = newWms("wsb-wms", wsb);
        wms.setId("WMSInfo-wsb");

        repository.add(wms);

        assertThat(repository.getServiceByWorkspace(wsb, WMSInfo.class))
                .map(WMSInfo::getId)
                .contains("WMSInfo-wsb");
    }

    // services of different classes may share a name: the miss of one class must not hide the other
    @Test
    void nameMissOfOtherClass() {
        assertThat(repository.getServiceByName("WMS", WFSInfo.class)).isEmpty();

        Optional<WMSInfo> found = repository.getServiceByName("WMS", WMSInfo.class);

        assertThat(found).map(WMSInfo::getId).contains(globalWmsId);
    }

    // the key of the name holds the id of the WMS, and the lookup asks for the WFS with the same name
    @Test
    void nameHintOfOtherClass() {
        String wfsId = addService(newWfs("WMS")).getId();
        assertThat(repository.getServiceByName("WMS", WMSInfo.class)).isPresent();

        Optional<WFSInfo> found = repository.getServiceByName("WMS", WFSInfo.class);

        assertThat(found).map(WFSInfo::getId).contains(wfsId);
    }

    // the service is cached again under its id: a hint left under the old name would find it
    @Test
    void renameEvictsBothNames() {
        repository.getServiceByName("WMS", ServiceInfo.class);
        repository.getServiceByName("renamed", ServiceInfo.class);
        ServiceInfo wms =
                repository.getServiceById(globalWmsId, ServiceInfo.class).orElseThrow();

        repository.update(wms, new Patch().with("name", "renamed"));
        repository.getServiceById(globalWmsId, ServiceInfo.class);

        assertThat(repository.getServiceByName("WMS", ServiceInfo.class)).isEmpty();
        assertThat(repository.getServiceByName("renamed", ServiceInfo.class))
                .map(ServiceInfo::getId)
                .contains(globalWmsId);
    }

    // a move evicts the class lookups of both workspaces: the hint of the old one, and the miss of the new one
    @Test
    void moveEvictsClassLookups() {
        WorkspaceInfo wsb = addWorkspace("wsb");
        WMSInfo wms = repository.getServiceByWorkspace(wsa, WMSInfo.class).orElseThrow();
        assertThat(repository.getServiceByWorkspace(wsb, WMSInfo.class)).isEmpty();

        repository.update(wms, new Patch().with("workspace", wsb));

        CacheKey oldWorkspaceKey = ConfigInfoKeys.serviceByWorkspaceAndClass(wsa.getId(), WMSInfo.class);
        assertThat(cache.lookup(oldWorkspaceKey)).isInstanceOf(Lookup.Absent.class);
        assertThat(repository.getServiceByWorkspace(wsa, WMSInfo.class)).isEmpty();
        assertThat(repository.getServiceByWorkspace(wsb, WMSInfo.class))
                .map(WMSInfo::getId)
                .contains(wsaWmsId);
    }

    // another service instance renames the service, and a listing here caches the renamed service under its id
    @Test
    void renamedHintIsEvicted() {
        WMSInfo wms = repository.getServiceByName("WMS", WMSInfo.class).orElseThrow();
        otherInstance().update(wms, new Patch().with("name", "renamed"));
        globalServices();

        Optional<WMSInfo> found = repository.getServiceByName("WMS", WMSInfo.class);

        assertThat(found).isEmpty();
        assertThat(cache.lookup(ConfigInfoKeys.serviceByName("WMS"))).isNotInstanceOf(Lookup.Hit.class);
    }

    // another service instance moves the service, and a listing here caches the moved service under its id
    @Test
    void movedHintIsEvicted() {
        WorkspaceInfo wsb = addWorkspace("wsb");
        WMSInfo wms = repository.getServiceByWorkspace(wsa, WMSInfo.class).orElseThrow();
        otherInstance().update(wms, new Patch().with("workspace", wsb));
        servicesOf(wsb);

        Optional<WMSInfo> found = repository.getServiceByWorkspace(wsa, WMSInfo.class);

        assertThat(found).isEmpty();
        CacheKey oldWorkspaceKey = ConfigInfoKeys.serviceByWorkspaceAndClass(wsa.getId(), WMSInfo.class);
        assertThat(cache.lookup(oldWorkspaceKey)).isNotInstanceOf(Lookup.Hit.class);
    }

    @Test
    void listingFeedsCache() {
        assertThat(globalServices()).extracting(ServiceInfo::getId).containsExactly(globalWmsId);
        counter.reset();

        repository.getServiceById(globalWmsId, WMSInfo.class);

        assertThat(counter.count()).isZero();
    }

    // each read falls back to the database, and the first one logs a warning
    @Test
    void badCachedJsonFallsBack() {
        cache.put(ConfigInfoKeys.service(globalWmsId), "{".getBytes(UTF_8), cache.epoch());
        try (LogCapture log = LogCapture.of(CONFIG_LOGGER)) {
            counter.reset();

            Optional<WMSInfo> first = repository.getServiceById(globalWmsId, WMSInfo.class);
            Optional<WMSInfo> second = repository.getServiceById(globalWmsId, WMSInfo.class);

            assertThat(first).map(WMSInfo::getId).contains(globalWmsId);
            assertThat(second).map(WMSInfo::getId).contains(globalWmsId);
            assertThat(counter.count()).isEqualTo(2);
            assertThat(log.messages(Level.WARN)).singleElement().asString().contains(globalWmsId);
            assertThat(log.messages(Level.DEBUG))
                    .filteredOn(message -> message.contains(globalWmsId))
                    .hasSize(1);
        }
    }

    // a read within the transaction may see changes not committed yet: they must stay out of the shared cache
    @Test
    void transactionReadSkipsCache() {
        transaction().executeWithoutResult(status -> repository.getGlobal());

        assertThat(cache.lookup(ConfigInfoKeys.global())).isInstanceOf(Lookup.Absent.class);
    }

    @Test
    void evictsAfterCommit() {
        GeoServerInfo global = repository.getGlobal().orElseThrow();
        CacheKey key = ConfigInfoKeys.global();

        transaction().executeWithoutResult(status -> {
            repository.setGlobal(global);
            assertThat(cache.lookup(key)).isInstanceOf(Lookup.Hit.class);
        });

        assertThat(cache.lookup(key)).isInstanceOf(Lookup.Absent.class);
    }

    @Test
    void nullCacheReadsDatabase() {
        repository.setEntityCache(null);

        repository.getGlobal();
        repository.getGlobal();

        assertThat(counter.count()).isEqualTo(2);
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(new DataSourceTransactionManager(counter.dataSource()));
    }

    private List<ServiceInfo> globalServices() {
        try (Stream<ServiceInfo> services = repository.getGlobalServices()) {
            return services.toList();
        }
    }

    private List<ServiceInfo> servicesOf(WorkspaceInfo workspace) {
        try (Stream<ServiceInfo> services = repository.getServicesByWorkspace(workspace)) {
            return services.toList();
        }
    }

    /** The configuration repository of another service instance, without a cache. */
    private PgconfigConfigRepository otherInstance() {
        return new PgconfigConfigRepository(db.getTemplate());
    }

    private WorkspaceInfo addWorkspace(String name) {
        WorkspaceInfo workspace = catalog.getFactory().createWorkspace();
        workspace.setName(name);
        catalog.add(workspace);
        return catalog.getWorkspaceByName(name);
    }

    private SettingsInfo addSettings(WorkspaceInfo workspace) {
        SettingsInfo settings = geoServer.getFactory().createSettings();
        settings.setWorkspace(workspace);
        geoServer.add(settings);
        return settings;
    }

    /** Settings of the workspace, with an id and not added. */
    private SettingsInfo newSettings(String id, WorkspaceInfo workspace) {
        SettingsInfoImpl settings = new SettingsInfoImpl();
        settings.setId(id);
        settings.setWorkspace(workspace);
        return settings;
    }

    /** Adds a service, and returns it with the id set by the configuration. */
    private <S extends ServiceInfo> S addService(S service) {
        geoServer.add(service);
        return service;
    }

    /** A WMS, global if the workspace is null, not added. */
    private WMSInfoImpl newWms(String name, WorkspaceInfo workspace) {
        WMSInfoImpl wms = new WMSInfoImpl();
        wms.setName(name);
        wms.setWorkspace(workspace);
        return wms;
    }

    /** A global WFS, not added. */
    private WFSInfoImpl newWfs(String name) {
        WFSInfoImpl wfs = new WFSInfoImpl();
        wfs.setName(name);
        return wfs;
    }
}
