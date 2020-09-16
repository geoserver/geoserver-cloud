/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto;

import java.util.Map;
import lombok.Data;
import org.geoserver.config.GeoServerInfo;

/** DTO for {@link GeoServerInfo} */
public @Data class GeoServer {
    public static enum ResourceErrorHandling {
        OGC_EXCEPTION_REPORT,
        SKIP_MISCONFIGURED_LAYERS
    }

    public static enum WebUIMode {
        DEFAULT,
        REDIRECT,
        DO_NOT_REDIRECT
    };

    private String id;
    private Settings settings;
    private JAI jai;
    private CoverageAccess coverageAccess;
    private Map<String, Object> metadata;
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
