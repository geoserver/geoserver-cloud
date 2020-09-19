/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import org.geoserver.catalog.WorkspaceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import reactor.core.publisher.Mono;

public interface WorkspaceClient {
    @PutMapping(path = "/workspaces/default/{workspaceId}")
    void setDefaultWorkspace(@PathVariable("workspaceId") String workspaceId);

    @GetMapping(path = "/workspaces/default")
    Mono<WorkspaceInfo> getDefaultWorkspace();
}
