/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;
import org.geoserver.config.ServiceInfo;
import org.geoserver.jackson.databind.catalog.dto.InfoReference;
import org.geoserver.jackson.databind.catalog.dto.Keyword;
import org.geoserver.jackson.databind.catalog.dto.MetadataLink;

/** DTO for {@link ServiceInfo} */
public @Data class Service {
    private String id;
    private String name;
    private InfoReference workspace;
    private InfoReference geoServer;
    private boolean citeCompliant;
    private boolean enabled;
    private String onlineResource;
    private String title;
    private String Abstract;
    private String maintainer;
    private String fees;
    private String accessConstraints;
    private List<String> versions;
    private List<Keyword> keywords;
    private List<String> exceptionFormats;
    private MetadataLink metadataLink;
    private String outputStrategy;
    private String schemaBaseURL;
    private boolean verbose;
    private Map<String, Object> metadata;
    // not used
    // Map<Object, Object> clientProperties;
}
