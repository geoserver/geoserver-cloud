/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.autoconfigure.accessmanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.acl.plugin.accessmanager.ACLDispatcherCallback;
import org.geoserver.acl.plugin.accessmanager.ACLResourceAccessManager;
import org.geoserver.acl.plugin.accessmanager.wps.WPSHelper;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** {@link AclAccessManagerAutoConfiguration} tests */
class AclAccessManagerAutoConfigurationTest {

    private ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean("rawCatalog", Catalog.class, CatalogImpl::new)
            .withBean(
                    "layerGroupContainmentCache",
                    LayerGroupContainmentCache.class,
                    () -> mock(LayerGroupContainmentCache.class))
            .withConfiguration(AutoConfigurations.of(AclAccessManagerAutoConfiguration.class));

    @Test
    void testEnabledByDefaultWhenServiceUrlIsProvided() {
        runner.withPropertyValues(
                        "geoserver.acl.client.startupCheck=false", "geoserver.acl.client.basePath=http://acl.test:9000")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(ACLResourceAccessManager.class)
                            .hasSingleBean(ACLDispatcherCallback.class)
                            .hasSingleBean(WPSHelper.class);
                });
    }

    @Test
    void testConditionalOnAclEnabled() {
        runner.withPropertyValues("geoserver.acl.enabled=false", "geoserver.acl.client.basePath=http://acl.test:9000")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(ACLResourceAccessManager.class)
                            .doesNotHaveBean(ACLDispatcherCallback.class)
                            .doesNotHaveBean(WPSHelper.class);
                });

        runner.withPropertyValues(
                        "geoserver.acl.enabled=true",
                        "geoserver.acl.client.startupCheck=false",
                        "geoserver.acl.client.basePath=http://acl.test:9000")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(ACLResourceAccessManager.class)
                            .hasSingleBean(ACLDispatcherCallback.class)
                            .hasSingleBean(WPSHelper.class);
                });
    }

    @Test
    void testFailsIfEnabledAndServiceUrlNotProvided() {
        runner.withPropertyValues("geoserver.acl.client.basePath=").run(context -> {
            assertThat(context).hasFailed().getFailure().hasMessageContaining("geoserver.acl.client.basePath");
        });
    }
}
