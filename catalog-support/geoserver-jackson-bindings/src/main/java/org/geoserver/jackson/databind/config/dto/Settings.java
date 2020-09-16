/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto;

import java.util.Map;
import lombok.Data;
import org.geoserver.config.SettingsInfo;
import org.geoserver.jackson.databind.catalog.dto.InfoReference;

/** DTO for {@link SettingsInfo} */
public @Data class Settings {
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
    private Map<String, Object> metadata;
    // seems not to be used at all in geoserver
    // Map<Object, Object> clientProperties;
    private boolean localWorkspaceIncludesPrefix;
    private boolean showCreatedTimeColumnsInAdminList;
    private boolean showModifiedTimeColumnsInAdminList;
}
