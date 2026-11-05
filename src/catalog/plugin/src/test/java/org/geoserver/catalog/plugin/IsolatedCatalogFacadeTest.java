/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IsolatedCatalogFacadeTest {

    private ExtendedCatalogFacade subject;
    private IsolatedCatalogFacade isolated;
    private LayerGroupInfo group;

    @BeforeEach
    void setUp() {
        WorkspaceInfoImpl iso = new WorkspaceInfoImpl();
        iso.setName("iso");
        iso.setIsolated(true);
        group = new LayerGroupInfoImpl();
        ((LayerGroupInfoImpl) group).setId("g-id");
        group.setName("g");
        group.setWorkspace(iso);

        subject = mock(ExtendedCatalogFacade.class);
        when(subject.getLayerGroup("g-id")).thenReturn(group);
        when(subject.getPublished("g-id")).thenReturn(group);
        isolated = new IsolatedCatalogFacade(subject);

        WorkspaceInfo other = new WorkspaceInfoImpl();
        other.setName("other");
        LocalWorkspace.set(other);
        Dispatcher.REQUEST.set(new Request());
    }

    @AfterEach
    void tearDown() {
        LocalWorkspace.remove();
        Dispatcher.REQUEST.remove();
    }

    @Test
    void getPublishedHidesIsolated() {
        assertThat(isolated.getPublished("g-id")).isNull();
        assertThat(isolated.getLayerGroup("g-id")).isNull();
    }

    @Test
    void publishedByIdHidesIsolated() {
        LayerInfoImpl visible = new LayerInfoImpl();
        visible.setId("l-id");
        List<String> ids = List.of("g-id", "l-id");
        when(subject.findAllById(ids, PublishedInfo.class)).thenReturn(Stream.of(group, visible));

        try (Stream<PublishedInfo> found = isolated.findAllById(ids, PublishedInfo.class)) {
            assertThat(found).containsExactly(visible);
        }
    }

    @Test
    void stylesByIdHidesIsolated() {
        StyleInfoImpl hidden = new StyleInfoImpl(null);
        hidden.setId("s-iso");
        hidden.setWorkspace(group.getWorkspace());
        StyleInfoImpl visible = new StyleInfoImpl(null);
        visible.setId("s-global");
        List<String> ids = List.of("s-iso", "s-global");
        when(subject.findAllById(ids, StyleInfo.class)).thenReturn(Stream.of(hidden, visible));

        try (Stream<StyleInfo> found = isolated.findAllById(ids, StyleInfo.class)) {
            assertThat(found).containsExactly(visible);
        }
    }

    @Test
    void getPublishedSeesIsolatedInItsWorkspace() {
        LocalWorkspace.set(group.getWorkspace());

        assertThat(isolated.getPublished("g-id")).isSameAs(group);
    }
}
