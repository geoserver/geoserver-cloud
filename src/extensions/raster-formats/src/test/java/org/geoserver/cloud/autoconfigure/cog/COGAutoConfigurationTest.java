/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.cog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.catalog.Catalog;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.store.cog.panel.CogRasterEditPanel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @since 1.0
 */
class COGAutoConfigurationTest {

    private final Catalog mockCatalog = mock(Catalog.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean("catalog", Catalog.class, () -> mockCatalog)
            .withConfiguration(AutoConfigurations.of(COGAutoConfiguration.class, COGWebUIAutoConfiguration.class));

    @Test
    void testWebUIUnavailable() {
        FilteredClassLoader hiddenClasses =
                new FilteredClassLoader(GeoServerApplication.class, CogRasterEditPanel.class);

        contextRunner.withClassLoader(hiddenClasses).run(context -> {
            assertThat(context).hasNotFailed();

            for (String beanName : nonWebuiBeanNames()) {
                assertThat(context).hasBean(beanName);
            }

            for (String webuiBeanName : webuiBeanNames()) {
                assertThat(context).doesNotHaveBean(webuiBeanName);
            }
        });
    }

    @Test
    void testWebUIAvailable() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            for (String beanName : nonWebuiBeanNames()) {
                assertThat(context).hasBean(beanName);
            }
            for (String webuiBeanName : webuiBeanNames()) {
                assertThat(context).hasBean(webuiBeanName);
            }
        });
    }

    private String[] nonWebuiBeanNames() {
        return new String[] {
            "coverageReaderInputObjectCogConverter",
            "cogSettingsInitializer",
            "cogSettingsXStreamInitializer",
            "cogEncryptedFieldsProvider"
        };
    }

    private String[] webuiBeanNames() {
        return COGWebUIAutoConfiguration.WEBUI_BEAN_NAMES.split("\\|");
    }
}
