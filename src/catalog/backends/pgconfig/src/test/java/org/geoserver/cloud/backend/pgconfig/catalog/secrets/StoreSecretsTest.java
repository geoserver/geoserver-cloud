/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.secrets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.geoserver.cloud.security.secrets.SecurityManagerSupport.COVERAGE_SECRET;

import java.io.File;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigObjectMapper;
import org.geoserver.cloud.security.secrets.SecurityManagerSupport;
import org.geoserver.cloud.security.secrets.StoreSecretsCipher;
import org.geoserver.security.GeoServerSecurityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Encryption of store secrets with the keystore and encoders of a real security manager. */
class StoreSecretsTest {

    private static final ObjectMapper MAPPER = PgconfigObjectMapper.newObjectMapper();

    @TempDir
    File dataDirectory;

    private ApplicationContextRunner runner;

    @BeforeEach
    void setUp() {
        runner = SecurityManagerSupport.contextRunner(dataDirectory);
    }

    @Test
    void wmsPasswordEncrypted() {
        runInitialized((manager, secrets) -> {
            WMSStoreInfo store = wmsStore("wms-secret");

            JsonNode row = MAPPER.readTree(secrets.encrypt(store));

            assertThat(row.path("password").asString()).startsWith("crypt1:");
            assertThat(store.getPassword()).as("the store is not modified").isEqualTo("wms-secret");
        });
    }

    @Test
    void decryptRestoresClearText() {
        runInitialized((manager, secrets) -> {
            String row = secrets.encrypt(wmsStore("wms-secret"));

            WMSStoreInfo read = (WMSStoreInfo) decode(secrets.decrypt(row));
            WMSStoreInfo cached = (WMSStoreInfo) decode(secrets.decryptForCache(row.getBytes(UTF_8)));

            assertThat(read.getPassword()).isEqualTo("wms-secret");
            assertThat(cached.getPassword()).isEqualTo("wms-secret");
            assertThat(secrets.decryptedValues()).isEqualTo(2);
        });
    }

    @Test
    void providerFieldsEncrypted() {
        runInitialized((manager, secrets) -> {
            CoverageStoreInfo store = coverageStore("s3-secret");

            JsonNode row = MAPPER.readTree(secrets.encrypt(store));
            CoverageStoreInfo read = (CoverageStoreInfo) decode(secrets.decrypt(MAPPER.writeValueAsString(row)));

            assertThat(row.path("connectionParameters").path(COVERAGE_SECRET).asString())
                    .startsWith("crypt1:");
            assertThat(row.path("connectionParameters").path("region").asString())
                    .isEqualTo("eu-west-1");
            assertThat(read.getConnectionParameters()).containsEntry(COVERAGE_SECRET, "s3-secret");
        });
    }

    @Test
    void strongEncrypterUsed() {
        runInitialized((manager, secrets) -> {
            SecurityManagerSupport.useConfigEncrypter(manager, "strongPbePasswordEncoder");

            String row = secrets.encrypt(wmsStore("wms-secret"));
            WMSStoreInfo read = (WMSStoreInfo) decode(secrets.decrypt(row));

            assertThat(MAPPER.readTree(row).path("password").asString()).startsWith("crypt2:");
            assertThat(read.getPassword()).isEqualTo("wms-secret");
        });
    }

    // a store without secrets is not parsed
    @Test
    void clearTextReturnedAsIs() {
        runInitialized((manager, secrets) -> {
            String row = MAPPER.writeValueAsString(coverageStore(null));
            byte[] bytes = row.getBytes(UTF_8);

            assertThat(secrets.decrypt(row)).isSameAs(row);
            assertThat(secrets.decryptForCache(bytes)).isSameAs(bytes);
            assertThat(secrets.decryptedValues()).isZero();
        });
    }

    // vanilla fails the save; the row keeps the value it can decrypt
    @Test
    void encryptedValueKept() {
        runInitialized((manager, secrets) -> {
            String encrypted = MAPPER.readTree(secrets.encrypt(wmsStore("wms-secret")))
                    .path("password")
                    .asString();

            JsonNode row = MAPPER.readTree(secrets.encrypt(wmsStore(encrypted)));

            assertThat(row.path("password").asString()).isEqualTo(encrypted);
        });
    }

    // a database restored without its keystore must not make the catalog fail
    @Test
    void undecryptableStoreDisabled() {
        runInitialized((manager, secrets) -> {
            String row = MAPPER.writeValueAsString(wmsStore("crypt1:bm90IGVuY3J5cHRlZCB3aXRoIHRoaXMga2V5"));

            WMSStoreInfo read = (WMSStoreInfo) decode(secrets.decrypt(row));

            assertThat(read.isEnabled()).isFalse();
            assertThat(read.getPassword()).startsWith("crypt1:");
        });
    }

    @Test
    void notReadyKeepsCacheOut() {
        runInitialized((manager, initialized) -> {
            String row = initialized.encrypt(wmsStore("wms-secret"));
            StoreSecrets notReady = new StoreSecrets(new StoreSecretsCipher(() -> null));

            assertThat(notReady.decrypt(row)).isSameAs(row);
            byte[] bytes = row.getBytes(UTF_8);
            assertThatThrownBy(() -> notReady.decryptForCache(bytes)).isInstanceOf(SecretsNotReadyException.class);
        });
    }

    @Test
    void uninitializedWritesClearText() {
        runner.run(context -> {
            StoreSecrets secrets = new StoreSecrets(new StoreSecretsCipher(() -> securityManager(context)));

            JsonNode row = MAPPER.readTree(secrets.encrypt(wmsStore("wms-secret")));

            assertThat(row.path("password").asString()).isEqualTo("wms-secret");
        });
    }

    // rows written before encryption hold their secrets in clear text
    @Test
    void clearTextReadAsIs() {
        runInitialized((manager, secrets) -> {
            String row = MAPPER.writeValueAsString(wmsStore("wms-secret"));

            WMSStoreInfo read = (WMSStoreInfo) decode(secrets.decrypt(row));

            assertThat(read.getPassword()).isEqualTo("wms-secret");
        });
    }

    @Test
    void noneLeavesRowsAsTheyAre() {
        runInitialized((manager, secrets) -> {
            String encrypted = secrets.encrypt(wmsStore("wms-secret"));

            JsonNode written = MAPPER.readTree(StoreSecrets.none().encrypt(wmsStore("wms-secret")));

            assertThat(written.path("password").asString()).isEqualTo("wms-secret");
            assertThat(StoreSecrets.none().decrypt(encrypted)).isSameAs(encrypted);
        });
    }

    @FunctionalInterface
    interface SecretsTest {
        void run(GeoServerSecurityManager manager, StoreSecrets secrets) throws Exception;
    }

    private void runInitialized(SecretsTest test) {
        ContextConsumer<AssertableApplicationContext> consumer = context -> {
            GeoServerSecurityManager manager = SecurityManagerSupport.initialize(securityManager(context));
            test.run(manager, new StoreSecrets(new StoreSecretsCipher(() -> manager)));
        };
        runner.run(consumer);
    }

    private static GeoServerSecurityManager securityManager(AssertableApplicationContext context) {
        return context.getBean(GeoServerSecurityManager.class);
    }

    private static StoreInfo decode(String json) {
        return MAPPER.readValue(json, StoreInfo.class);
    }

    private static StoreInfo decode(byte[] json) {
        return MAPPER.readValue(json, StoreInfo.class);
    }

    private static WMSStoreInfo wmsStore(String password) {
        WMSStoreInfoImpl store = new WMSStoreInfoImpl((Catalog) null);
        store.setId("wms1");
        store.setName("wms1");
        store.setWorkspace(workspace());
        store.setEnabled(true);
        store.setCapabilitiesURL("http://example.com/wms");
        store.setUsername("user");
        store.setPassword(password);
        return store;
    }

    private static CoverageStoreInfo coverageStore(String secret) {
        CoverageStoreInfoImpl store = new CoverageStoreInfoImpl((Catalog) null);
        store.setId("cs1");
        store.setName("cs1");
        store.setType("COG");
        store.setWorkspace(workspace());
        store.setEnabled(true);
        store.setURL("s3://bucket/image.tif");
        store.getConnectionParameters().put("region", "eu-west-1");
        if (null != secret) {
            store.getConnectionParameters().put(COVERAGE_SECRET, secret);
        }
        return store;
    }

    private static WorkspaceInfoImpl workspace() {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("ws1");
        workspace.setName("ws1");
        return workspace;
    }
}
