/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jdbcconfig;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;

/**
 * Overrides {@link #loadGeoServer(GeoServer, XStreamPersister)} to avoid a class cast exception on
 * {@link GeoServerImpl} (we're using {@link org.geoserver.config.plugin.GeoServerImpl}), and
 * because we don't do import, and other methods to avoid coupling on {@link JDBCCatalogFacade} just
 * to get a handle to the {@link ConfigDatabase}
 */
@Slf4j
public class CloudJdbcGeoServerLoader extends DefaultGeoServerLoader {

    private JDBCConfigProperties config;

    private ConfigDatabase configdb;

    private String lockProviderBeanName;

    public CloudJdbcGeoServerLoader(
            GeoServerResourceLoader resourceLoader,
            JDBCConfigProperties config,
            ConfigDatabase configdb)
            throws Exception {
        super(resourceLoader);
        this.config = config;
        this.configdb = configdb;
    }

    /** */
    public void setInitializingLockProviderName(String lockProviderBeanName) {
        this.lockProviderBeanName = lockProviderBeanName;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void loadGeoServer(GeoServer geoServer, XStreamPersister xp) throws Exception {
        if (geoServer.getGlobal() == null) {
            geoServer.setGlobal(geoServer.getFactory().createGlobal());
        }
        if (geoServer.getLogging() == null) {
            geoServer.setLogging(geoServer.getFactory().createLogging());
        }
        // ensure we have a service configuration for every service we know about
        final List<XStreamServiceLoader> loaders =
                GeoServerExtensions.extensions(XStreamServiceLoader.class);
        for (XStreamServiceLoader l : loaders) {
            ServiceInfo s = geoServer.getService(l.getServiceClass());
            if (s == null) {
                geoServer.add(l.create(geoServer));
            }
        }
        // initialized log provider if unset
        if (this.lockProviderBeanName != null) {
            GeoServerInfo global = geoServer.getGlobal();
            if (global.getLockProviderName() == null) {
                global.setLockProviderName(lockProviderBeanName);
                geoServer.save(global);
            } else if (!lockProviderBeanName.equals(global.getLockProviderName())) {
                log.warn(
                        "Configured lock provider {} doesn't match default {}, please make sure that's what you want",
                        global.getLockProviderName(),
                        lockProviderBeanName);
            }
        }
    }

    @Override
    protected void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        loadCatalogInternal((CatalogPlugin) catalog, xp);
        catalog.addListener(new GeoServerResourcePersister(catalog));
    }

    private void loadCatalogInternal(CatalogPlugin catalog, XStreamPersister xp) throws Exception {
        if (!config.isInitDb() && !config.isImport() && config.isRepopulate()) {
            ConfigDatabase configDatabase = this.configdb;
            configDatabase.repopulateQueryableProperties();
            config.setRepopulate(false);
            config.save();
        }
    }
}
