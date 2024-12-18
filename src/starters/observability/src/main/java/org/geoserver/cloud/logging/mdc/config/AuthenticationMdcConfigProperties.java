/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.mdc.config;

import lombok.Data;

@Data
public class AuthenticationMdcConfigProperties {

    /** Whether to append the enduser.id MDC property from the Authentication name */
    private boolean id = false;

    /**
     * Whether to append the enduser.roles MDC property from the Authentication granted authorities
     */
    private boolean roles = false;
}
