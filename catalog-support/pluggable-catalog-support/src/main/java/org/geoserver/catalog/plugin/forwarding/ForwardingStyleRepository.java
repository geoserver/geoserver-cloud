/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.List;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;

public class ForwardingStyleRepository
        extends ForwardingCatalogRepository<StyleInfo, StyleRepository> implements StyleRepository {

    public ForwardingStyleRepository(StyleRepository subject) {
        super(subject);
    }

    public @Override List<StyleInfo> findAllByNullWorkspace() {
        return subject.findAllByNullWorkspace();
    }

    public @Override List<StyleInfo> findAllByWorkspace(WorkspaceInfo ws) {
        return subject.findAllByWorkspace(ws);
    }

    public @Override StyleInfo findByNameAndWordkspaceNull(String name) {
        return subject.findByNameAndWordkspaceNull(name);
    }

    public @Override StyleInfo findByNameAndWordkspace(String name, WorkspaceInfo workspace) {
        return subject.findByNameAndWordkspace(name, workspace);
    }
}
