/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus;

import lombok.Data;

public @Data class GeoServerBusProperties {
    private boolean sendObject = false;
    private boolean sendDiff = false;
}
