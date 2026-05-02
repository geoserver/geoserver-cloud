/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.backend;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigBackendProperties;
import org.geoserver.gwc.config.GeoserverXMLResourceProvider;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.ConfigLoader;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration;

/**
 * Writes default GeoWebCache DiskQuota config files on first start so the upstream JDBC quota
 * store binds to the pgconfig JNDI DataSource.
 *
 * <p>Two files are seeded, each only when absent:
 *
 * <ol>
 *   <li>{@code geowebcache-diskquota.xml}: a default {@link DiskQuotaConfig} with
 *       {@code quotaStore=JDBC} (and {@code enabled=false} so users opt in explicitly), written via
 *       {@link ConfigLoader#saveConfig(DiskQuotaConfig)}.
 *   <li>{@code geowebcache-diskquota-jdbc.xml}: a {@link JDBCConfiguration} with the
 *       {@code PostgreSQL} dialect, the pgconfig JNDI source, and the pgconfig schema, written
 *       via {@link JDBCConfiguration#store(JDBCConfiguration, OutputStream)}.
 * </ol>
 *
 * <p>Runs in {@link PostConstruct}: completes before any {@code ContextRefreshedEvent}, hence
 * before the {@code DiskQuotaMonitor} drives {@code ConfigurableQuotaStoreProvider#reloadQuotaStore},
 * so the upstream code reads the fresh files on the very first start. Idempotent: a second
 * invocation with both files already present is a no-op.
 * @since 2.28.3.1
 */
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.backend")
public class PgconfigDiskQuotaBootstrap {

    static final String DIALECT = "PostgreSQL";

    private final ConfigLoader configLoader;
    private final GeoserverXMLResourceProvider mainConfigResourceProvider;
    private final GeoserverXMLResourceProvider jdbcConfigResourceProvider;
    private final PgconfigBackendProperties pgconfigProperties;

    public PgconfigDiskQuotaBootstrap(
            ConfigLoader configLoader,
            GeoserverXMLResourceProvider mainConfigResourceProvider,
            GeoserverXMLResourceProvider jdbcConfigResourceProvider,
            PgconfigBackendProperties pgconfigProperties) {
        this.configLoader = configLoader;
        this.mainConfigResourceProvider = mainConfigResourceProvider;
        this.jdbcConfigResourceProvider = jdbcConfigResourceProvider;
        this.pgconfigProperties = pgconfigProperties;
    }

    @PostConstruct
    public void bootstrap() throws IOException, ConfigurationException {
        seedDiskQuotaConfigIfMissing();
        seedJdbcConfigIfMissing();
    }

    private void seedDiskQuotaConfigIfMissing() throws IOException, ConfigurationException {
        Resource configFile = configFile(mainConfigResourceProvider);
        if (Resources.exists(configFile)) {
            log.debug("{} already present; not writing defaults", mainConfigResourceProvider.getConfigFileName());
            return;
        }
        DiskQuotaConfig defaults = new DiskQuotaConfig();
        defaults.setDefaults();
        defaults.setEnabled(false);
        defaults.setQuotaStore("JDBC");
        configLoader.saveConfig(defaults);
        log.info("Bootstrapped {} with quotaStore=JDBC, enabled=false", mainConfigResourceProvider.getConfigFileName());
    }

    private void seedJdbcConfigIfMissing() throws IOException {
        Resource configFile = configFile(jdbcConfigResourceProvider);
        if (Resources.exists(configFile)) {
            log.debug("{} already present; not writing defaults", jdbcConfigResourceProvider.getConfigFileName());
            return;
        }
        String jndiName = pgconfigJndiName();
        String schema = pgconfigProperties.schema();
        JDBCConfiguration cfg = newJdbcConfiguration(jndiName, schema);
        try (OutputStream out = configFile.out()) {
            JDBCConfiguration.store(cfg, out);
        }
        log.info(
                "Bootstrapped {} (dialect=PostgreSQL, JNDISource={}, schema={})",
                jdbcConfigResourceProvider.getConfigFileName(),
                jndiName,
                schema);
    }

    private Resource configFile(GeoserverXMLResourceProvider resourceProvider) {
        Resource configDir = resourceProvider.getConfigDirectory();
        return configDir.get(resourceProvider.getConfigFileName());
    }

    private String pgconfigJndiName() {
        return pgconfigProperties.getDatasource().getJndiName();
    }

    private JDBCConfiguration newJdbcConfiguration(String jndiName, String schema) {
        JDBCConfiguration cfg = new JDBCConfiguration();
        cfg.setDialect(DIALECT);
        cfg.setJNDISource(jndiName);
        cfg.setSchema(schema);
        return cfg;
    }
}
