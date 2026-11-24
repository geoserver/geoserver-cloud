/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.geoserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Verifies that the combined application's {@code application.yml} ships no convenience knob mapping for the pgconfig
 * backend or the ACL extension: deployment knobs for these live in externalized configuration (a config server or a
 * mounted {@code /etc/geoserver/geoserver.yml}) or are set directly through the components' own configuration
 * properties, never through a profile document baked into the jar.
 *
 * <p>Such a jar-baked document previously existed for the {@code pgconfig}, {@code acl}, and {@code datadir} profiles,
 * each with its own localhost/default-credential values. A config-server-backed deployment (the config-first channel)
 * places its remote property source below profile-specific documents in the config data processing order; those
 * in-image defaults therefore outranked whatever the externalized config defined for the same properties. Guarding the
 * documents' activation was one fix; removing them outright is a stronger one - with no in-image document, there is
 * nothing left to outrank a remote source in any channel.
 *
 * <p>The data directory backend is the one exception: it is the zero-config monolith's self-contained default, and its
 * {@code enabled: true} override stays in the unconditional (non-profile) document.
 *
 * <p>Only the {@link Environment} is inspected here, not a full application context: the properties under test are
 * resolved while Spring Boot processes configuration data, before any GeoServer bean exists.
 */
class InImageProfileDocumentsPrecedenceTest {

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfig {}

    private Environment environmentFor(String... profiles) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EmptyConfig.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .profiles(profiles)
                .run("--spring.cloud.config.enabled=false")) {
            return context.getEnvironment();
        }
    }

    @Test
    void pgconfigProfileDefinesNoInImageOverrides() {
        Environment env = environmentFor("pgconfig");
        assertThat(env.getProperty("pgconfig.host")).isNull();
        assertThat(env.getProperty("geoserver.backend.pgconfig.enabled")).isNull();
    }

    @Test
    void pgconfigWithConfigFirstDefinesNoInImageOverrides() {
        Environment env = environmentFor("pgconfig", "config-first");
        assertThat(env.getProperty("pgconfig.host")).isNull();
        assertThat(env.getProperty("geoserver.backend.pgconfig.enabled")).isNull();
    }

    @Test
    void aclProfileDefinesNoInImageOverrides() {
        Environment env = environmentFor("acl");
        assertThat(env.getProperty("acl.enabled")).isNull();
        assertThat(env.getProperty("acl.url")).isNull();
        assertThat(env.getProperty("geoserver.extension.security.acl.enabled")).isNull();
    }

    @Test
    void dataDirectoryBackendStaysDefaultOn() {
        Environment env = environmentFor();
        assertThat(env.getProperty("geoserver.backend.data-directory.enabled", Boolean.class))
                .isTrue();
    }

    // a deployment asking for a configuration file must not start on the in-image defaults when the file is missing
    @Test
    void missingStandaloneConfigFails() {
        assertThatThrownBy(() -> standaloneEnvironment("file:/nonexistent/geoserver.yml"))
                .isInstanceOf(ConfigDataResourceNotFoundException.class);
    }

    private Environment standaloneEnvironment(String configLocation) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EmptyConfig.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .profiles("bootstrap-standalone")
                .run("--spring.cloud.config.enabled=false", "--standalone.config.location=" + configLocation)) {
            return context.getEnvironment();
        }
    }
}
