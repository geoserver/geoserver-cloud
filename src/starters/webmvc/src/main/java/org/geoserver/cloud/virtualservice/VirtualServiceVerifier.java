/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.virtualservice;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Helper service for OWS controllers to verify the existence of virtual services before proceeding.
 *
 * @since 1.0
 */
@RequiredArgsConstructor
public class VirtualServiceVerifier {

    private final @NonNull Catalog rawCatalog;

    /**
     * @throws 404 ResponseStatusException if {@code virtualService} can't be mapped to a workspace
     *     or a root layer group
     */
    public void checkVirtualService(@NonNull String virtualService) {
        findWorkspace(virtualService)
                .map(WorkspaceInfo::getName)
                .or(() -> findGlobalLayerGroup(virtualService).map(LayerGroupInfo::getName))
                .orElseThrow(() -> virtualServiceNotFound(virtualService));
    }

    /**
     * @throws 404 ResponseStatusException if {@code virtualService} can't be mapped to a workspace
     *     and layer to a {@link PublishedInfo} inside it
     */
    public void checkVirtualService(String virtualService, String layer) {
        WorkspaceInfo ws =
                findWorkspace(virtualService)
                        .orElseThrow(() -> virtualServiceNotFound(virtualService));
        findPublished(ws.getName(), layer).orElseThrow(() -> layerNotFound(virtualService, layer));
    }

    private Optional<WorkspaceInfo> findWorkspace(String workspace) {
        return Optional.ofNullable(rawCatalog.getWorkspaceByName(workspace));
    }

    private Optional<LayerGroupInfo> findGlobalLayerGroup(String name) {
        return Optional.ofNullable(rawCatalog.getLayerGroupByName(name));
    }

    private Optional<PublishedInfo> findPublished(String workspace, String layer) {
        PublishedInfo l = rawCatalog.getLayerGroupByName(workspace, layer);
        if (null == l) {
            String qualifiedName = workspace + ":" + layer;
            l = rawCatalog.getLayerByName(qualifiedName);
        }
        return Optional.ofNullable(l);
    }

    private ResponseStatusException virtualServiceNotFound(String service) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Workspace or global LayerGroup does not exist: " + service);
    }

    private ResponseStatusException layerNotFound(String workspace, String layer) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Layer " + layer + " does not exist in " + workspace);
    }
}
