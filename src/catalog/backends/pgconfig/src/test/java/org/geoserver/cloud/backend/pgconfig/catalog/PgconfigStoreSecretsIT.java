/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.NoopCodec;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.RepositoryDecodes;
import org.geoserver.cloud.backend.pgconfig.catalog.secrets.StoreSecrets;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.security.secrets.SecurityManagerSupport;
import org.geoserver.cloud.security.secrets.StoreSecretsCipher;
import org.geoserver.security.GeoServerSecurityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Store secrets in the rows of a pgconfig catalog with the entity cache: the rows hold them encrypted with the keystore
 * of the security manager, the catalog returns them in clear text, and a cached store is decrypted once.
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigStoreSecretsIT {

    private static final long ONE_MIB = 1024 * 1024;
    private static final String PASSWORD = "wms-secret";

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    @TempDir
    File securityDirectory;

    @Test
    void rowHoldsEncryptedSecret() {
        runInitialized(manager -> {
            Fixture fixture = new Fixture(new StoreSecrets(new StoreSecretsCipher(() -> manager)));

            WMSStoreInfo store = fixture.addWmsStore(PASSWORD);

            assertThat(rowPassword(store)).startsWith("crypt1:");
            assertThat(fixture.catalog
                            .getStore(store.getId(), WMSStoreInfo.class)
                            .getPassword())
                    .isEqualTo(PASSWORD);
        });
    }

    @Test
    void passwordChangeEncrypted() {
        runInitialized(manager -> {
            Fixture fixture = new Fixture(new StoreSecrets(new StoreSecretsCipher(() -> manager)));
            WMSStoreInfo store = fixture.addWmsStore(PASSWORD);
            String before = rowPassword(store);

            WMSStoreInfo changed = fixture.catalog.getStore(store.getId(), WMSStoreInfo.class);
            changed.setPassword("changed-secret");
            fixture.catalog.save(changed);

            assertThat(rowPassword(store)).startsWith("crypt1:").isNotEqualTo(before);
            assertThat(fixture.catalog
                            .getStore(store.getId(), WMSStoreInfo.class)
                            .getPassword())
                    .isEqualTo("changed-secret");
        });
    }

    // a cached store decodes on each lookup, and must not decrypt each time
    @Test
    void cachedStoreDecryptedOnce() {
        runInitialized(manager -> {
            StoreSecrets secrets = new StoreSecrets(new StoreSecretsCipher(() -> manager));
            Fixture fixture = new Fixture(secrets);
            WMSStoreInfo store = fixture.addWmsStore(PASSWORD);
            fixture.catalog.getStore(store.getId(), WMSStoreInfo.class);
            long decryptsAfterFirstRead = secrets.decryptedValues();
            long decodesBefore = RepositoryDecodes.total();

            for (int i = 0; i < 20; i++) {
                assertThat(fixture.catalog
                                .getStore(store.getId(), WMSStoreInfo.class)
                                .getPassword())
                        .isEqualTo(PASSWORD);
            }

            assertThat(decryptsAfterFirstRead).isEqualTo(1);
            assertThat(secrets.decryptedValues()).isEqualTo(decryptsAfterFirstRead);
            assertThat(RepositoryDecodes.total() - decodesBefore).isGreaterThanOrEqualTo(20);
        });
    }

    // the rows of a listing share their store: it is decrypted once per listing
    @Test
    void listingDecryptsSharedStoreOnce() {
        runInitialized(manager -> {
            StoreSecrets secrets = new StoreSecrets(new StoreSecretsCipher(() -> manager));
            Fixture fixture = new Fixture(secrets);
            WMSStoreInfo store = fixture.addWmsStore(PASSWORD);
            for (int i = 0; i < 20; i++) {
                fixture.addWmsLayer(store, "layer" + i);
            }
            Fixture fresh = new Fixture(secrets);
            long before = secrets.decryptedValues();

            List<WMSLayerInfo> layers = fresh.catalog.getResources(WMSLayerInfo.class);

            assertThat(layers).hasSize(20).allSatisfy(layer -> assertThat(
                            ((WMSStoreInfo) layer.getStore()).getPassword())
                    .isEqualTo(PASSWORD));
            assertThat(secrets.decryptedValues() - before).isEqualTo(1);
        });
    }

    // a row written in clear text by an earlier version reads as it is, and saving the store encrypts it
    @Test
    void clearTextRowEncryptedOnSave() {
        runInitialized(manager -> {
            WMSStoreInfo store = new Fixture(StoreSecrets.none()).addWmsStore(PASSWORD);
            assertThat(rowPassword(store)).isEqualTo(PASSWORD);
            Fixture fixture = new Fixture(new StoreSecrets(new StoreSecretsCipher(() -> manager)));
            WMSStoreInfo read = fixture.catalog.getStore(store.getId(), WMSStoreInfo.class);
            assertThat(read.getPassword()).isEqualTo(PASSWORD);

            fixture.catalog.save(read);

            assertThat(rowPassword(store)).startsWith("crypt1:");
            assertThat(fixture.catalog
                            .getStore(store.getId(), WMSStoreInfo.class)
                            .getPassword())
                    .isEqualTo(PASSWORD);
        });
    }

    // the keystore of a new instance decrypts the rows written by the previous one
    @Test
    void restartReadsDecrypted() {
        AtomicReference<String> storeId = new AtomicReference<>();
        runInitialized(manager -> {
            Fixture fixture = new Fixture(new StoreSecrets(new StoreSecretsCipher(() -> manager)));
            storeId.set(fixture.addWmsStore(PASSWORD).getId());
        });

        runInitialized(manager -> {
            Fixture restarted = new Fixture(new StoreSecrets(new StoreSecretsCipher(() -> manager)));

            WMSStoreInfo store = restarted.catalog.getStore(storeId.get(), WMSStoreInfo.class);

            assertThat(store.getPassword()).isEqualTo(PASSWORD);
        });
    }

    // a store read before the security manager exists must not stay encrypted in the cache
    @Test
    void readBeforeSecurityManagerNotCached() {
        runInitialized(manager -> {
            WMSStoreInfo store =
                    new Fixture(new StoreSecrets(new StoreSecretsCipher(() -> manager))).addWmsStore(PASSWORD);
            AtomicReference<GeoServerSecurityManager> available = new AtomicReference<>();
            Fixture fixture = new Fixture(new StoreSecrets(new StoreSecretsCipher(available::get)));

            WMSStoreInfo early = fixture.catalog.getStore(store.getId(), WMSStoreInfo.class);
            available.set(manager);
            WMSStoreInfo later = fixture.catalog.getStore(store.getId(), WMSStoreInfo.class);

            assertThat(early.getPassword()).startsWith("crypt1:");
            assertThat(later.getPassword()).isEqualTo(PASSWORD);
        });
    }

    // a database restored without its keystore must not make the listings fail
    @Test
    void undecryptableStoreDisabled() {
        runInitialized(manager -> {
            Fixture fixture = new Fixture(new StoreSecrets(new StoreSecretsCipher(() -> manager)));
            WMSStoreInfo store = fixture.addWmsStore(PASSWORD);
            db.getTemplate()
                    .update(
                            "UPDATE storeinfo SET info = jsonb_set(info, '{password}', to_jsonb(?::text)) WHERE id = ?",
                            "crypt1:bm90IGVuY3J5cHRlZCB3aXRoIHRoaXMga2V5",
                            store.getId());
            Fixture fresh = new Fixture(new StoreSecrets(new StoreSecretsCipher(() -> manager)));

            List<StoreInfo> stores = fresh.catalog.getStores(StoreInfo.class);

            assertThat(stores).singleElement().satisfies(found -> assertThat(found.isEnabled())
                    .isFalse());
        });
    }

    private String rowPassword(StoreInfo store) {
        return db.getTemplate()
                .queryForObject("SELECT info->>'password' FROM storeinfo WHERE id = ?", String.class, store.getId());
    }

    @FunctionalInterface
    interface SecurityTest {
        void run(GeoServerSecurityManager manager) throws Exception;
    }

    private void runInitialized(SecurityTest test) {
        SecurityManagerSupport.contextRunner(securityDirectory).run((AssertableApplicationContext context) -> {
            GeoServerSecurityManager manager = context.getBean(GeoServerSecurityManager.class);
            test.run(SecurityManagerSupport.initialize(manager));
        });
    }

    /** A catalog over the test database, with an entity cache of its own, as a service instance has. */
    private class Fixture {
        final CatalogPlugin catalog = new CatalogPlugin();

        Fixture(StoreSecrets secrets) {
            EntityCache cache = new EntityCache("gs-catalog", ONE_MIB, Duration.ofSeconds(30), new NoopCodec());
            cache.setInitialized(true);
            PgconfigCatalogFacade facade = new PgconfigCatalogFacade(new JdbcTemplate(db.getDataSource()), cache);
            facade.setStoreSecrets(secrets);
            catalog.setFacade(PgconfigBackendBuilder.resolving(facade));
        }

        WMSStoreInfo addWmsStore(String password) {
            CatalogFactory factory = catalog.getFactory();
            WorkspaceInfo workspace = workspace();
            WMSStoreInfo store = factory.createWebMapServer();
            store.setName("wms");
            store.setWorkspace(workspace);
            store.setEnabled(true);
            store.setCapabilitiesURL("http://example.com/wms");
            store.setUsername("user");
            store.setPassword(password);
            catalog.add(store);
            return catalog.getStoreByName(workspace, "wms", WMSStoreInfo.class);
        }

        void addWmsLayer(WMSStoreInfo store, String name) {
            WMSLayerInfo layer = catalog.getFactory().createWMSLayer();
            layer.setName(name);
            layer.setNativeName(name);
            layer.setStore(store);
            layer.setNamespace(catalog.getNamespaceByPrefix("ws"));
            layer.setEnabled(true);
            catalog.add(layer);
        }

        private WorkspaceInfo workspace() {
            WorkspaceInfo workspace = catalog.getWorkspaceByName("ws");
            if (null != workspace) {
                return workspace;
            }
            workspace = catalog.getFactory().createWorkspace();
            workspace.setName("ws");
            catalog.add(workspace);
            NamespaceInfo namespace = catalog.getFactory().createNamespace();
            namespace.setPrefix("ws");
            namespace.setURI("http://ws");
            catalog.add(namespace);
            return catalog.getWorkspaceByName("ws");
        }
    }
}
