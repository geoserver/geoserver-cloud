/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;

import org.geoserver.config.SettingsInfo;
import org.geoserver.jackson.databind.catalog.dto.InfoReference;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

/** DTO for {@link SettingsInfo} */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SettingsInfo")
public @Data @Generated class Settings extends ConfigInfoDto {
    private InfoReference workspace;
    private String title;
    private Contact contact;
    private String charset;
    private int numDecimals;
    private String onlineResource;
    private String proxyBaseUrl;
    private String schemaBaseUrl;
    private boolean verbose;
    private boolean verboseExceptions;
    private Map<String, Serializable> metadata;
    // seems not to be used at all in geoserver
    // Map<Object, Object> clientProperties;
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
}
