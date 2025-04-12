/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.event;

@SuppressWarnings("serial")
public class ConfigChangeEvent extends GeoWebCacheEvent {

    public static final String OBJECT_ID = "gwcConfig";

    public ConfigChangeEvent(Object source) {
        super(source, Type.MODIFIED);
    }

    @Override
    protected String getObjectId() {
        return OBJECT_ID;
    }
}
