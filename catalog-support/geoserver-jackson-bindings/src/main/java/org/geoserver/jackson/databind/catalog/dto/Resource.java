/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Coverage.class, name = "CoverageInfo"),
    @JsonSubTypes.Type(value = FeatureType.class, name = "FeatureTypeInfo"),
    @JsonSubTypes.Type(value = WMSLayer.class, name = "WMSLayerInfo"),
    @JsonSubTypes.Type(value = WMTSLayer.class, name = "WMTSLayerInfo")
})
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Resource extends CatalogInfoDto {
    public enum ProjectionPolicy {
        FORCE_DECLARED,
        REPROJECT_TO_DECLARED,
        NONE
    }

    private String name;
    private InfoReference namespace;
    private InfoReference store;
    private String nativeName;
    private List<String> alias;
    private String title;
    private String description;
    private String Abstract;
    private List<Keyword> keywords;
    private List<MetadataLink> metadataLinks;
    private List<DataLink> dataLinks;
    private CRS nativeCRS;
    private String SRS;
    private Envelope nativeBoundingBox;
    private Envelope latLonBoundingBox;
    private ProjectionPolicy projectionPolicy;
    private boolean enabled;
    private Boolean advertised;
    private Map<String, Serializable> metadata;
    private boolean serviceConfiguration;
    private List<String> disabledServices;
    private Boolean simpleConversionEnabled;

    /** @since geoserver 2.20.0 */
    private Map<String, String> internationalTitle;
    /** @since geoserver 2.20.0 */
    private Map<String, String> internationalAbstract;
}
