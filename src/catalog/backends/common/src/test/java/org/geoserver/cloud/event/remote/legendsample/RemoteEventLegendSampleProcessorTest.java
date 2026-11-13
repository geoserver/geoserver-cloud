/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.remote.legendsample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.event.impl.CatalogPostModifyEventImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.wms.capabilities.LegendSampleImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

class RemoteEventLegendSampleProcessorTest {

    private Catalog rawCatalog;
    private LegendSampleImpl legendSample;
    private ObjectProvider<LegendSampleImpl> legendSamples;
    private RemoteEventLegendSampleProcessor processor;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        rawCatalog = mock(Catalog.class);
        legendSample = mock(LegendSampleImpl.class);
        legendSamples = mock(ObjectProvider.class);
        when(legendSamples.getIfAvailable()).thenReturn(legendSample);
        processor = new RemoteEventLegendSampleProcessor(rawCatalog, legendSamples);
    }

    @Test
    void remoteModifyInvalidatesSample() {
        StyleInfoImpl style = style("style-id", "roads", null);
        when(rawCatalog.getStyle("style-id")).thenReturn(style);

        processor.onRemoteStyleModified(modified(style, true));

        ArgumentCaptor<CatalogPostModifyEvent> captor = ArgumentCaptor.forClass(CatalogPostModifyEvent.class);
        verify(legendSample).handlePostModifyEvent(captor.capture());
        assertThat(captor.getValue().getSource()).isSameAs(style);
    }

    @Test
    void localModifyIgnored() {
        StyleInfoImpl style = style("style-id", "roads", null);
        when(rawCatalog.getStyle("style-id")).thenReturn(style);

        processor.onRemoteStyleModified(modified(style, false));

        verifyNoInteractions(legendSample);
    }

    @Test
    void remoteRemovalOfGlobalStyle() {
        processor.onRemoteStyleRemoved(removed(style("style-id", "roads", null), true));

        StyleInfo removed = capturedRemovedStyle();
        assertThat(removed.getName()).isEqualTo("roads");
        assertThat(removed.getWorkspace()).isNull();
    }

    @Test
    void remoteRemovalOfWorkspaceStyle() {
        WorkspaceInfoImpl ws1 = new WorkspaceInfoImpl();
        ws1.setId("ws1-id");
        ws1.setName("ws1");

        processor.onRemoteStyleRemoved(removed(style("style-id", "roads", ws1), true));

        StyleInfo removed = capturedRemovedStyle();
        assertThat(removed.getName()).isEqualTo("roads");
        assertThat(removed.getWorkspace().getName()).isEqualTo("ws1");
    }

    @Test
    void localRemovalIgnored() {
        processor.onRemoteStyleRemoved(removed(style("style-id", "roads", null), false));

        verifyNoInteractions(legendSample);
    }

    @Test
    void otherTypesIgnored() {
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        ws.setId("ws-id");
        ws.setName("ws");

        processor.onRemoteStyleRemoved(removed(ws, true));
        processor.onRemoteStyleModified(modified(ws, true));

        verifyNoInteractions(legendSample);
    }

    @Test
    void noLegendSampleNothing() {
        when(legendSamples.getIfAvailable()).thenReturn(null);
        StyleInfoImpl style = style("style-id", "roads", null);
        when(rawCatalog.getStyle("style-id")).thenReturn(style);

        processor.onRemoteStyleModified(modified(style, true));
        processor.onRemoteStyleRemoved(removed(style, true));

        verifyNoInteractions(legendSample);
    }

    @Test
    void modifiedStyleGoneIgnored() {
        StyleInfoImpl style = style("style-id", "roads", null);
        when(rawCatalog.getStyle("style-id")).thenReturn(null);

        processor.onRemoteStyleModified(modified(style, true));

        verifyNoInteractions(legendSample);
    }

    private StyleInfo capturedRemovedStyle() {
        ArgumentCaptor<CatalogRemoveEvent> captor = ArgumentCaptor.forClass(CatalogRemoveEvent.class);
        verify(legendSample).handleRemoveEvent(captor.capture());
        return (StyleInfo) captor.getValue().getSource();
    }

    private StyleInfoImpl style(String id, String name, WorkspaceInfo workspace) {
        StyleInfoImpl style = new StyleInfoImpl(rawCatalog);
        style.setId(id);
        style.setName(name);
        style.setWorkspace(workspace);
        return style;
    }

    private CatalogInfoModified modified(CatalogInfo info, boolean remote) {
        CatalogPostModifyEventImpl postModify = new CatalogPostModifyEventImpl();
        postModify.setSource(info);
        postModify.setPropertyNames(List.of());
        postModify.setOldValues(List.of());
        postModify.setNewValues(List.of());
        CatalogInfoModified event = CatalogInfoModified.createLocal(1, postModify);
        event.setRemote(remote);
        return event;
    }

    private CatalogInfoRemoved removed(CatalogInfo info, boolean remote) {
        CatalogInfoRemoved event = CatalogInfoRemoved.createLocal(1, info);
        event.setRemote(remote);
        return event;
    }
}
