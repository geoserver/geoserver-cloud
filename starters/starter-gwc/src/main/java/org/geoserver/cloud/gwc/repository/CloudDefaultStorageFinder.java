/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.repository;

import java.nio.file.Path;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.storage.DefaultStorageFinder;
import org.geowebcache.util.ApplicationContextProvider;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;

/** @since 1.0 */
public class CloudDefaultStorageFinder extends DefaultStorageFinder {

    private Environment environment;

    static final ApplicationContextProvider NOOP =
            new ApplicationContextProvider() {
                public @Override WebApplicationContext getApplicationContext() {
                    return null;
                }
            };

    private Path defaultCacheDirectory;

    public CloudDefaultStorageFinder(Path defaultCacheDirectory, Environment environment) {
        super(NOOP);
        this.defaultCacheDirectory = defaultCacheDirectory;
        this.environment = environment;
    }

    public @Override String getDefaultPath() throws ConfigurationException {
        return defaultCacheDirectory.toString();
    }

    public @Override String findEnvVar(String varStr) {
        return environment.getProperty(varStr);
    }
}
