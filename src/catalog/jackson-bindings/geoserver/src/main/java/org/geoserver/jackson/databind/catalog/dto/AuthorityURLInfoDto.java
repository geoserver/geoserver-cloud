/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

/**
 * DTO for {@link org.geoserver.catalog.AuthorityURLInfo}
 */
@Data
@JsonTypeName("AuthorityURLInfo")
public class AuthorityURLInfoDto {
    private String name;
    private String href;
}
