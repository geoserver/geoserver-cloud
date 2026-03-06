/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.UserDetailsDisplaySettingsInfo;
import org.geoserver.config.UserDetailsDisplaySettingsInfo.EmailDisplayMode;
import org.geoserver.config.UserDetailsDisplaySettingsInfo.LoggedInUserDisplayMode;
import org.geoserver.jackson.databind.catalog.dto.MetadataMapDto;

/** DTO for {@link GeoServerInfo} */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("GeoServerInfo")
public class GeoServerInfoDto extends ConfigInfoDto {
    /** DTO for {@link org.geoserver.config.ResourceErrorHandling} */
    @JsonTypeName("ResourceErrorHandling")
    public enum ResourceErrorHandlingDto {
        OGC_EXCEPTION_REPORT,
        SKIP_MISCONFIGURED_LAYERS
    }

    /** DTO for {@link GeoServerInfo.WebUIMode} */
    @JsonTypeName("WebUIMode")
    public enum WebUIModeDto {
        DEFAULT,
        REDIRECT,
        DO_NOT_REDIRECT
    }

    private SettingsInfoDto settings;

    @JsonAlias({"jai", "JAI"})
    private ImageProcessingInfoDto imageProcessing;

    private CoverageAccessInfoDto coverageAccess;
    private MetadataMapDto metadata;
    private long updateSequence;
    private String adminUsername;
    private String adminPassword;
    private int featureTypeCacheSize;
    private Boolean globalServices;
    private Integer xmlPostRequestLogBufferSize;
    private String lockProviderName;
    private WebUIModeDto webUIMode;
    private Boolean allowStoredQueriesPerWorkspace;
    private ResourceErrorHandlingDto resourceErrorHandling;

    /** @since geoserver 2.24.0 */
    private boolean trailingSlashMatch;

    /** @since 2.28.1 */
    private UserDetailsDisplaySettingsInfoDto userDetailsDisplaySettings = new UserDetailsDisplaySettingsInfoDto();

    /**
     * DTO for {@link UserDetailsDisplaySettingsInfo}
     *
     * @since 2.28.1
     */
    @Data
    @JsonTypeName("UserDetailsDisplaySettingsInfo")
    public static class UserDetailsDisplaySettingsInfoDto {

        /** DTO for {@link LoggedInUserDisplayMode} */
        @JsonTypeName("LoggedInUserDisplayMode")
        public enum LoggedInUserDisplayModeDto {
            USERNAME,
            PREFERRED_USERNAME,
            FULL_NAME,
            FALLBACK
        }

        /** DTO for {@link EmailDisplayMode} */
        @JsonTypeName("EmailDisplayMode")
        public enum EmailDisplayModeDto {
            HIDDEN,
            DOMAIN_ONLY,
            MASKED,
            FULL
        }

        // ignoring String id, this is not an entity but a value object

        LoggedInUserDisplayModeDto loggedInUserDisplayMode = LoggedInUserDisplayModeDto.USERNAME;
        boolean showProfileColumnsInUserList;
        EmailDisplayModeDto emailDisplayMode = EmailDisplayModeDto.DOMAIN_ONLY;
        boolean revealEmailAtClick;
    }
}
