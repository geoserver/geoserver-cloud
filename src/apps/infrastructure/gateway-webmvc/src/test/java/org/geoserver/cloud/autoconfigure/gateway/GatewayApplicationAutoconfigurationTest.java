/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.geoserver.cloud.gateway.filter.GeoServerGatewayFilterFunctions;
import org.geoserver.cloud.gateway.predicate.GeoServerGatewayRequestPredicates;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.filter.CorsFilter;

/** @since 3.0.0 */
class GatewayApplicationAutoconfigurationTest {

    private WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GatewayApplicationAutoconfiguration.class));

    @Test
    void testDefaultAppContextContributions() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(GeoServerGatewayRequestPredicates.GeoServerGatewayPredicateSupplier.class)
                .hasSingleBean(GeoServerGatewayFilterFunctions.GeoServerGatewayFilterSupplier.class)
                .hasSingleBean(CorsFilter.class));
    }

    @Test
    void secureHeadersProperties_defaultValues() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(SecureHeadersProperties.class);
            SecureHeadersProperties props = context.getBean(SecureHeadersProperties.class);
            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getFrameOptions()).isEqualTo(SecureHeadersProperties.X_FRAME_OPTIONS_HEADER_DEFAULT);
            assertThat(props.getReferrerPolicy()).isEqualTo(SecureHeadersProperties.REFERRER_POLICY_HEADER_DEFAULT);
        });
    }

    @Test
    void secureHeadersProperties_customValues() {
        runner.withPropertyValues(
                        "spring.cloud.gateway.server.webmvc.filter.secure-headers.frame-options=SAMEORIGIN",
                        "spring.cloud.gateway.server.webmvc.filter.secure-headers.xss-protection-header=0",
                        "spring.cloud.gateway.server.webmvc.filter.secure-headers.disable=content-security-policy")
                .run(context -> {
                    SecureHeadersProperties props = context.getBean(SecureHeadersProperties.class);
                    assertThat(props.getFrameOptions()).isEqualTo("SAMEORIGIN");
                    assertThat(props.getXssProtectionHeader()).isEqualTo("0");
                    Map<String, String> headers = props.resolveHeaders();
                    assertThat(headers).doesNotContainKey(SecureHeadersProperties.CONTENT_SECURITY_POLICY_HEADER);
                    assertThat(headers).containsEntry(SecureHeadersProperties.X_FRAME_OPTIONS_HEADER, "SAMEORIGIN");
                });
    }

    @Test
    void gatewaySharedAuth_enabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            SharedAuthConfigurationProperties props = context.getBean(SharedAuthConfigurationProperties.class);
            assertThat(props.isEnabled()).isTrue();
        });
    }

    @Test
    void gatewaySharedAuth_disabledByConfig() {
        runner.withPropertyValues("geoserver.security.gateway-shared-auth.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SharedAuthConfigurationProperties props = context.getBean(SharedAuthConfigurationProperties.class);
                    assertThat(props.isEnabled()).isFalse();
                });
    }
}
