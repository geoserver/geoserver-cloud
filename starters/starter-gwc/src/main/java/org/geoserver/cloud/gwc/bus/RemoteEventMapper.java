/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.bus;

import lombok.NonNull;
import org.geoserver.cloud.gwc.event.BlobStoreEvent;
import org.geoserver.cloud.gwc.event.GeoWebCacheEvent;
import org.geoserver.cloud.gwc.event.GridsetEvent;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import org.mapstruct.ReportingPolicy;

/** @since 1.0 */
@Mapper(componentModel = "default", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface RemoteEventMapper {

    default RemoteGeoWebCacheEvent toRemote(
            @NonNull GeoWebCacheEvent local,
            @NonNull Object source,
            @NonNull String originService) {
        if (local instanceof TileLayerEvent)
            return map((TileLayerEvent) local, source, originService);
        if (local instanceof GridsetEvent) return map((GridsetEvent) local, source, originService);
        if (local instanceof BlobStoreEvent)
            return map((BlobStoreEvent) local, source, originService);
        throw new IllegalArgumentException("unknown GeoWebCacheEvent type: " + local);
    }

    default GeoWebCacheEvent toLocal(@NonNull RemoteGeoWebCacheEvent remote) {
        if (remote instanceof RemoteTileLayerEvent) return map((RemoteTileLayerEvent) remote);
        if (remote instanceof RemoteGridsetEvent) return map((RemoteGridsetEvent) remote);
        if (remote instanceof RemoteBlobStoreEvent) return map((RemoteBlobStoreEvent) remote);
        throw new IllegalArgumentException("unknown RemoteGeoWebCacheEvent type: " + remote);
    }

    TileLayerEvent map(RemoteTileLayerEvent remote);

    RemoteTileLayerEvent map(
            TileLayerEvent local, @Context Object source, @Context String originService);

    GridsetEvent map(RemoteGridsetEvent remote);

    RemoteGridsetEvent map(
            GridsetEvent local, @Context Object source, @Context String originService);

    BlobStoreEvent map(RemoteBlobStoreEvent remote);

    RemoteBlobStoreEvent map(
            BlobStoreEvent local, @Context Object source, @Context String originService);

    @ObjectFactory
    default RemoteTileLayerEvent newRemoteTileEvent(
            @Context Object source, @Context String originService) {
        return new RemoteTileLayerEvent(source, originService);
    }

    @ObjectFactory
    default RemoteGridsetEvent newRemoteGridsetEvent(
            @Context Object source, @Context String originService) {
        return new RemoteGridsetEvent(source, originService);
    }

    @ObjectFactory
    default RemoteBlobStoreEvent newRemoteBlobStoreEvent(
            @Context Object source, @Context String originService) {
        return new RemoteBlobStoreEvent(source, originService);
    }
}
