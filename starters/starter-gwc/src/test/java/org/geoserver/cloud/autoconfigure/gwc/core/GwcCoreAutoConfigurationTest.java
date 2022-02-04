/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.core;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Throwables;
import java.io.File;
import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.config.datadirectory.NoServletContextDataDirectoryResourceStore;
import org.geoserver.cloud.gwc.repository.CloudDefaultStorageFinder;
import org.geoserver.cloud.gwc.repository.CloudGwcXmlConfiguration;
import org.geoserver.cloud.gwc.repository.CloudXMLResourceProvider;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.ResourceStoreFactory;
import org.geoserver.security.GeoServerSecurityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/** @since 1.0 */
class GwcCoreAutoConfigurationTest {

    WebApplicationContextRunner runner =
            new WebApplicationContextRunner()
                    .withAllowBeanDefinitionOverriding(true)
                    .withAllowCircularReferences(true)
                    .withConfiguration(AutoConfigurations.of(GeoWebCacheAutoConfiguration.class));

    @TempDir File tmpDir;

    /** @throws java.lang.Exception */
    @BeforeEach
    void setUp() throws Exception {
        runner =
                setUpRequiredGeoServerBeans()
                        .withConfiguration(
                                AutoConfigurations.of(GeoWebCacheAutoConfiguration.class))
                        .withPropertyValues("gwc.cache-directory=" + tmpDir.toString());
    }

    private WebApplicationContextRunner setUpRequiredGeoServerBeans() throws Exception {
        Catalog catalog = new CatalogPlugin();
        GeoServer geoserver = new GeoServerImpl();
        geoserver.setCatalog(catalog);
        ResourceStore store = new NoServletContextDataDirectoryResourceStore(tmpDir);
        GeoServerResourceLoader gsReourceLoader = new GeoServerResourceLoader(tmpDir);
        GeoServerDataDirectory datadir = new GeoServerDataDirectory(gsReourceLoader);
        GeoServerSecurityManager geoServerSecurityManager = new GeoServerSecurityManager(datadir);
        return new WebApplicationContextRunner()
                .withAllowBeanDefinitionOverriding(true)
                .withAllowCircularReferences(true)
                .withBean("rawCatalog", Catalog.class, () -> catalog)
                .withBean("catalog", Catalog.class, () -> catalog)
                .withBean("geoServer", GeoServer.class, () -> geoserver)
                .withBean("resourceStore", ResourceStoreFactory.class)
                .withBean("resourceStoreImpl", ResourceStore.class, () -> store)
                .withBean("resourceLoader", GeoServerResourceLoader.class, () -> gsReourceLoader)
                .withBean("extensions", GeoServerExtensions.class, () -> new GeoServerExtensions())
                .withBean(
                        "geoServerSecurityManager",
                        GeoServerSecurityManager.class,
                        () -> geoServerSecurityManager)
                .withBean("xstreamPersisterFactory", XStreamPersisterFactory.class)
                .withBean("dispatcher", org.geoserver.ows.Dispatcher.class);
    }

    public @Test void defaultCacheDirectoryConfigPropertyIsMandatory() {
        runner = runner.withPropertyValues("gwc.cache-directory="); // null-ify it
        assertContextLoadFails(InvalidPropertyException.class, "gwc.cache-directory is not set");
    }

    public @Test void defaultCacheDirectoryIsAbsolutePath() {
        runner = runner.withPropertyValues("gwc.cache-directory=relative/path");
        assertContextLoadFails(BeanInitializationException.class, "must be an absolute path");
    }

    public @Test void defaultCacheDirectoryIsAFile() throws IOException {
        File file = new File(tmpDir, "file");
        assertTrue(file.createNewFile());
        runner = runner.withPropertyValues("gwc.cache-directory=" + file.getAbsolutePath());
        assertContextLoadFails(BeanInitializationException.class, "is not a directory");
    }

    public @Test void contextLoads() throws IOException {
        runner.run(
                context -> {
                    context.isTypeMatch("gwcXmlConfig", CloudGwcXmlConfiguration.class);
                    context.isTypeMatch(
                            "gwcXmlConfigResourceProvider", CloudXMLResourceProvider.class);
                    context.isTypeMatch("gwcDefaultStorageFinder", CloudDefaultStorageFinder.class);
                });
    }

    protected void assertContextLoadFails(
            Class<? extends Exception> expectedException, String expectedMessage) {
        runner.run(
                c -> {
                    IllegalStateException expected =
                            assertThrows(IllegalStateException.class, () -> c.isRunning());
                    Throwable root = Throwables.getRootCause(expected);
                    if (!expectedException.isInstance(root)) root.printStackTrace();
                    assertInstanceOf(expectedException, root);
                    if (null != expectedMessage)
                        assertThat(root.getMessage(), containsString(expectedMessage));
                });
    }
}
