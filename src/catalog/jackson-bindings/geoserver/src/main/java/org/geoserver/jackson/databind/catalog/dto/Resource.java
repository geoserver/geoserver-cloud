/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.geotools.jackson.databind.dto.CRS;
import org.geotools.jackson.databind.dto.Envelope;

import java.util.List;
import java.util.Map;

@JsonSubTypes({
    @JsonSubTypes.Type(value = Coverage.class),
    @JsonSubTypes.Type(value = FeatureType.class),
    @JsonSubTypes.Type(value = WMSLayer.class),
    @JsonSubTypes.Type(value = WMTSLayer.class)
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
    private String namespace;
    private String store;
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
    private MetadataMapDto metadata;
    private boolean serviceConfiguration;
    private List<String> disabledServices;
    private Boolean simpleConversionEnabled;

    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalTitle;

    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalAbstract;
}
