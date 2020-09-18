/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import java.util.NoSuchElementException;
import lombok.Getter;
import org.geoserver.catalog.WorkspaceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(WorkspaceController.BASE_URI)
public class WorkspaceController extends AbstractCatalogInfoController<WorkspaceInfo> {

    public static final String BASE_URI = BASE_API_URI + "/workspaces";

    private final @Getter Class<WorkspaceInfo> infoType = WorkspaceInfo.class;

    @GetMapping(path = "/default")
    public Mono<WorkspaceInfo> getDefault() {
        return Mono.just(catalog.getDefaultWorkspace())
                .switchIfEmpty(notFound("No default Workspace exists"));
    }

    @PutMapping(path = "/default/{id}")
    public Mono<WorkspaceInfo> setDefaultById(@PathVariable("id") String workspaceId) {
        WorkspaceInfo ws = catalog.getWorkspace(workspaceId);
        if (ws == null)
            throw new NoSuchElementException("Workspace " + workspaceId + " does not exist");
        catalog.setDefaultWorkspace(ws);
        return Mono.just(ws);
    }
}
