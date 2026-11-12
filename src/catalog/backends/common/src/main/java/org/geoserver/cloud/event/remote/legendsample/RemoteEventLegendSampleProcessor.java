/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.remote.legendsample;

import static org.geoserver.cloud.event.remote.resourcepool.RemoteEventResourcePoolProcessor.CLEANUP_AFTER_CHANGE_ORDER;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.impl.CatalogPostModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogRemoveEventImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.wms.capabilities.LegendSampleImpl;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

/**
 * Marks the legend sample of a style as invalid when another instance modifies or removes the style.
 *
 * <p>{@link LegendSampleImpl} only reacts to the events of the local catalog. This processor hands it the remote
 * changes of styles as the local events it already handles. The sample is drawn again by the next capabilities request.
 * Services without a legend sample, such as the configuration services, have nothing to invalidate.
 */
@Slf4j(topic = "org.geoserver.cloud.event.remote.legendsample")
public class RemoteEventLegendSampleProcessor {

    private final Catalog rawCatalog;
    private final ObjectProvider<LegendSampleImpl> legendSamples;

    public RemoteEventLegendSampleProcessor(Catalog rawCatalog, ObjectProvider<LegendSampleImpl> legendSamples) {
        this.rawCatalog = rawCatalog;
        this.legendSamples = legendSamples;
    }

    /** Runs after the backend dropped the style from its caches, to read the style as modified. */
    @EventListener(CatalogInfoModified.class)
    @Order(CLEANUP_AFTER_CHANGE_ORDER)
    public void onRemoteStyleModified(CatalogInfoModified event) {
        if (isRemoteStyleEvent(event)) {
            invalidateModifiedStyle(event);
        }
    }

    @EventListener(CatalogInfoRemoved.class)
    public void onRemoteStyleRemoved(CatalogInfoRemoved event) {
        if (isRemoteStyleEvent(event)) {
            invalidateRemovedStyle(event);
        }
    }

    private boolean isRemoteStyleEvent(InfoEvent event) {
        return event.isRemote() && event.getObjectType() == ConfigInfoType.STYLE;
    }

    private void invalidateModifiedStyle(CatalogInfoModified event) {
        LegendSampleImpl legendSample = legendSamples.getIfAvailable();
        if (legendSample == null) {
            return;
        }
        StyleInfo style = rawCatalog.getStyle(event.getObjectId());
        if (style == null) {
            log.debug("{} not found, nothing to invalidate", event.toShortString());
            return;
        }
        CatalogPostModifyEventImpl localEvent = new CatalogPostModifyEventImpl();
        localEvent.setSource(style);
        legendSample.handlePostModifyEvent(localEvent);
    }

    private void invalidateRemovedStyle(CatalogInfoRemoved event) {
        LegendSampleImpl legendSample = legendSamples.getIfAvailable();
        if (legendSample == null) {
            return;
        }
        CatalogRemoveEventImpl localEvent = new CatalogRemoveEventImpl();
        localEvent.setSource(removedStyle(event.getObjectName()));
        legendSample.handleRemoveEvent(localEvent);
    }

    /** The removed style is gone from the catalog; its name and workspace are all the legend sample reads. */
    private StyleInfo removedStyle(String prefixedName) {
        StyleInfoImpl style = new StyleInfoImpl(rawCatalog);
        style.setName(InfoEvent.localName(prefixedName));
        InfoEvent.prefix(prefixedName).ifPresent(prefix -> style.setWorkspace(workspaceNamed(prefix)));
        return style;
    }

    private WorkspaceInfo workspaceNamed(String name) {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setName(name);
        return workspace;
    }
}
