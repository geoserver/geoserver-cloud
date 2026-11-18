/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.event.bus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.security.secrets.SecurityManagerSupport.COVERAGE_SECRET;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.security.secrets.SecurityManagerSupport;
import org.geoserver.cloud.security.secrets.StoreSecretsCipher;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import tools.jackson.databind.ObjectMapper;

/** Store secrets in the events sent through the bus, with the keystore and encoders of a real security manager. */
class RemoteEventSecretsTest {

    private static final ObjectMapper MAPPER = ObjectMapperUtil.newObjectMapper();

    @TempDir
    File dataDirectory;

    @Test
    void addedStoreSentEncrypted() {
        runInitialized((catalog, secrets) -> {
            WMSStoreInfo store = wmsStore(catalog, "wms-secret");
            CatalogInfoAdded event = CatalogInfoAdded.createLocal(1, store);

            CatalogInfoAdded sent = (CatalogInfoAdded) secrets.encrypt(event);

            assertThat(((WMSStoreInfo) sent.getObject()).getPassword()).startsWith("crypt1:");
            assertThat(event.getObject())
                    .as("the catalog store is not modified")
                    .isSameAs(store);
            assertThat(store.getPassword()).isEqualTo("wms-secret");
        });
    }

    @Test
    void addedStoreReceivedDecrypted() {
        runInitialized((catalog, secrets) -> {
            GeoServerEvent sent = secrets.encrypt(CatalogInfoAdded.createLocal(1, wmsStore(catalog, "wms-secret")));

            CatalogInfoAdded received = (CatalogInfoAdded) overTheWire(sent);
            secrets.decrypt(received);

            assertThat(((WMSStoreInfo) received.getObject()).getPassword()).isEqualTo("wms-secret");
        });
    }

    @Test
    void secretParametersEncrypted() {
        runInitialized((catalog, secrets) -> {
            CoverageStoreInfo store = coverageStore(catalog, "s3-secret");

            CatalogInfoAdded sent = (CatalogInfoAdded) secrets.encrypt(CatalogInfoAdded.createLocal(1, store));

            Map<String, Serializable> parameters = ((CoverageStoreInfo) sent.getObject()).getConnectionParameters();
            assertThat((String) parameters.get(COVERAGE_SECRET)).startsWith("crypt1:");
            assertThat(parameters).containsEntry("region", "eu-west-1");
        });
    }

    @Test
    void modifiedPasswordSentEncrypted() {
        runInitialized((catalog, secrets) -> {
            WMSStoreInfo store = wmsStore(catalog, "wms-secret");
            catalog.add(store);
            CatalogInfoModified event = passwordChange(catalog, "new-secret");

            CatalogInfoModified sent = (CatalogInfoModified) secrets.encrypt(event);

            assertThat((String) sent.getPatch().getValue("password").orElseThrow())
                    .startsWith("crypt1:");
            assertThat(event.getPatch().getValue("password")).contains("new-secret");
        });
    }

    @Test
    void modifiedPasswordReceivedDecrypted() {
        runInitialized((catalog, secrets) -> {
            catalog.add(wmsStore(catalog, "wms-secret"));
            GeoServerEvent sent = secrets.encrypt(passwordChange(catalog, "new-secret"));

            CatalogInfoModified received = (CatalogInfoModified) overTheWire(sent);
            secrets.decrypt(received);

            assertThat(received.getPatch().getValue("password")).contains("new-secret");
        });
    }

    @Test
    void otherEventsSentAsTheyAre() {
        runInitialized((catalog, secrets) -> {
            CatalogInfoAdded event = CatalogInfoAdded.createLocal(1, catalog.getDefaultWorkspace());

            assertThat(secrets.encrypt(event)).isSameAs(event);
        });
    }

    // the keystore does not exist before the security manager is initialized
    @Test
    void uninitializedSendsAsIs() {
        SecurityManagerSupport.contextRunner(dataDirectory).run(context -> {
            Catalog catalog = catalog(context);
            RemoteEventSecrets secrets = secrets(catalog, securityManager(context));
            CatalogInfoAdded event = CatalogInfoAdded.createLocal(1, wmsStore(catalog, "wms-secret"));

            assertThat(secrets.encrypt(event)).isSameAs(event);
        });
    }

    @FunctionalInterface
    interface SecretsTest {
        void run(Catalog catalog, RemoteEventSecrets secrets) throws Exception;
    }

    private void runInitialized(SecretsTest test) {
        SecurityManagerSupport.contextRunner(dataDirectory).run(context -> {
            GeoServerSecurityManager manager = SecurityManagerSupport.initialize(securityManager(context));
            Catalog catalog = catalog(context);
            addWorkspace(catalog);
            test.run(catalog, secrets(catalog, manager));
        });
    }

    private static RemoteEventSecrets secrets(Catalog catalog, GeoServerSecurityManager manager) {
        return new RemoteEventSecrets(new StoreSecretsCipher(() -> manager), catalog, MAPPER);
    }

    private static GeoServerEvent overTheWire(GeoServerEvent event) {
        byte[] json = MAPPER.writeValueAsBytes(event);
        return MAPPER.readValue(json, GeoServerEvent.class);
    }

    // the catalog returns its objects wrapped in a modification proxy, as needed by the event
    private static CatalogInfoModified passwordChange(Catalog catalog, String password) {
        WMSStoreInfo store = catalog.getStoreByName("wms1", WMSStoreInfo.class);
        store.setPassword(password);
        CatalogInfoModified event = CatalogInfoModified.createLocal(2, store);
        catalog.save(store);
        return event;
    }

    private static Catalog catalog(AssertableApplicationContext context) {
        return context.getBean("rawCatalog", Catalog.class);
    }

    private static GeoServerSecurityManager securityManager(AssertableApplicationContext context) {
        return context.getBean(GeoServerSecurityManager.class);
    }

    private static void addWorkspace(Catalog catalog) {
        WorkspaceInfo workspace = new WorkspaceInfoImpl();
        workspace.setName("ws1");
        catalog.add(workspace);
        NamespaceInfo namespace = new NamespaceInfoImpl();
        namespace.setPrefix("ws1");
        namespace.setURI("http://ws1");
        catalog.add(namespace);
    }

    private static WMSStoreInfo wmsStore(Catalog catalog, String password) {
        WMSStoreInfoImpl store = new WMSStoreInfoImpl(catalog);
        store.setId("wms1");
        store.setName("wms1");
        store.setWorkspace(catalog.getDefaultWorkspace());
        store.setEnabled(true);
        store.setCapabilitiesURL("http://example.com/wms");
        store.setUsername("user");
        store.setPassword(password);
        return store;
    }

    private static CoverageStoreInfo coverageStore(Catalog catalog, String secret) {
        CoverageStoreInfoImpl store = new CoverageStoreInfoImpl(catalog);
        store.setId("cs1");
        store.setName("cs1");
        store.setType("GeoTIFF");
        store.setWorkspace(catalog.getDefaultWorkspace());
        store.setEnabled(true);
        store.setURL("file:data/test.tif");
        store.getConnectionParameters().put(COVERAGE_SECRET, secret);
        store.getConnectionParameters().put("region", "eu-west-1");
        return store;
    }
}
