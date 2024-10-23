/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.bus;

import lombok.Generated;
import lombok.NonNull;
import org.geoserver.cloud.gwc.event.BlobStoreEvent;
import org.geoserver.cloud.gwc.event.ConfigChangeEvent;
import org.geoserver.cloud.gwc.event.GeoWebCacheEvent;
import org.geoserver.cloud.gwc.event.GridsetEvent;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import org.mapstruct.ReportingPolicy;

/**
 * @since 1.0
 */
@Mapper(componentModel = "default", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Generated.class)
interface RemoteEventMapper {

    default RemoteGeoWebCacheEvent toRemote(
            @NonNull GeoWebCacheEvent local, @NonNull Object source, @NonNull String originService) {

        return switch (local) {
            case TileLayerEvent tle -> toRemote(tle, source, originService);
            case GridsetEvent gse -> toRemote(gse, source, originService);
            case BlobStoreEvent bse -> toRemote(bse, source, originService);
            case ConfigChangeEvent ce -> toRemote(ce, source, originService);
            default -> throw new IllegalArgumentException("unknown GeoWebCacheEvent type: " + local);
        };
    }

    default GeoWebCacheEvent toLocal(@NonNull RemoteGeoWebCacheEvent remote, @NonNull Object source) {
        return switch (remote) {
            case RemoteTileLayerEvent tle -> toLocal(tle, source);
            case RemoteGridsetEvent gse -> toLocal(gse, source);
            case RemoteBlobStoreEvent bse -> toLocal(bse, source);
            case RemoteConfigChangeEvent ce -> toLocal(ce, source);
            default -> throw new IllegalArgumentException("unknown RemoteGeoWebCacheEvent type: " + remote);
        };
    }

    TileLayerEvent toLocal(RemoteTileLayerEvent remote, @Context Object source);

    RemoteTileLayerEvent toRemote(TileLayerEvent local, @Context Object source, @Context String originService);

    GridsetEvent toLocal(RemoteGridsetEvent remote, @Context Object source);

    RemoteGridsetEvent toRemote(GridsetEvent local, @Context Object source, @Context String originService);

    BlobStoreEvent toLocal(RemoteBlobStoreEvent remote, @Context Object source);

    RemoteBlobStoreEvent toRemote(BlobStoreEvent local, @Context Object source, @Context String originService);

    ConfigChangeEvent toLocal(RemoteConfigChangeEvent remote, @Context Object source);

    RemoteConfigChangeEvent toRemote(ConfigChangeEvent local, @Context Object source, @Context String originService);

    @ObjectFactory
    default TileLayerEvent newTileEvent(@Context Object source) {
        return new TileLayerEvent(source);
    }

    @ObjectFactory
    default RemoteTileLayerEvent newRemoteTileEvent(@Context Object source, @Context String originService) {
        return new RemoteTileLayerEvent(source, originService);
    }

    @ObjectFactory
    default GridsetEvent newGridsetEvent(@Context Object source) {
        return new GridsetEvent(source);
    }

    @ObjectFactory
    default RemoteGridsetEvent newRemoteGridsetEvent(@Context Object source, @Context String originService) {
        return new RemoteGridsetEvent(source, originService);
    }

    @ObjectFactory
    default BlobStoreEvent newBlobStoreEvent(@Context Object source) {
        return new BlobStoreEvent(source);
    }

    @ObjectFactory
    default RemoteBlobStoreEvent newRemoteBlobStoreEvent(@Context Object source, @Context String originService) {
        return new RemoteBlobStoreEvent(source, originService);
    }

    @ObjectFactory
    default ConfigChangeEvent newConfigChangeEvent(@Context Object source) {
        return new ConfigChangeEvent(source);
    }

    @ObjectFactory
    default RemoteConfigChangeEvent newConfigChangeEvent(@Context Object source, @Context String originService) {
        return new RemoteConfigChangeEvent(source, originService);
    }
}
