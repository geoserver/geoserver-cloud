package org.geowebcache;

import org.apache.commons.lang.StringUtils;
import org.geowebcache.config.ServerConfiguration;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.stats.RuntimeStats;
import org.geowebcache.storage.BlobStoreAggregator;
import org.geowebcache.storage.StorageBroker;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

public class GSCloudGeoWebCacheDispatcher extends GeoWebCacheDispatcher {

    /**
     * Should be invoked through Spring
     *
     * @param tileLayerDispatcher
     * @param gridSetBroker
     * @param storageBroker
     * @param blobStoreAggregator
     * @param mainConfiguration
     * @param runtimeStats
     */
    public GSCloudGeoWebCacheDispatcher(
            TileLayerDispatcher tileLayerDispatcher,
            GridSetBroker gridSetBroker,
            StorageBroker storageBroker,
            BlobStoreAggregator blobStoreAggregator,
            ServerConfiguration mainConfiguration,
            RuntimeStats runtimeStats) {
        super(
                tileLayerDispatcher,
                gridSetBroker,
                storageBroker,
                blobStoreAggregator,
                mainConfiguration,
                runtimeStats);
    }

    @Override
    String normalizeURL(HttpServletRequest request) {
        String normalized = request.getRequestURI();
        List<String> elements = Arrays.asList(normalized.split("/"));

        // only /home /service and /demo are handled.

        // at the end, we should have
        // /{home|service|demo}/{...}

        // requestURI will always begin with a "/", so elements
        // will be either:
        // [ "", "gwc", "service|home|demo", "layer (prefixed or not)" ]
        // [ "", "{namespace}", "gwc", "service|home|demo", "layer (prefixed or not)" ]
        // [ "", "whateveer", "path", "{namespace}", "gwc", "service|home|demo", "layer (prefixed or
        // not)" ]
        // ...

        boolean isNamespacePrefixed = (elements.indexOf("gwc") > 1);
        if (!isNamespacePrefixed) {
            return "/" + elements.stream().skip(2).collect(Collectors.joining("/"));
        } else {
            String srv = elements.get(elements.indexOf("gwc") + 1);

            String extraPath =
                    elements.stream()
                            .skip(elements.indexOf("gwc") + 2)
                            .collect(Collectors.joining("/"));

            // If demo is requested, we have to provide
            // a fully-qualified layer name.
            if ("demo".equals(srv)) {
                String namespace = elements.get(elements.indexOf("gwc") - 1);
                // extraPath is empty: return /demo
                if (StringUtils.isEmpty(extraPath)) return "/demo";
                if (extraPath.startsWith(namespace + ":")) {
                    return String.format("/%s/%s", srv, extraPath);
                } else {
                    return String.format("/%s/%s:%s", srv, namespace, extraPath);
                }
            }

            if (StringUtils.isEmpty(extraPath)) {
                return String.format("/%s", srv);
            }
            return String.format("/%s/%s", srv, extraPath);
        }
    }
}
