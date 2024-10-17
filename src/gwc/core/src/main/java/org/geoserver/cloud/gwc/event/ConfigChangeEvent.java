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
