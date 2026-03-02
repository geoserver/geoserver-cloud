/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * DTO for {@link org.geoserver.catalog.impl.LayerGroupStyle}
 *
 * @see org.geoserver.catalog.impl.LayerGroupStyle
 * @since 1.0-RC2 (geoserver 2.21.0)
 */
@Data
@JsonTypeName("LayerGroupStyle")
public class LayerGroupStyleDto {

    private String id;

    /** The style name as a StyleInfo. */
    private StyleInfoDto name;

    /** The list of contained PublishedInfo. */
    private List<String> layers;

    /** The List of StyleInfo for {@link #getLayers() the layers} */
    private List<String> styles;

    private String title;
    private Map<String, String> internationalTitle;

    @JsonProperty("abstract")
    @SuppressWarnings("java:S116")
    private String Abstract;

    private Map<String, String> internationalAbstract;
}
