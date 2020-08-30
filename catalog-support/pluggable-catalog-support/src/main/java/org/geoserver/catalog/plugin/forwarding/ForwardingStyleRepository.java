/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.List;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;

public class ForwardingStyleRepository extends ForwardingCatalogRepository<StyleInfo>
        implements StyleRepository {

    public ForwardingStyleRepository(CatalogInfoRepository<StyleInfo> subject) {
        super(subject);
    }

    public @Override StyleInfo findOneByName(String name) {
        return ((StyleRepository) subject).findOneByName(name);
    }

    public @Override List<StyleInfo> findAllByNullWorkspace() {
        return ((StyleRepository) subject).findAllByNullWorkspace();
    }

    public @Override List<StyleInfo> findAllByWorkspace(WorkspaceInfo ws) {
        return ((StyleRepository) subject).findAllByWorkspace(ws);
    }
}
