/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.pgraster;

import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.DataStorePanelInfo;
import org.geoserver.web.data.store.pgraster.PGRasterCoverageStoreEditPanel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ImportResource;

/**
 * Auto configuration to enable the pg_raster customized store panel when the web-ui is present.
 *
 * @implNote importing {@literal classpath:pgrasterApplicationContext.xml} instead of defining the
 *     bean in place because of parameterized class incompatibility on {@link
 *     org.geoserver.web.data.resource.DataStorePanelInfo#setComponentClass(Class)}
 */
@AutoConfiguration
@ConditionalOnClass({
    GeoServerApplication.class,
    DataStorePanelInfo.class,
    PGRasterCoverageStoreEditPanel.class
})
@ImportResource(locations = "classpath:pgrasterApplicationContext.xml")
public class PostgisRasterWebUIAutoConfiguration {}
