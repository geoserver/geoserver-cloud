package org.geoserver.catalog.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.catalog.impl.WMTSLayerInfoImpl;
import org.geoserver.catalog.impl.WMTSStoreInfoImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractCatalogVisitorTest {

    private StoreInfo visitedStore;
    private ResourceInfo visitedResource;
    private AbstractCatalogVisitor visitor;

    @BeforeEach
    void setup() {
        visitor =
                new AbstractCatalogVisitor() {
                    @Override
                    protected void visit(StoreInfo store) {
                        visitedStore = store;
                    }

                    @Override
                    protected void visit(ResourceInfo resource) {
                        visitedResource = resource;
                    }
                };
    }

    @Test
    void testResourceInfo_FeatureType() {
        assertResource(new FeatureTypeInfoImpl(null), FeatureTypeInfo.class);
    }

    @Test
    void testResourceInfo_Coverage() {
        assertResource(new CoverageInfoImpl(null), CoverageInfo.class);
    }

    @Test
    void testResourceInfo_WMSLayer() {
        assertResource(new WMSLayerInfoImpl(null), WMSLayerInfo.class);
    }

    @Test
    void testResourceInfo_WMTSLayer() {
        assertResource(new WMTSLayerInfoImpl(null), WMTSLayerInfo.class);
    }

    @Test
    void testResourceInfo_CoverageStore() {
        assertStore(new CoverageStoreInfoImpl(null), CoverageStoreInfo.class);
    }

    @Test
    void testResourceInfo_DataStore() {
        assertStore(new DataStoreInfoImpl(null), DataStoreInfo.class);
    }

    @Test
    void testResourceInfo_WMSStore() {
        assertStore(new WMSStoreInfoImpl(null), WMSStoreInfo.class);
    }

    @Test
    void testResourceInfo_WMTSStore() {
        assertStore(new WMTSStoreInfoImpl(null), WMTSStoreInfo.class);
    }

    private <T extends ResourceInfo> void assertResource(T resource, Class<T> type) {
        resource.accept(visitor);
        assertThat(visitedResource).isSameAs(resource);
        T proxy = ModificationProxy.create(resource, type);
        proxy.accept(visitor);
        assertThat(visitedResource).isSameAs(proxy);
    }

    private <T extends StoreInfo> void assertStore(T resource, Class<T> type) {
        resource.accept(visitor);
        assertThat(visitedStore).isSameAs(resource);
        T proxy = ModificationProxy.create(resource, type);
        proxy.accept(visitor);
        assertThat(visitedStore).isSameAs(proxy);
    }
}
