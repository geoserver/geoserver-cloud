/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto.mapper;

import org.geotools.util.Version;
import org.mapstruct.Mapper;

@Mapper(config = SpringConfigInfoMapperConfig.class)
public interface ValueMappers {

    default Version map(String v) {
        return v == null ? null : new Version(v);
    }

    default String map(Version v) {
        return v == null ? null : v.toString();
    }
}
