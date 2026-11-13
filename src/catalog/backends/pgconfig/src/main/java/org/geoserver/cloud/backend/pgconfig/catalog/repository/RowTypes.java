/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import static org.geoserver.cloud.event.info.ConfigInfoType.COVERAGE;
import static org.geoserver.cloud.event.info.ConfigInfoType.COVERAGESTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.DATASTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.FEATURETYPE;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYERGROUP;
import static org.geoserver.cloud.event.info.ConfigInfoType.NAMESPACE;
import static org.geoserver.cloud.event.info.ConfigInfoType.STYLE;
import static org.geoserver.cloud.event.info.ConfigInfoType.WMSLAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.WMSSTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.WMTSLAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.WMTSSTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.WORKSPACE;

import java.util.List;
import org.geoserver.cloud.event.info.ConfigInfoType;

/**
 * The concrete types of the objects of a repository, in lookup order: the most common type of a family first. The cache
 * keys of an object and the type filter of a query both take them.
 */
final class RowTypes {

    /** The types of the objects held by catalog rows, in lookup order. */
    private static final List<ConfigInfoType> ALL = List.of(
            WORKSPACE,
            NAMESPACE,
            DATASTORE,
            COVERAGESTORE,
            WMSSTORE,
            WMTSSTORE,
            FEATURETYPE,
            COVERAGE,
            WMSLAYER,
            WMTSLAYER,
            LAYER,
            LAYERGROUP,
            STYLE);

    private final Class<?> contentType;

    private final List<ConfigInfoType> types;

    RowTypes(Class<?> contentType) {
        this.contentType = contentType;
        this.types = ALL.stream()
                .filter(type -> contentType.isAssignableFrom(type.getType()))
                .toList();
    }

    /** The types of this repository assignable to the class, in lookup order. */
    List<ConfigInfoType> assignableTo(Class<?> clazz) {
        if (clazz.isAssignableFrom(contentType)) {
            return types;
        }
        return types.stream()
                .filter(type -> clazz.isAssignableFrom(type.getType()))
                .toList();
    }
}
