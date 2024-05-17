/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Throwables;

import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geoserver.cloud.gwc.repository.CloudDefaultStorageFinder;
import org.geoserver.cloud.gwc.repository.CloudGwcXmlConfiguration;
import org.geoserver.cloud.gwc.repository.CloudXMLResourceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.io.File;
import java.io.IOException;

/**
 * @since 1.0
 */
class GwcCoreAutoConfigurationTest {

    @TempDir File tmpDir;
    WebApplicationContextRunner runner;

    @BeforeEach
    void setUp() {
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
    void defaultCacheDirectoryIsAFile() throws IOException {
        File file = new File(tmpDir, "file");
        assertTrue(file.createNewFile());
        runner = runner.withPropertyValues("gwc.cache-directory=" + file.getAbsolutePath());
        assertContextLoadFails(BeanInitializationException.class, "is not a directory");
    }

    @Test
    void contextLoads() {
        runner.run(
                context -> {
                    assertThat(context.isTypeMatch("gwcXmlConfig", CloudGwcXmlConfiguration.class))
                            .isTrue();
                    assertThat(
                                    context.isTypeMatch(
                                            "gwcXmlConfigResourceProvider",
                                            CloudXMLResourceProvider.class))
                            .isTrue();
                    assertThat(
                                    context.isTypeMatch(
                                            "gwcDefaultStorageFinder",
                                            CloudDefaultStorageFinder.class))
                            .isTrue();
                });
    }

    protected void assertContextLoadFails(
            Class<? extends Exception> expectedException, String expectedMessage) {
        runner.run(
                c -> {
                    Throwable startupFailure = c.getStartupFailure();
                    assertNotNull(startupFailure);
                    Throwable root = Throwables.getRootCause(startupFailure);
                    if (!expectedException.isInstance(root)) root.printStackTrace();
                    assertInstanceOf(expectedException, root);
                    if (null != expectedMessage)
                        assertThat(root.getMessage(), containsString(expectedMessage));
                });
    }
}
