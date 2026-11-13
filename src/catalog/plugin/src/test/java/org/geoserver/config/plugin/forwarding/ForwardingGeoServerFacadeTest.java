/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.plugin.forwarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.config.plugin.ConfigRepository;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;
import org.junit.jupiter.api.Test;

class ForwardingGeoServerFacadeTest {

    @Test
    void getSettingsByIdForwards() {
        RepositoryGeoServerFacade subject = mock(RepositoryGeoServerFacade.class);
        SettingsInfo settings = new SettingsInfoImpl();
        when(subject.getSettings("settings-id")).thenReturn(settings);
        ForwardingGeoServerFacade facade = new ForwardingGeoServerFacade(subject);

        SettingsInfo found = facade.getSettings("settings-id");

        assertThat(found).isSameAs(settings);
    }

    @Test
    void setRepositoryForwards() {
        RepositoryGeoServerFacade subject = mock(RepositoryGeoServerFacade.class);
        ConfigRepository repository = mock(ConfigRepository.class);
        ForwardingGeoServerFacade facade = new ForwardingGeoServerFacade(subject);

        facade.setRepository(repository);

        verify(subject).setRepository(repository);
    }

    // a GeoServerFacade has no lookup of settings by id
    @Test
    void getSettingsByIdRejectsOtherSubject() {
        GeoServerFacade subject = mock(GeoServerFacade.class);
        ForwardingGeoServerFacade facade = new ForwardingGeoServerFacade(subject);

        assertThatThrownBy(() -> facade.getSettings("settings-id")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void setRepositoryRejectsOtherSubject() {
        GeoServerFacade subject = mock(GeoServerFacade.class);
        ConfigRepository repository = mock(ConfigRepository.class);
        ForwardingGeoServerFacade facade = new ForwardingGeoServerFacade(subject);

        assertThatThrownBy(() -> facade.setRepository(repository)).isInstanceOf(IllegalStateException.class);
    }
}
