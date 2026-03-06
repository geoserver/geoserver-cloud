/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.geotools.jackson.databind.dto.CoordinateReferenceSystemDto;
import org.geotools.jackson.databind.dto.ReferencedEnvelopeDto;

/** Base DTO for {@link org.geoserver.catalog.ResourceInfo} */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CoverageInfoDto.class),
    @JsonSubTypes.Type(value = FeatureTypeInfoDto.class),
    @JsonSubTypes.Type(value = WMSLayerInfoDto.class),
    @JsonSubTypes.Type(value = WMTSLayerInfoDto.class)
})
public abstract class ResourceInfoDto extends CatalogInfoDto {
    public enum ProjectionPolicy {
        FORCE_DECLARED,
        REPROJECT_TO_DECLARED,
        NONE
    }

    private String name;
    private String namespace;
    private String store;
    private String nativeName;
    private List<String> alias;
    private String title;
    private String description;

    @JsonProperty("abstract")
    @SuppressWarnings("java:S116")
    private String Abstract;

    private List<KeywordInfoDto> keywords;
    private List<MetadataLinkInfoDto> metadataLinks;
    private List<DataLinkInfoDto> dataLinks;
    private CoordinateReferenceSystemDto nativeCRS;

    @JsonProperty("srs")
    @SuppressWarnings("java:S116")
    private String SRS;

    private ReferencedEnvelopeDto nativeBoundingBox;
    private ReferencedEnvelopeDto latLonBoundingBox;
    private ProjectionPolicy projectionPolicy;
    private boolean enabled;
    private Boolean advertised;
    private MetadataMapDto metadata;
    private boolean serviceConfiguration;
    private List<String> disabledServices;
    private Boolean simpleConversionEnabled;

    /** @since geoserver 2.20.0 */
    private Map<String, String> internationalTitle;

    /** @since geoserver 2.20.0 */
    private Map<String, String> internationalAbstract;
}
