/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.integration.local;

import lombok.NonNull;

import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.TileRange;
import org.gwc.tiling.model.CacheIdentifier;
import org.gwc.tiling.model.MetaTileIdentifier;
import org.gwc.tiling.model.TileIdentifier;
import org.gwc.tiling.model.TileIndex3D;
import org.gwc.tiling.model.TileRange3D;
import org.gwc.tiling.service.MetaTileRequest;
import org.gwc.tiling.service.TileLayerSeeder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @since 1.0
 */
public class GeoServerTileLayerSeeder extends TileLayerSeeder {

    private StorageBroker storageBroker;
    private GeoServerTileLayer geoserverTileLayer;

    public GeoServerTileLayerSeeder(GeoServerTileLayer layer) {
        super(new TileLayerInfoAdapter(layer));
        this.geoserverTileLayer = layer;
    }

    @Override
    public void seed(MetaTileRequest request) {
        final MimeType mime = getLayerFormat(request.cache().getFormat());
        Stream<TileIdentifier> missing = findMissing(request.getMetaTile());
        if (mime.supportsTiling()) {
            missing = missing.limit(1);
        }
        this.seed(request, missing);
    }

    @Override
    public void reseed(MetaTileRequest request) {
        Stream<TileIdentifier> all = request.tiles();
        this.seed(request, all);
    }

    @Override
    public void truncate(MetaTileRequest metatile) {
        TileRange tileRange = toTileRange(metatile);
        try {
            storageBroker.delete(tileRange);
        } catch (StorageException e) {
            throw new IllegalStateException(e);
        }
    }

    private void seed(MetaTileRequest req, Stream<TileIdentifier> tiles) {
        ConveyorTile proto = createConveyorTileProto(req);
        tiles.sequential()
                .map(
                        tileId -> {
                            proto.getTileIndex()[0] = tileId.x();
                            proto.getTileIndex()[1] = tileId.y();
                            proto.getTileIndex()[2] = tileId.z();
                            return proto;
                        })
                .forEach(this::seed);
    }

    protected ConveyorTile createConveyorTileProto(MetaTileRequest req) {
        final CacheIdentifier cacheId = req.cache();
        final MimeType mimeType = getLayerFormat(cacheId.getFormat());
        String layerName = cacheId.getLayerName();
        String gridsetId = cacheId.getGridsetId();
        long[] tileIndex = new long[3];
        Map<String, String[]> fullParameters = null; // unnecessary/unsused
        Map<String, String> filteringParameters = null; // unnecessary/unsused
        HttpServletRequest servletReq = null; // unnecessary/unsused
        HttpServletResponse servletResponse = null; // unnecessary/unsused

        String parametersId = cacheId.getParametersId().orElse(null);
        ConveyorTile proto =
                new ConveyorTile(
                        storageBroker,
                        layerName,
                        gridsetId,
                        tileIndex,
                        mimeType,
                        fullParameters,
                        filteringParameters,
                        servletReq,
                        servletResponse);
        proto.getStorageObject().setParametersId(parametersId);
        return proto;
    }

    private void seed(ConveyorTile tile) {
        final boolean tryCache = false;
        try {
            this.geoserverTileLayer.seedTile(tile, tryCache);
        } catch (GeoWebCacheException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private TileRange toTileRange(MetaTileRequest metatile) {
        final CacheIdentifier cacheId = metatile.cache();

        String layerName = cacheId.getLayerName();
        String gridsetId = cacheId.getGridsetId();
        int zoomStart = metatile.getMetaTile().getTiles().getZoomLevel();
        int zoomStop = zoomStart;
        long[][] rangeBounds = toRangeBounds(metatile.getMetaTile().getTiles());
        MimeType mimeType = this.getLayerFormat(cacheId.getFormat());
        Map<String, String> parameters = Map.of();
        String parametersId = cacheId.getParametersId().orElse(null);

        return new TileRange(
                layerName,
                gridsetId,
                zoomStart,
                zoomStop,
                rangeBounds,
                mimeType,
                parameters,
                parametersId);
    }

    /**
     * @return {@code [[minx,miny,maxx,maxy,level]]}
     */
    private long[][] toRangeBounds(@NonNull TileRange3D range) {
        long[][] bounds = {
            {range.minx(), range.miny(), range.maxx(), range.maxy(), range.getZoomLevel()}
        };
        return bounds;
    }

    private Stream<TileIdentifier> findMissing(MetaTileIdentifier metatile) {
        CacheIdentifier cacheId = metatile.getCache();
        TileObject tileProto = createTileProto(cacheId);
        return metatile.asTiles().sequential().filter(tileId -> this.isMissing(tileId, tileProto));
    }

    protected TileObject createTileProto(CacheIdentifier cacheId) {
        String layerName = getLayer().getName();
        long[] xyz = new long[3];
        String gridsetId = cacheId.getGridsetId();
        String format = cacheId.getFormat();
        Map<String, String> parametersUnused = Map.of();

        TileObject tileProto =
                TileObject.createQueryTileObject(
                        layerName, xyz, gridsetId, format, parametersUnused);
        tileProto.setParametersId(cacheId.getParametersId().orElse(null));
        return tileProto;
    }

    private boolean isMissing(TileIdentifier tileId, TileObject tileProto) {
        boolean found = this.find(tileId, tileProto);
        return !found;
    }

    private boolean find(TileIdentifier tileId, TileObject tileProto) {
        TileIndex3D tileIndex = tileId.getTileIndex();
        long[] xyz = tileProto.getXYZ();
        xyz[0] = tileIndex.getX();
        xyz[1] = tileIndex.getY();
        xyz[2] = tileIndex.getZ();
        boolean found;
        try {
            found = storageBroker.get(tileProto);
        } catch (StorageException e) {
            throw new IllegalStateException(e);
        }
        return !found;
    }

    private MimeType getLayerFormat(@NonNull String format) {
        MimeType expected;
        try {
            expected = MimeType.createFromFormat(format);
        } catch (MimeException e) {
            throw new IllegalArgumentException(e);
        }
        List<MimeType> mimeTypes = geoserverTileLayer.getMimeTypes();
        if (!mimeTypes.contains(expected)) {
            throw new IllegalArgumentException(
                    format
                            + " is not a supported format for layer "
                            + geoserverTileLayer.getName());
        }
        return expected;
    }
}
