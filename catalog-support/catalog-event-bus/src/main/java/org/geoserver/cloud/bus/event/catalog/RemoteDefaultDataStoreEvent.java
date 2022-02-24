/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.catalog;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.Patch;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@EqualsAndHashCode(callSuper = true)
public class RemoteDefaultDataStoreEvent extends AbstractRemoteCatalogModifyEvent {
    private static final long serialVersionUID = 1L;

    private @NonNull @Getter @Setter String workspaceId;
    private @Getter @Setter String defaultDataStoreId;

    /** default constructor, needed for deserialization */
    protected RemoteDefaultDataStoreEvent() {
        //
    }

    public RemoteDefaultDataStoreEvent(
            @NonNull Catalog source,
            @NonNull WorkspaceInfo workspace,
            Patch patch,
            @NonNull String originService,
            String destinationService) {

        super(source, source, patch, originService, destinationService);
        this.workspaceId = workspace.getId();
        DataStoreInfo newValue = (DataStoreInfo) patch.get("defaultDataStore").get().getValue();
        this.defaultDataStoreId = RemoteCatalogEvent.resolveId(newValue);
    }
}
