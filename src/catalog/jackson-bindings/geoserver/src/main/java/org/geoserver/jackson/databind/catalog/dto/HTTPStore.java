/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonSubTypes({@JsonSubTypes.Type(value = WMSStore.class), @JsonSubTypes.Type(value = WMTSStore.class)})
public abstract class HTTPStore extends Store {
    private String capabilitiesURL;
    private String username;
    private String password;
    private int maxConnections;
    private int readTimeout;
    private int connectTimeout;
    private boolean useConnectionPooling;

    /** Pulled up from {@link WMTSStore} to match the GeoServer 2.25.1 refactoring */
    private String headerName;

    /** Pulled up from {@link WMTSStore} to match the GeoServer 2.25.1 refactoring */
    private String headerValue;

    /**
     * @since GeoServer 2.25.1
     */
    private String authKey;
}
