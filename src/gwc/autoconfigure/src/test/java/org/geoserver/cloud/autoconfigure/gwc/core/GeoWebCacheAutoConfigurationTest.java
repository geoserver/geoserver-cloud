/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.core;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Throwables;
import java.io.File;
import java.io.IOException;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geoserver.cloud.gwc.config.core.GeoWebCacheConfigurationProperties;
import org.geoserver.cloud.gwc.repository.CloudDefaultStorageFinder;
import org.geoserver.cloud.gwc.repository.CloudGwcXmlConfiguration;
import org.geoserver.cloud.gwc.repository.CloudXMLResourceProvider;
import org.geoserver.gwc.ConfigurableLockProvider;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.GeoServerLockProvider;
import org.geoserver.gwc.config.AbstractGwcInitializer;
import org.geoserver.gwc.config.DefaultGwcInitializer;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geowebcache.locks.LockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * @since 1.0
 */
class GeoWebCacheAutoConfigurationTest {

    @TempDir
    File tmpDir;

    WebApplicationContextRunner runner;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp(@TempDir File tmpDir) {
        System.clearProperty("GEOWEBCACHE_CACHE_DIR");
        runner = GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir);
    }

    @Test
    void defaultCacheDirectoryConfigPropertyIsMandatory() {
        runner = runner.withPropertyValues("gwc.cache-directory="); // null-ify it
        assertContextLoadFails(InvalidPropertyException.class, "gwc.cache-directory is not set");
    }

    @Test
    void defaultCacheDirectoryIsAbsolutePath() {
        runner = runner.withPropertyValues("gwc.cache-directory=relative/path");
        assertContextLoadFails(BeanInitializationException.class, "must be an absolute path");
    }

    @Test
    void defaultCacheDirectoryIsAFile(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "file");
        assertTrue(file.createNewFile());
        runner = runner.withPropertyValues("gwc.cache-directory=" + file.getAbsolutePath());
        assertContextLoadFails(BeanInitializationException.class, "is not a directory");
    }

    @Test
    void lockProviderDelegatesStoGeoSeverLockProvider() {
        runner.run(context -> {
            GeoServerExtensionsHelper.init(context);
            assertThat(context)
                    .hasNotFailed()
                    .hasBean(AbstractGwcInitializer.GWC_LOCK_PROVIDER_BEAN_NAME)
                    .getBean(AbstractGwcInitializer.GWC_LOCK_PROVIDER_BEAN_NAME)
                    .isInstanceOf(GeoServerLockProvider.class);

            GWCConfigPersister persister = context.getBean(GWCConfigPersister.class);
            GWCConfig config = persister.getConfig();
            assertThat(config.getLockProviderName()).isEqualTo(AbstractGwcInitializer.GWC_LOCK_PROVIDER_BEAN_NAME);

            GWC gwc = GWC.get();

            LockProvider lockProvider = gwc.getLockProvider();
            assertThat(lockProvider).isInstanceOf(ConfigurableLockProvider.class);
            GeoServerLockProvider expected =
                    context.getBean(AbstractGwcInitializer.GWC_LOCK_PROVIDER_BEAN_NAME, GeoServerLockProvider.class);
            assertThat(((ConfigurableLockProvider) lockProvider).getDelegate()).isSameAs(expected);
        });
    }

    @Test
    void contextLoads() {
        runner.run(context -> {
            GeoServerExtensionsHelper.init(context);
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("gwcInitializer")
                    .getBean("gwcInitializer")
                    .isInstanceOf(DefaultGwcInitializer.class);

            assertThat(context.isTypeMatch("gwcXmlConfig", CloudGwcXmlConfiguration.class))
                    .isTrue();
            assertThat(context.isTypeMatch("gwcXmlConfigResourceProvider", CloudXMLResourceProvider.class))
                    .isTrue();
            assertThat(context.isTypeMatch("gwcDefaultStorageFinder", CloudDefaultStorageFinder.class))
                    .isTrue();
        });
    }

    protected void assertContextLoadFails(Class<? extends Exception> expectedException, String expectedMessage) {
        runner.run(context -> {
            GeoServerExtensionsHelper.init(context);
            Throwable startupFailure = context.getStartupFailure();
            assertNotNull(startupFailure);
            Throwable root = Throwables.getRootCause(startupFailure);
            if (!expectedException.isInstance(root)) {
                root.printStackTrace();
            }
            assertInstanceOf(expectedException, root);
            if (null != expectedMessage) {
                assertThat(root.getMessage(), containsString(expectedMessage));
            }
        });
    }

    /**
     * Note this test will fail without {@code --add-opens=java.base/java.util=ALL-UNNAMED} JVM
     * parameter (watch out if running it from the IDE, maven is configured to set it)
     */
    @Test
    void defaultCacheDirectoryFromEnvVariable() throws Exception {
        File dir = new File(tmpDir, "env_cachedir");
        assertThat(dir).doesNotExist();
        String dirpath = dir.getAbsolutePath();
        withEnvironmentVariable("GEOWEBCACHE_CACHE_DIR", dirpath).execute(() -> {
            assertThat(System.getenv("GEOWEBCACHE_CACHE_DIR")).isEqualTo(dirpath);
            runner.withPropertyValues("gwc.cache-directory: ${GEOWEBCACHE_CACHE_DIR}")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(dir).isDirectory();

                        var gwcConfigProps = context.getBean(GeoWebCacheConfigurationProperties.class);
                        assertThat(gwcConfigProps.getCacheDirectory()).isEqualTo(dir.toPath());
                    });
        });
        assertThat(System.getenv("GEOWEBCACHE_CACHE_DIR")).isNull();
    }

    @Test
    void defaultCacheDirectoryFromSystemProperty() {
        File dir = new File(tmpDir, "sysprop_cachedir");
        assertThat(dir).doesNotExist();
        String dirpath = dir.getAbsolutePath();

        System.setProperty("GEOWEBCACHE_CACHE_DIR", dirpath);
        try {
            runner.withPropertyValues("gwc.cache-directory: ${GEOWEBCACHE_CACHE_DIR}")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(dir).isDirectory();
                    });
        } finally {
            System.clearProperty("GEOWEBCACHE_CACHE_DIR");
        }
    }
}
