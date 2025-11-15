/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.acl.authorization.cache.CachingAuthorizationService;
import org.geoserver.acl.autoconfigure.messaging.bus.AclSpringCloudBusAutoConfiguration;
import org.geoserver.acl.config.authorization.cache.CachingAuthorizationServiceConfiguration;
import org.geoserver.acl.config.webapi.client.ApiClientProperties;
import org.geoserver.acl.messaging.bus.RemoteAclRuleEventsBridge;
import org.geoserver.acl.plugin.accessmanager.AclResourceAccessManager;
import org.geoserver.acl.plugin.accessmanager.AclResourceAccessManagerSpringConfig;
import org.geoserver.acl.plugin.config.accessmanager.AclWebApiAccessManagerConfiguration;
import org.geoserver.acl.plugin.config.cache.CachingAuthorizationServicePluginConfiguration;
import org.geoserver.acl.plugin.config.webapi.ApiClientConfiguration;
import org.geoserver.acl.plugin.config.webui.AclWebUIConfiguration;
import org.geoserver.acl.plugin.config.wps.AclWpsIntegrationConfiguration;
import org.geoserver.acl.plugin.config.wps.WPSResourceManagerClassCondition;
import org.geoserver.acl.plugin.web.css.CSSConfiguration;
import org.geoserver.acl.plugin.wps.AclWPSHelperImpl;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWPS;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.cloud.autoconfigure.extensions.security.acl.AclExtensionAutoConfiguration.AclCachingAutoConfiguration;
import org.geoserver.cloud.autoconfigure.extensions.security.acl.AclExtensionAutoConfiguration.AclWebApiAccessManagerAutoConfiguration;
import org.geoserver.cloud.autoconfigure.extensions.security.acl.AclExtensionAutoConfiguration.AclWebUIAutoConfiguration;
import org.geoserver.cloud.autoconfigure.extensions.security.acl.AclExtensionAutoConfiguration.AclWpsAutoConfiguration;
import org.geoserver.config.GeoServer;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.geoserver.web.Category;
import org.geoserver.wps.resource.WPSResourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.BusBridge;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.PathDestinationFactory;

/**
 * Test suite for {@link AclExtensionAutoConfiguration}
 *
 * @since 2.27.0.0
 */
class AclExtensionAutoConfigurationTest {

    private ApplicationContextRunner runner;

    @BeforeEach
    void setup() {
        // Create a mock GeoServer instance to satisfy @ConditionalOnGeoServer
        var mockGeoServer = mock(GeoServer.class);

        runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AclExtensionAutoConfiguration.class))
                .withBean("rawCatalog", Catalog.class, CatalogImpl::new)
                .withBean(
                        "layerGroupContainmentCache",
                        LayerGroupContainmentCache.class,
                        () -> mock(LayerGroupContainmentCache.class))
                .withBean("geoServer", GeoServer.class, () -> mockGeoServer)
                .withBean(GeoServerSecurityManager.class, () -> mock(GeoServerSecurityManager.class))
                .withPropertyValues( // tie geoserver.client.enabled to the extension.enabled property for simplicity
                        "geoserver.acl.client.enabled: ${geoserver.extension.security.acl.enabled}",
                        // disable API client start up check to test without it trying to connect
                        "geoserver.acl.client.startupCheck: false",
                        "geoserver.acl.client.basePath: http://acl.test:9000");
    }

    private void enableExtension() {
        runner = runner.withPropertyValues("geoserver.extension.security.acl.enabled: true");
    }

    private void disableExtension() {
        runner = runner.withPropertyValues("geoserver.extension.security.acl.enabled: false");
    }

    private void enableApiClientImplementation() {
        runner = runner.withPropertyValues("geoserver.acl.client.enabled: true");
    }

    private void disableApiClientImplementation() {
        runner = runner.withPropertyValues("geoserver.acl.client.enabled: false");
    }

    @Test
    void extensionDisabledByDefault() {
        assertExtensionUnavailable();
    }

    @Test
    void extensionExplicitlyDisabled() {
        assertExtensionUnavailable("geoserver.extension.security.acl.enabled=false");
    }

    @Test
    void extensionExplicitlyEnabled() {
        assertExtensionAvailable("geoserver.extension.security.acl.enabled=true");
    }

    @Test
    void extensionFailsIfNoAclResourceAccessManagerPresent() {
        enableExtension();
        disableApiClientImplementation();
        runner.run(context -> assertThat(context)
                .as(
                        "when the GeoServer ACL extension is enabled without an AclResourceAccessManager it should prevent startup")
                .hasFailed()
                .getFailure()
                .hasRootCauseMessage("No AclResourceAccessManager configured"));
    }

    private void assertExtensionAvailable(String... configProperties) {
        runner.withPropertyValues(configProperties).run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(AclExtensionAutoConfiguration.class)
                    .getBean(AclExtensionConfigurationProperties.class)
                    .hasFieldOrPropertyWithValue("enabled", true);
        });
    }

    private void assertExtensionUnavailable(String... configProperties) {
        runner.withPropertyValues(configProperties).run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .doesNotHaveBean(AclExtensionAutoConfiguration.class)
                    .doesNotHaveBean(AclExtensionConfigurationProperties.class);
        });
    }

    /**
     * Test {@link AclWebApiAccessManagerAutoConfiguration}
     */
    @Nested
    class AclWebApiAccessManagerAutoConfigurationTest {

        @BeforeEach
        void setUpExtension() {
            enableExtension();
            enableApiClientImplementation();
            runner = runner.withPropertyValues("geoserver.acl.client.enabled: true");
        }

        @Test
        void extensionDisabledIgnoresApiClientEnabled() {
            disableExtension();
            enableApiClientImplementation();
            runner.withPropertyValues("geoserver.acl.client.enabled: false").run(context -> assertThat(context)
                    .hasNotFailed()
                    .doesNotHaveBean(AclWebApiAccessManagerAutoConfiguration.class));
        }

        @Test
        void extensionAndApiClientEnabled() {
            enableExtension();
            enableApiClientImplementation();
            runner.run(context -> assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(AclWebApiAccessManagerConfiguration.class)
                    .hasSingleBean(ApiClientConfiguration.class)
                    .hasSingleBean(ApiClientProperties.class)
                    .hasSingleBean(AclResourceAccessManagerSpringConfig.class)
                    .hasSingleBean(AclResourceAccessManager.class));
        }
    }

    /**
     * Test {@link AclWebUIAutoConfiguration}
     */
    @Nested
    class AclWebUIAutoConfigurationTest {

        @BeforeEach
        void setUpExtension() {
            enableExtension();
            enableApiClientImplementation();
            runner = runner.withBean("securityCategory", Category.class);
        }

        /**
         * @see ConditionalOnGeoServerWebUI
         */
        private void enableConditionalOnGeoServerWebUI() {
            runner = runner.withPropertyValues("geoserver.service.webui.enabled: true");
        }

        private void disableConditionalOnGeoServerWebUI() {
            runner = runner.withPropertyValues("geoserver.service.webui.enabled: false");
        }

        private void enableConditionalOnWebuiAclEnabled() {
            runner = runner.withPropertyValues("geoserver.web-ui.acl.enabled: true");
        }

        private void disableConditionalOnWebuiAclEnabled() {
            runner = runner.withPropertyValues("geoserver.web-ui.acl.enabled: false");
        }

        @Test
        void disabledByDefault() {
            assertAclWebuiUnavailable();
        }

        @Test
        void testConditionalOnGeoServerWebUI() {
            enableConditionalOnWebuiAclEnabled();

            disableConditionalOnGeoServerWebUI();
            assertAclWebuiUnavailable();

            enableConditionalOnGeoServerWebUI();
            assertAclWebuiAvailable();
        }

        @Test
        void testWebUiEnabledWithAclEnabledByDefault() {
            enableConditionalOnGeoServerWebUI();

            disableConditionalOnWebuiAclEnabled();
            assertAclWebuiUnavailable();

            enableConditionalOnWebuiAclEnabled();
            assertAclWebuiAvailable();
        }

        private void assertAclWebuiAvailable() {
            runner.withPropertyValues().run(context -> {
                assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(AclResourceAccessManager.class)
                        .hasSingleBean(AclWebUIAutoConfiguration.class)
                        .hasSingleBean(AclWebUIConfiguration.class)
                        .hasSingleBean(CSSConfiguration.class)
                        .hasBean("aclServiceConfigPageMenuInfo")
                        .hasBean("accessRulesACLPageMenuInfo")
                        .hasBean("adminRulesAclPageMenuInfo");
            });
        }

        private void assertAclWebuiUnavailable() {
            runner.run(context -> {
                assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(AclWebUIAutoConfiguration.class)
                        .doesNotHaveBean(AclWebUIConfiguration.class)
                        .doesNotHaveBean(CSSConfiguration.class)
                        .doesNotHaveBean("aclServiceConfigPageMenuInfo")
                        .doesNotHaveBean("accessRulesACLPageMenuInfo")
                        .doesNotHaveBean("adminRulesAclPageMenuInfo");
            });
        }
    }

    /**
     * Test {@link AclCachingAutoConfiguration}
     */
    @Nested
    class AclCachingAutoConfigurationTest {

        @BeforeEach
        void setUpExtension() {
            enableExtension();
            enableApiClientImplementation();
        }

        private void addCacheManager() {
            runner = runner.withBean(CacheManager.class, () -> mock(CacheManager.class));
        }

        @Test
        void conditionalOnSpringCacheManager() {
            assertCachingUnavailable();

            addCacheManager();

            assertCachingAvailable();
        }

        @Test
        void conditionalOnClientCachingEnabledProperty() {
            addCacheManager();

            runner = runner.withPropertyValues("geoserver.acl.client.caching: false");
            assertCachingUnavailable();

            runner = runner.withPropertyValues("geoserver.acl.client.caching: true");
            assertCachingAvailable();
        }

        private void assertCachingUnavailable() {
            runner.run(context -> assertThat(context)
                    .hasNotFailed()
                    .doesNotHaveBean(AclCachingAutoConfiguration.class)
                    .doesNotHaveBean(CachingAuthorizationServicePluginConfiguration.class)
                    .doesNotHaveBean(CachingAuthorizationServiceConfiguration.class)
                    .doesNotHaveBean(CachingAuthorizationService.class));
        }

        private void assertCachingAvailable() {
            runner.run(context -> assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(AclCachingAutoConfiguration.class)
                    .hasSingleBean(CachingAuthorizationServicePluginConfiguration.class)
                    .hasSingleBean(CachingAuthorizationServiceConfiguration.class)
                    .hasSingleBean(CachingAuthorizationService.class));
        }
    }

    /**
     * Test {@link AclWpsAutoConfiguration}
     */
    @Nested
    class AclWpsAutoConfigurationTest {

        @BeforeEach
        void setUpExtension() {
            enableExtension();
            enableApiClientImplementation();
            runner = runner.withBean(
                    "wpsResourceManager", WPSResourceManager.class, () -> mock(WPSResourceManager.class));
        }

        @AfterEach
        void resetWPSResourceManagerClassCondition() {
            WPSResourceManagerClassCondition.classLoader(null);
        }

        /**
         * @see ConditionalOnGeoServerWPS
         */
        private void enableConditionalOnGeoServerWPS() {
            runner = runner.withPropertyValues("geoserver.service.wps.enabled: true");
        }

        private void disableConditionalOnGeoServerWPS() {
            runner = runner.withPropertyValues("geoserver.service.wps.enabled: false");
        }

        @Test
        void conditionalOnGeoServerWPS() {
            disableConditionalOnGeoServerWPS();
            runner.run(context -> assertThat(context)
                    .hasNotFailed()
                    .doesNotHaveBean(AclWpsAutoConfiguration.class)
                    .doesNotHaveBean(AclWpsIntegrationConfiguration.class)
                    .doesNotHaveBean(AclWPSHelperImpl.class));

            enableConditionalOnGeoServerWPS();
            runner.run(context -> assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(AclWpsAutoConfiguration.class)
                    .hasSingleBean(AclWpsIntegrationConfiguration.class)
                    .hasSingleBean(AclWPSHelperImpl.class));
        }
    }

    /**
     * {@link AclSpringCloudBusAutoConfiguration} is already an auto configuration from {@code gs-acl-spring-cloud-bus-adapter} let's test its enablement
     */
    @Nested
    class AclSpringCloudBusAutoConfigurationTest {

        @BeforeEach
        void setUpExtension() {
            enableExtension();
            enableApiClientImplementation();

            // fake Spring Cloud Bus required beans by org.springframework.cloud.bus.BusAutoConfiguration
            runner = runner.withBean(ServiceMatcher.class, () -> mock(ServiceMatcher.class))
                    .withBean(BusBridge.class, () -> mock(BusBridge.class))
                    .withConfiguration(AutoConfigurations.of(BusAutoConfiguration.class));
        }

        /**
         * {@code geoserver.bus.enabled} is true, depends on whether Spring Cloud Bus is
         * available
         */
        @Test
        void conditionalOnSpringCloudBusEnabled() {
            runner = runner.withPropertyValues("geoserver.bus.enabled: true")
                    .withPropertyValues("spring.cloud.bus.enabled: false");

            assertAclSpringCloudBusUnavailable();
            runner = runner.withPropertyValues("spring.cloud.bus.enabled: true");
            assertAclSpringCloudBusAvailable();
        }

        /**
         * Spring Cloud Bus is available, depends on {@code geoserver.bus.enabled}
         */
        @Test
        void conditionalOnPropertyGeoserverBusEnabled() {
            runner = runner.withPropertyValues("spring.cloud.bus.enabled: true");

            assertAclSpringCloudBusUnavailable();
            runner = runner.withPropertyValues("geoserver.bus.enabled: true");
            assertAclSpringCloudBusAvailable();
        }

        void assertAclSpringCloudBusUnavailable() {
            runner.run(context -> assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(AclResourceAccessManager.class)
                    .doesNotHaveBean(AclSpringCloudBusAutoConfiguration.class)
                    .doesNotHaveBean(RemoteAclRuleEventsBridge.class));
        }

        void assertAclSpringCloudBusAvailable() {
            runner.run(context -> assertThat(context)
                    .hasNotFailed()
                    // verify SpringCloudBus is available
                    .hasSingleBean(PathDestinationFactory.class)
                    // verify ACL access manager is available
                    .hasSingleBean(AclResourceAccessManager.class)
                    // verify the ACL spring cloud bus integration is available
                    .hasSingleBean(AclSpringCloudBusAutoConfiguration.class)
                    .hasSingleBean(RemoteAclRuleEventsBridge.class));
        }
    }
}
