/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigBackendProperties;
import org.geoserver.gwc.config.GeoserverXMLResourceProvider;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.ConfigLoader;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.DefaultStorageFinder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

/**
 * Pure unit test for {@link PgconfigDiskQuotaBootstrap}.
 *
 * <p>Exercises the bootstrap directly with a real {@link FileSystemResourceStore}-backed pair of
 * {@link GeoserverXMLResourceProvider} instances; no Spring context, no DB, no JNDI. Verifies the
 * default DiskQuota XML files end up in the expected location with the right values, and that an
 * existing file is never overwritten.
 */
class PgconfigDiskQuotaBootstrapTest {

    @TempDir
    Path dataDir;

    private Path gwcDir() {
        return dataDir.resolve("gwc");
    }

    @Test
    void writesBothFilesWhenAbsent() throws Exception {
        PgconfigDiskQuotaBootstrap bootstrap = newBootstrap("java:comp/env/jdbc/pgconfig", "pgconfigschema");

        bootstrap.bootstrap();

        Path jdbc = gwcDir().resolve("geowebcache-diskquota-jdbc.xml");
        assertThat(jdbc).exists();
        String jdbcXml = Files.readString(jdbc);
        assertThat(jdbcXml)
                .contains("<dialect>PostgreSQL</dialect>")
                .contains("<JNDISource>java:comp/env/jdbc/pgconfig</JNDISource>")
                .contains("<schema>pgconfigschema</schema>");

        Path main = gwcDir().resolve("geowebcache-diskquota.xml");
        assertThat(main).exists();
        String mainXml = Files.readString(main);
        assertThat(mainXml).contains("<quotaStore>JDBC</quotaStore>").contains("<enabled>false</enabled>");
    }

    @Test
    void doesNotOverwriteExistingJdbcConfig() throws Exception {
        Files.createDirectories(gwcDir());
        Path jdbc = gwcDir().resolve("geowebcache-diskquota-jdbc.xml");
        String preexisting = "<gwcJdbcConfiguration><dialect>HSQL</dialect></gwcJdbcConfiguration>";
        Files.writeString(jdbc, preexisting);

        PgconfigDiskQuotaBootstrap bootstrap = newBootstrap("java:comp/env/jdbc/pgconfig", "pgconfigschema");
        bootstrap.bootstrap();

        assertThat(Files.readString(jdbc)).isEqualTo(preexisting);
    }

    @Test
    void doesNotOverwriteExistingMainConfig() throws Exception {
        Files.createDirectories(gwcDir());
        Path main = gwcDir().resolve("geowebcache-diskquota.xml");
        String preexisting = "<diskQuotaConfig><enabled>true</enabled></diskQuotaConfig>";
        Files.writeString(main, preexisting);

        PgconfigDiskQuotaBootstrap bootstrap = newBootstrap("java:comp/env/jdbc/pgconfig", "pgconfigschema");
        bootstrap.bootstrap();

        assertThat(Files.readString(main)).isEqualTo(preexisting);
    }

    @Test
    void defaultsSchemaToPublicWhenUnset() throws Exception {
        PgconfigDiskQuotaBootstrap bootstrap = newBootstrap("java:comp/env/jdbc/pgconfig", null);
        bootstrap.bootstrap();

        assertThat(Files.readString(gwcDir().resolve("geowebcache-diskquota-jdbc.xml")))
                .contains("<schema>public</schema>");
    }

    private PgconfigDiskQuotaBootstrap newBootstrap(String jndiName, String schema) throws ConfigurationException {
        FileSystemResourceStore resourceStore = new FileSystemResourceStore(dataDir.toFile());
        // Default config directory name is "gwc", resolved relative to the data-dir-rooted resource
        // store. This avoids the Resources.fromPath() branch that needs GeoServerExtensions wiring.
        GeoserverXMLResourceProvider mainProvider =
                new GeoserverXMLResourceProvider("geowebcache-diskquota.xml", resourceStore);
        GeoserverXMLResourceProvider jdbcProvider =
                new GeoserverXMLResourceProvider("geowebcache-diskquota-jdbc.xml", resourceStore);

        ConfigLoader configLoader = new ConfigLoader(
                mainProvider, Mockito.mock(DefaultStorageFinder.class), Mockito.mock(TileLayerDispatcher.class));

        PgconfigBackendProperties properties = new PgconfigBackendProperties();
        DataSourceProperties ds = new DataSourceProperties();
        ds.setJndiName(jndiName);
        properties.setDatasource(ds);
        if (schema != null) {
            properties.setSchema(schema);
        }

        return new PgconfigDiskQuotaBootstrap(configLoader, mainProvider, jdbcProvider, properties);
    }
}
