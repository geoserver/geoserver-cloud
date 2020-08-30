/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.List;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;

public class ForwardingNamespaceRepository extends ForwardingCatalogRepository<NamespaceInfo>
        implements NamespaceRepository {

    public ForwardingNamespaceRepository(NamespaceRepository subject) {
        super(subject);
    }

    public @Override void setDefaultNamespace(NamespaceInfo namespace) {
        ((NamespaceRepository) subject).setDefaultNamespace(namespace);
    }

    public @Override NamespaceInfo getDefaultNamespace() {
        return ((NamespaceRepository) subject).getDefaultNamespace();
    }

    public @Override NamespaceInfo findOneByURI(String uri) {
        return ((NamespaceRepository) subject).findOneByURI(uri);
    }

    public @Override List<NamespaceInfo> findAllByURI(String uri) {
        return ((NamespaceRepository) subject).findAllByURI(uri);
    }
}
