/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.validation;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.catalog.impl.WMTSLayerInfoImpl;
import org.geoserver.ows.util.OwsUtils;

import java.util.ArrayList;
import java.util.List;

/** */
public class DefaultPropertyValuesResolver {

    private Catalog catalog;

    public DefaultPropertyValuesResolver(Catalog catalog) {
        this.catalog = catalog;
    }

    public void resolve(CatalogInfo info) {
        if (info instanceof LayerGroupInfo lg) {
            resolve(lg);
        } else if (info instanceof LayerInfo l) {
            resolve(l);
        } else if (info instanceof MapInfo m) {
            resolve(m);
        } else if (info instanceof NamespaceInfo ns) {
            resolve(ns);
        } else if (info instanceof ResourceInfo res) {
            resolve(res);
        } else if (info instanceof StoreInfo st) {
            resolve(st);
        } else if (info instanceof StyleInfo style) {
            resolve(style);
        } else if (info instanceof WorkspaceInfo ws) {
            resolve(ws);
        } else {
            throw new IllegalArgumentException("Unknown resource type: " + info);
        }
    }

    protected WorkspaceInfo resolve(WorkspaceInfo workspace) {
        resolveCollections(workspace);
        return workspace;
    }

    private void resolve(NamespaceInfo namespace) {
        resolveCollections(namespace);
    }

    private void resolve(StoreInfo store) {
        resolveCollections(store);
        StoreInfoImpl s = (StoreInfoImpl) store;
        s.setCatalog(catalog);
    }

    private void resolve(ResourceInfo resource) {
        ResourceInfoImpl r = (ResourceInfoImpl) resource;
        r.setCatalog(catalog);

        if (resource instanceof FeatureTypeInfo ft) {
            resolve(ft);
        }
        if (r instanceof CoverageInfo c) {
            resolve(c);
        }
        if (r instanceof WMSLayerInfo wms) {
            resolve(wms);
        }
        if (r instanceof WMTSLayerInfo wmts) {
            resolve(wmts);
        }
    }

    private CoverageInfo resolve(CoverageInfo r) {
        if (r instanceof CoverageInfoImpl c) {
            if (c.getDimensions() != null) {
                for (CoverageDimensionInfo dim : c.getDimensions()) {
                    if (dim.getNullValues() == null) {
                        ((CoverageDimensionImpl) dim).setNullValues(new ArrayList<Double>());
                    }
                }
            }
            resolveCollections(r);
        }
        return r;
    }

    /**
     * We don't want the world to be able and call this without going through {@link
     * #resolve(ResourceInfo)}
     */
    private void resolve(FeatureTypeInfo featureType) {
        FeatureTypeInfoImpl ft = (FeatureTypeInfoImpl) featureType;
        resolveCollections(ft);
    }

    private WMSLayerInfo resolve(WMSLayerInfo wmsLayer) {
        if (wmsLayer instanceof WMSLayerInfoImpl impl) {
            resolveCollections(impl);
        }
        return wmsLayer;
    }

    private WMTSLayerInfo resolve(WMTSLayerInfo wmtsLayer) {
        if (wmtsLayer instanceof WMTSLayerInfoImpl impl) {
            resolveCollections(impl);
        }
        return wmtsLayer;
    }

    private void resolve(LayerInfo layer) {
        if (layer.getAttribution() == null) {
            layer.setAttribution(catalog.getFactory().createAttribution());
        }
        if (layer.getType() == null) {
            if (layer.getResource() instanceof FeatureTypeInfo) {
                layer.setType(PublishedType.VECTOR);
            } else if (layer.getResource() instanceof CoverageInfo) {
                layer.setType(PublishedType.RASTER);
            } else if (layer.getResource() instanceof WMTSLayerInfo) {
                layer.setType(PublishedType.WMTS);
            } else if (layer.getResource() instanceof WMSLayerInfo) {
                layer.setType(PublishedType.WMS);
            } else {
                String msg = "Layer type not set and can't be derived from resource";
                throw new IllegalArgumentException(msg);
            }
        }
        resolveCollections(layer);
    }

    private void resolve(LayerGroupInfo layerGroup) {
        List<StyleInfo> styles = layerGroup.getStyles();
        if (styles.isEmpty()) {
            layerGroup.getLayers().forEach(l -> styles.add(null));
        }
        resolveCollections(layerGroup);
    }

    private void resolve(StyleInfo style) {
        ((StyleInfoImpl) style).setCatalog(catalog);
    }

    private void resolve(MapInfo map) {
        resolveCollections(map);
    }

    /** Method which reflectively sets all collections when they are null. */
    protected void resolveCollections(Object object) {
        OwsUtils.resolveCollections(object);
    }
}
