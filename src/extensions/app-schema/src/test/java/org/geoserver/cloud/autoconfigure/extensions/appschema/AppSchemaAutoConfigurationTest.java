/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.appschema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.complex.ComplexToSimpleOutputDispatcherCallback;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.ModuleStatus;
import org.geotools.api.data.DataAccessFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.vfny.geoserver.util.DataStoreUtils;

class AppSchemaAutoConfigurationTest {

    private ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean("geoServer", GeoServer.class, GeoServerImpl::new)
            .withPropertyValues(
                    "geoserver.data.filtering.vector-formats.[Application Schema DataAccess]=${geoserver.extension.appschema.enabled:false}")
            .withConfiguration(AutoConfigurations.of(AppSchemaAutoConfiguration.class));

    /**
     * The config property
     * {@code geoserver.data.filtering.vector-formats.[Application Schema
     * DataAccess]=${geoserver.extension.appschema.enabled:false}} shall make the
     * app schema datastore unavailable
     */
    @Test
    void testDisabledByDefault() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .doesNotHaveBean(ComplexToSimpleOutputDispatcherCallback.class)
                    .hasBean("appSchemaExtension")
                    .getBean("appSchemaExtension", ModuleStatus.class)
                    .hasFieldOrPropertyWithValue("available", true)
                    .hasFieldOrPropertyWithValue("enabled", false);
            Set<String> dataStoreFactories = getAvalableDataAccessFactoryNames();
            assertThat(dataStoreFactories).contains("Application Schema DataAccess");
        });
    }

    @Test
    void testEnabled() {
        runner.withPropertyValues("geoserver.extension.appschema.enabled=true").run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(ComplexToSimpleOutputDispatcherCallback.class)
                    .hasBean("appSchemaExtension")
                    .getBean("appSchemaExtension", ModuleStatus.class)
                    .hasFieldOrPropertyWithValue("available", true)
                    .hasFieldOrPropertyWithValue("enabled", true);

            Set<String> dataStoreFactories = getAvalableDataAccessFactoryNames();
            assertThat(dataStoreFactories).contains("Application Schema DataAccess");
        });
    }

    private Set<String> getAvalableDataAccessFactoryNames() {
        return DataStoreUtils.getAvailableDataStoreFactories().stream()
                .map(DataAccessFactory::getDisplayName)
                .collect(Collectors.toSet());
    }
}
