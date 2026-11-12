/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.bus;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.gwc.event.GeoWebCacheEvent;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RemoteEventMapperTest {

    private final RemoteEventMapper mapper = Mappers.getMapper(RemoteEventMapper.class);

    @Test
    void localCopyIsRemote() {
        TileLayerEvent created = TileLayerEvent.created(this, "id", "ws:roads");
        RemoteGeoWebCacheEvent remote = mapper.toRemote(created, this, "other-service");

        GeoWebCacheEvent local = mapper.toLocal(remote, this);

        assertThat(local.isRemote()).isTrue();
        assertThat(created.isRemote()).isFalse();
    }
}
