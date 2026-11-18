/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.security.secrets;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Set;
import lombok.SneakyThrows;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.autoconfigure.main.GeoServerMainSecurityAutoConfiguration;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.password.EncryptedFieldsProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * A GeoServer security manager for tests, over a data directory of its own: the security configuration, the keystore
 * and the password encoders of a GeoServer instance, without the catalog under test. The data directory outlives the
 * application context, and a new context reads the keystore written by the previous one, as a restarted instance does.
 *
 * <p>The context declares an {@link EncryptedFieldsProvider} naming the {@value #COVERAGE_SECRET} parameter of coverage
 * stores, as the COG plugins do for their stores.
 */
public final class SecurityManagerSupport {

    /** The connection parameter of coverage stores declared as a secret. */
    public static final String COVERAGE_SECRET = "secretKey";

    private SecurityManagerSupport() {
        // static methods only
    }

    /** A context runner whose context holds a security manager over the data directory. */
    @SneakyThrows(Exception.class)
    public static ApplicationContextRunner contextRunner(File dataDirectory) {
        Catalog rawCatalog = new CatalogPlugin();
        SecureCatalogImpl secureCatalog = new SecureCatalogImpl(rawCatalog, new TestResourceAccessManager());
        FileSystemResourceStore resourceStore = new FileSystemResourceStore(dataDirectory);
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(resourceStore);
        GeoServerDataDirectory datadir = new GeoServerDataDirectory(resourceLoader);
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GeoServerMainSecurityAutoConfiguration.class))
                .withBean("extensions", GeoServerExtensions.class)
                .withBean(GeoServerConfigurationLock.class)
                .withBean(ResourceStore.class, () -> resourceStore)
                .withBean(GeoServerResourceLoader.class, () -> resourceLoader)
                .withBean("dataDirectory", GeoServerDataDirectory.class, () -> datadir)
                .withBean("rawCatalog", Catalog.class, () -> rawCatalog)
                .withBean("catalog", Catalog.class, () -> secureCatalog)
                .withBean("secureCatalog", Catalog.class, () -> secureCatalog)
                .withBean("geoServer", GeoServer.class, () -> mock(GeoServer.class))
                .withBean("updateSequence", UpdateSequence.class, () -> mock(UpdateSequence.class))
                .withBean("coverageSecrets", EncryptedFieldsProvider.class, CoverageSecrets::new)
                .withPropertyValues("logging.level.org.geoserver.platform: off");
    }

    /**
     * Initializes the security manager, as GeoServer does once its context is loaded. The first initialization over an
     * empty data directory creates the security configuration, the keystore and the master password.
     */
    @SneakyThrows(Exception.class)
    public static GeoServerSecurityManager initialize(GeoServerSecurityManager securityManager) {
        securityManager.reload();
        return securityManager;
    }

    /** Sets the encoder used to encrypt configuration passwords, by bean name. */
    @SneakyThrows(Exception.class)
    public static void useConfigEncrypter(GeoServerSecurityManager securityManager, String encoderName) {
        SecurityManagerConfig config = securityManager.loadSecurityConfig();
        config.setConfigPasswordEncrypterName(encoderName);
        securityManager.saveSecurityConfig(config);
    }

    static class CoverageSecrets implements EncryptedFieldsProvider {
        @Override
        public Set<String> getEncryptedFields(StoreInfo info) {
            return info instanceof CoverageStoreInfo ? Set.of(COVERAGE_SECRET) : Set.of();
        }
    }
}
