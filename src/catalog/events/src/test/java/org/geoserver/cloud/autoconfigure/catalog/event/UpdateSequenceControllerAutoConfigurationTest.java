/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.geoserver.cloud.autoconfigure.catalog.event.UpdateSequenceController.UpdateSeq;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.config.UpdateSequence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Tests for {@link UpdateSequenceControllerAutoConfiguration}. */
class UpdateSequenceControllerAutoConfigurationTest {

    private final UpdateSequence updateSequence = mock(UpdateSequence.class);

    private final GeoServer geoServer = mock(GeoServer.class, RETURNS_DEEP_STUBS);

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withBean(UpdateSequence.class, () -> updateSequence)
            .withBean(GeoServer.class, () -> geoServer)
            .withConfiguration(AutoConfigurations.of(UpdateSequenceControllerAutoConfiguration.class));

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // The roles granted to the admin login of the environment admin extension
    @Test
    void administratorIncrements() {
        authenticate("ADMIN", "ROLE_ADMINISTRATOR", "ROLE_AUTHENTICATED");
        when(geoServer.getGlobal().getUpdateSequence()).thenReturn(22L);
        when(updateSequence.nextValue()).thenReturn(23L);

        runner.run(context -> {
            UpdateSequenceController controller = context.getBean(UpdateSequenceController.class);
            UpdateSeq result = controller.nextVal();
            assertThat(result).isEqualTo(new UpdateSeq(22L, 23L));
        });
    }

    // GeoServer grants ROLE_ADMINISTRATOR to the holders of the admin role of the role service, whatever its name.
    // Without it, roles named ADMIN or ROLE_ADMIN are ordinary roles.
    @Test
    void otherRolesAreDenied() {
        authenticate("ADMIN", "ROLE_ADMIN", "ROLE_AUTHENTICATED");

        runner.run(context -> {
            UpdateSequenceController controller = context.getBean(UpdateSequenceController.class);
            assertThatThrownBy(controller::nextVal).isInstanceOf(AccessDeniedException.class);
        });
        verify(updateSequence, never()).nextValue();
    }

    private void authenticate(String... authorities) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "secret", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
