/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Unit tests for {@link NormalizedBasePathPropertySource}: the {@code basepath} property resolves to the canonical form
 * no matter how {@code geoserver.base-path} is spelled ("/", trailing slashes, missing leading slash).
 *
 * @since 3.1.0
 */
class NormalizedBasePathPropertySourceTest {

    private StandardEnvironment environment = new StandardEnvironment();

    @Test
    void rootBecomesEmpty() {
        assertThat(resolvedBasePath("/")).isEmpty();
    }

    @Test
    void emptyStaysEmpty() {
        assertThat(resolvedBasePath("")).isEmpty();
    }

    @Test
    void whitespaceRootBecomesEmpty() {
        assertThat(resolvedBasePath(" / ")).isEmpty();
    }

    @Test
    void trailingSlashStripped() {
        assertThat(resolvedBasePath("/geoserver/")).isEqualTo("/geoserver");
    }

    @Test
    void repeatedTrailingSlashesStripped() {
        assertThat(resolvedBasePath("/geoserver/cloud//")).isEqualTo("/geoserver/cloud");
    }

    @Test
    void missingLeadingSlashAdded() {
        assertThat(resolvedBasePath("geoserver")).isEqualTo("/geoserver");
    }

    @Test
    void canonicalValueUnchanged() {
        assertThat(resolvedBasePath("/geoserver/cloud")).isEqualTo("/geoserver/cloud");
    }

    @Test
    void undefinedBasePathStaysUndefined() {
        NormalizedBasePathPropertySource.register(environment);
        assertThat(environment.getProperty("basepath")).isNull();
    }

    @Test
    void directValueWithoutAliasChainNormalized() {
        addProperties(Map.of("basepath", "/foo/"));
        NormalizedBasePathPropertySource.register(environment);
        assertThat(environment.getProperty("basepath")).isEqualTo("/foo");
    }

    @Test
    void unresolvablePlaceholderKeepsDefaultStrictBehavior() {
        addProperties(Map.of("basepath", "${geoserver.base-path}"));
        NormalizedBasePathPropertySource.register(environment);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> environment.getProperty("basepath"))
                .withMessageContaining("geoserver.base-path");
    }

    @Test
    void registerTwice_singleSourceStillResolves() {
        addProperties(Map.of("basepath", "/foo/"));
        NormalizedBasePathPropertySource.register(environment);
        NormalizedBasePathPropertySource.register(environment);
        assertThat(environment.getProperty("basepath")).isEqualTo("/foo");
    }

    @Test
    void otherPropertiesUnaffected() {
        addProperties(Map.of("basepath", "/foo/", "other.property", "value/"));
        NormalizedBasePathPropertySource.register(environment);
        assertThat(environment.getProperty("other.property")).isEqualTo("value/");
    }

    /** Resolves {@code basepath} through the alias chain used in the shipped gateway config. */
    private String resolvedBasePath(String rawBasePath) {
        addProperties(Map.of("geoserver.base-path", rawBasePath, "basepath", "${geoserver.base-path}"));
        NormalizedBasePathPropertySource.register(environment);
        return environment.getProperty("basepath");
    }

    private void addProperties(Map<String, Object> properties) {
        environment.getPropertySources().addLast(new MapPropertySource("testProperties", properties));
    }
}
