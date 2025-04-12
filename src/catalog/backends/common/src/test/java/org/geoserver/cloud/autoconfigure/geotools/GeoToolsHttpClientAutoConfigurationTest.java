/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.geotools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.geotools.http.HTTPClient;
import org.geotools.http.HTTPClientFinder;
import org.geotools.util.factory.Hints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * @since 1.0
 */
class GeoToolsHttpClientAutoConfigurationTest {

    private WebApplicationContextRunner runner = new WebApplicationContextRunner() //
            .withInitializer(new GeoToolsStaticContextInitializer()) //
            .withConfiguration(AutoConfigurations.of(GeoToolsHttpClientAutoConfiguration.class));

    private final String forceXYSystemProperty = "org.geotools.referencing.forceXY";

    @BeforeEach
    void clearSystemPropertiesAndGeoToolsHints() {
        Hints.removeSystemDefault(Hints.HTTP_CLIENT_FACTORY);
        System.clearProperty(forceXYSystemProperty);
    }

    @Test
    void enabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(GeoToolsHttpClientProxyConfigurationProperties.class);
            assertThat(context).hasSingleBean(SpringEnvironmentAwareGeoToolsHttpClientFactory.class);
            assertThat(context.getBean(GeoToolsHttpClientProxyConfigurationProperties.class))
                    .hasFieldOrPropertyWithValue("enabled", true);

            HTTPClient client = HTTPClientFinder.createClient();
            assertThat(client)
                    .as(
                            """
                                    Expected SpringEnvironmentAwareGeoToolsHttpClient \
                                    after GeoToolsStaticContextInitializer sets \
                                    SpringEnvironmentAwareGeoToolsHttpClientFactory as the default factory
                                    """)
                    .isInstanceOf(SpringEnvironmentAwareGeoToolsHttpClient.class);
        });
    }

    @Test
    void testInitializerSetsForceXYSystemProperty() {
        assertNull(System.getProperty(forceXYSystemProperty));
        runner.run(
                context -> assertThat(System.getProperty(forceXYSystemProperty)).isEqualTo("true"));
    }

    @Test
    void testInitializerSetsHttpClientFactorySystemProperty() {
        final var expected = SpringEnvironmentAwareGeoToolsHttpClientFactory.class;

        assertNull(Hints.getSystemDefault(Hints.HTTP_CLIENT_FACTORY));
        runner.run(context ->
                assertThat(Hints.getSystemDefault(Hints.HTTP_CLIENT_FACTORY)).isEqualTo(expected));

        Hints.removeSystemDefault(Hints.HTTP_CLIENT_FACTORY);
        runner.withPropertyValues("geotools.httpclient.proxy.enabled: true")
                .run(context -> assertThat(Hints.getSystemDefault(Hints.HTTP_CLIENT_FACTORY))
                        .isEqualTo(expected));

        Hints.removeSystemDefault(Hints.HTTP_CLIENT_FACTORY);
        runner.withPropertyValues("geotools.httpclient.proxy.enabled: false")
                .run(context -> assertThat(Hints.getSystemDefault(Hints.HTTP_CLIENT_FACTORY))
                        .isNull());
    }
}
