/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.geoserver.catalog.WMSLayerInfo;

/**
 * DTO for {@link WMSLayerInfo}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("WMSLayerInfo")
public class WMSLayerInfoDto extends ResourceInfoDto {
    private String forcedRemoteStyle = "";
    private String preferredFormat;
    private Double minScale;
    private Double maxScale;
    private boolean metadataBBoxRespected;
    private List<String> selectedRemoteFormats;
    private List<String> selectedRemoteStyles;
    /**
     * @since 2.28
     */
    private Map<String, String> vendorParameters;
}
