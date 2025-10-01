/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Locale;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.geoserver.config.SettingsInfo;
import org.geoserver.jackson.databind.catalog.dto.MetadataMapDto;

/** DTO for {@link SettingsInfo} */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("SettingsInfo")
public class Settings extends ConfigInfoDto {
    private String workspace;
    private String title;
    private Contact contact;
    private String charset;
    private int numDecimals;
    private String onlineResource;
    private String proxyBaseUrl;
    private String schemaBaseUrl;
    private boolean verbose;
    private boolean verboseExceptions;
    private MetadataMapDto metadata;
    private boolean localWorkspaceIncludesPrefix;
    private boolean showCreatedTimeColumnsInAdminList;
    private boolean showModifiedTimeColumnsInAdminList;

    /**
     * @since geoserver 2.20.0
     */
    private Locale defaultLocale;

    /**
     * @since geoserver 2.22.0
     */
    private boolean useHeadersProxyURL;

    /**
     * @since 2.28.0
     */
    private boolean isShowModifiedUserInAdminList;
}
