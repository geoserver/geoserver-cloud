/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

/** DTO for {@link org.geoserver.catalog.MetadataLinkInfo} */
@Data
@JsonTypeName("MetadataLinkInfo")
public class MetadataLinkInfoDto {
    private String id;
    private String type;
    private String about;
    private String metadataType;
    private String content;
}
