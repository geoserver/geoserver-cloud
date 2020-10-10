/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerFacade;

/** */
public interface CachingGeoServerFacade extends GeoServerFacade {

    String CACHE_NAME = "config";
    String GEOSERVERINFO_KEY = "global_GeoServer";
    String LOGGINGINFO_KEY = "global_Logging";

    public static Object settingsKey(WorkspaceInfo ws) {
        return "settings@" + ws.getId();
    }

    boolean evict(Info info);
}
