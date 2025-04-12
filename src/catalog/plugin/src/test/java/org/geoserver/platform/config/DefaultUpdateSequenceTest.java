/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform.config;

import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.plugin.GeoServerImpl;
import org.junit.jupiter.api.BeforeEach;

/**
 * @since 1.2
 */
class DefaultUpdateSequenceTest implements UpdateSequenceConformanceTest {

    GeoServer gs;
    UpdateSequence updateSequence;

    @BeforeEach
    void init() {
        gs = new GeoServerImpl();
        gs.setGlobal(new GeoServerInfoImpl(gs));
        updateSequence = new DefaultUpdateSequence(gs);
    }

    @Override
    public UpdateSequence getUpdataSequence() {
        return updateSequence;
    }

    @Override
    public GeoServer getGeoSever() {
        return gs;
    }
}
