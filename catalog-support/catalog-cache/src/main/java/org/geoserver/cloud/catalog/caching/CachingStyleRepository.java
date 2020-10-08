/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import java.util.Optional;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.forwarding.ForwardingStyleRepository;
import org.springframework.cache.annotation.CacheConfig;

@CacheConfig(cacheNames = CacheNames.STYLE_CACHE)
public class CachingStyleRepository extends ForwardingStyleRepository {

    public CachingStyleRepository(StyleRepository subject) {
        super(subject);
    }

    public @Override Optional<StyleInfo> findByNameAndWordkspaceNull(String name) {
        return super.findByNameAndWordkspaceNull(name);
    }

    public @Override Optional<StyleInfo> findByNameAndWordkspace(
            String name, WorkspaceInfo workspace) {
        return super.findByNameAndWordkspace(name, workspace);
    }
}
