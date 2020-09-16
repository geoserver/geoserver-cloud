/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import lombok.Getter;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cloud.catalog.dto.Workspace;
import org.geoserver.cloud.catalog.modelmapper.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(WorkspaceController.BASE_URI)
public class WorkspaceController extends AbstractCatalogInfoController<WorkspaceInfo, Workspace> {

    public static final String BASE_URI = BASE_API_URI + "/workspaces";

    private final @Getter Class<WorkspaceInfo> infoType = WorkspaceInfo.class;

    private @Autowired WorkspaceMapper mapper;

    protected @Override WorkspaceInfo toInfo(Workspace dto) {
        return mapper.map(dto);
    }

    protected @Override Workspace toDto(WorkspaceInfo info) {
        return mapper.map(info);
    }

    @Nullable
    @GetMapping(path = "/default")
    public Mono<Workspace> getDefault() {
        WorkspaceInfoImpl fake = new WorkspaceInfoImpl();
        fake.setId("fake-ws-id");
        fake.setName("fakse-ws");
        return Mono.just(fake).map(this::toDto);
    }
}
