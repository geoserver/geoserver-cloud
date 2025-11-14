/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.autoconfigure.webui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.security.web.SecuritySettingsPage;
import org.geoserver.web.Category;
import org.geoserver.web.GeoServerBasePage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** {@link AclWebUIAutoConfiguration} tests */
class AclWebUIAutoConfigurationTest {

    private ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean("securityCategory", Category.class, () -> mock(Category.class))
            .withConfiguration(AutoConfigurations.of(AclWebUIAutoConfiguration.class));

    @Test
    void testWebUiDisabledByDefault() {
        runner.run(this::assertDisabled);
    }

    @Test
    void testConditionalOnProperty() {
        runner.withPropertyValues("geoserver.acl.enabled=true", "geoserver.web-ui.acl.enabled=true")
                .run(context -> assertEnabled(context));
    }

    @Test
    void testConditionalOnAclEnabled() {
        runner.withPropertyValues("geoserver.acl.enabled=false", "geoserver.web-ui.acl.enabled=true")
                .run(this::assertDisabled);
    }

    @Test
    void testWebUiEnabledWithAclEnabledByDefault() {
        runner.withPropertyValues("geoserver.web-ui.acl.enabled=true").run(this::assertEnabled);
    }

    @Test
    void testConditionalOnGeoServerBasePage() {
        runner.withClassLoader(new FilteredClassLoader(GeoServerBasePage.class)).run(this::assertDisabled);
    }

    @Test
    void testConditionalOnSecuritySettingsPage() {
        runner.withClassLoader(new FilteredClassLoader(SecuritySettingsPage.class))
                .run(this::assertDisabled);
    }

    private void assertEnabled(AssertableApplicationContext context) {
        assertThat(context)
                .hasNotFailed()
                .hasBean("aclServiceConfigPageMenuInfo")
                .hasBean("accessRulesACLPageMenuInfo")
                .hasBean("adminRulesAclPageMenuInfo");
    }

    private void assertDisabled(AssertableApplicationContext context) {
        assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean("aclServiceConfigPageMenuInfo")
                .doesNotHaveBean("accessRulesACLPageMenuInfo")
                .doesNotHaveBean("adminRulesAclPageMenuInfo");
    }
}
