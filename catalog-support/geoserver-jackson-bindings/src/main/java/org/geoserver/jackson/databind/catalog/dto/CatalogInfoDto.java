/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import java.util.Date;
import lombok.Data;

@Data
public abstract class CatalogInfoDto {
    private String id;
    private Date dateCreated;
    private Date dateModified;
}
