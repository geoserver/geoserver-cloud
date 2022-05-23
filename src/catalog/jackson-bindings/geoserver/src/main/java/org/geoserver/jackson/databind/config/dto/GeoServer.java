/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.geoserver.config.GeoServerInfo;

import java.io.Serializable;
import java.util.Map;

/** DTO for {@link GeoServerInfo} */
@EqualsAndHashCode(callSuper = true)
public @Data class GeoServer extends ConfigInfoDto {
    public static enum ResourceErrorHandling {
        OGC_EXCEPTION_REPORT,
        SKIP_MISCONFIGURED_LAYERS
    }

    public static enum WebUIMode {
        DEFAULT,
        REDIRECT,
        DO_NOT_REDIRECT
    };

    private Settings settings;
    private JaiDto JAI;
    private CoverageAccess coverageAccess;
    private Map<String, Serializable> metadata;
    // not used
    // private Map<Object, Object> clientProperties;
    private long updateSequence;
    private String adminUsername;
    private String adminPassword;
    private int featureTypeCacheSize;
    private Boolean globalServices;
    private Boolean useHeadersProxyURL;
    private Integer xmlPostRequestLogBufferSize;
    private Boolean xmlExternalEntitiesEnabled;
    private String lockProviderName;
    private WebUIMode webUIMode;
    private Boolean allowStoredQueriesPerWorkspace;
    private ResourceErrorHandling resourceErrorHandling;
}
