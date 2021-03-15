/*
 * (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.config.plugin;

import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.SettingsInfo;

public interface RepositoryGeoServerFacade extends GeoServerFacade {

    SettingsInfo getSettings(String id);

    void setRepository(ConfigRepository repository);
}
