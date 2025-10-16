/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.jackson.databind.catalog.dto.MetadataMapDto;

/** DTO for {@link GeoServerInfo} */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("GeoServerInfo")
// for backwards compatiblility, xmlExternalEntitiesEnabled removed in 2.28.0, might be present in a config JSON
// document from an earler version
@JsonIgnoreProperties("xmlExternalEntitiesEnabled")
public class GeoServer extends ConfigInfoDto {
    public enum ResourceErrorHandling {
        OGC_EXCEPTION_REPORT,
        SKIP_MISCONFIGURED_LAYERS
    }

    public enum WebUIMode {
        DEFAULT,
        REDIRECT,
        DO_NOT_REDIRECT
    }

    private Settings settings;

    @JsonAlias({"jai", "JAI"})
    private ImageProcessingInfoDto imageProcessing;

    private CoverageAccess coverageAccess;
    private MetadataMapDto metadata;
    private long updateSequence;
    private String adminUsername;
    private String adminPassword;
    private int featureTypeCacheSize;
    private Boolean globalServices;
    private Boolean useHeadersProxyURL;
    private Integer xmlPostRequestLogBufferSize;
    private String lockProviderName;
    private WebUIMode webUIMode;
    private Boolean allowStoredQueriesPerWorkspace;
    private ResourceErrorHandling resourceErrorHandling;

    /**
     * @since geoserver 2.24.0
     */
    private boolean trailingSlashMatch;
}
