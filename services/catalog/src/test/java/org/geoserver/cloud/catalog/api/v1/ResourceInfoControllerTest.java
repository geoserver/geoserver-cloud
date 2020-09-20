/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.cloud.catalog.test.CatalogTestClient;
import org.geoserver.ows.util.OwsUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

@Ignore
@AutoConfigureWebTestClient(timeout = "360000")
public class ResourceInfoControllerTest extends AbstractCatalogInfoControllerTest<ResourceInfo> {

    public ResourceInfoControllerTest() {
        super(ResourceController.BASE_URI, ResourceInfo.class);
    }

    protected @Override void assertPropertriesEqual(ResourceInfo expected, ResourceInfo actual) {
        assertEquals(expected.getAbstract(), actual.getAbstract());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.isEnabled(), actual.isEnabled());
        assertEquals(expected.isAdvertised(), actual.isAdvertised());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getStore().getId(), actual.getStore().getId());
    }

    public @Test void featureTypeCRUD() {
        FeatureTypeInfo resource =
                testData.createFeatureType(
                        "featureTypeCRUD",
                        testData.dataStoreC,
                        testData.namespaceC,
                        "featureTypeCRUD_name",
                        "featureTypeCRUD abs",
                        "featureTypeCRUD desc",
                        true);
        crudTest(
                resource,
                r -> {
                    resource.setEnabled(true);
                    resource.setName("modified ft name");
                    resource.setDescription("new description");
                },
                catalog::getFeatureType);
    }

    public @Test void coverageCRUD() {
        CoverageInfo resource =
                testData.createCoverage(
                        "coverageCRUD", testData.coverageStoreA, "coverageCRUD_name");
        crudTest(
                resource,
                r -> {
                    resource.setEnabled(true);
                    resource.setName("modified coverage name");
                    resource.setDescription("new description");
                },
                catalog::getCoverage);
    }

    /**
     * Horror here, ignored because {@link OwsUtils#copy} tries to connect to the real WMS service
     * as calls {@link WMSLayerInfoImpl#getRemoteStyleInfos()} (fault is on WMSLayerInfo having a
     * getter name for a method that does I/O).
     *
     * <p>{@code java.lang.IllegalAccessException: class
     * org.geoserver.catalog.impl.ModificationProxyCloner cannot access a member of class
     * java.util.Collections$EmptySet (in module java.base) with modifiers "private"}
     */
    @Ignore
    public @Test void wmsLayerCRUD() {
        WMSLayerInfo resource =
                testData.createWMSLayer(
                        "wmsLayerCRUD",
                        testData.wmsStoreA,
                        testData.namespaceA,
                        "wmsLayerCRUD_name",
                        false);
        crudTest(
                resource,
                r -> {
                    resource.setEnabled(false);
                    resource.setName("modified wms layer name");
                    resource.setDescription("new description");
                },
                id -> catalog.getResource(id, WMSLayerInfo.class));
    }

    public @Test void wmtsLayerCRUD() {
        WMTSLayerInfo resource =
                testData.createWMTSLayer(
                        "wmtsLayerCRUD",
                        testData.wmtsStoreA,
                        testData.namespaceA,
                        "wmtsLayerCRUD_name",
                        false);
        crudTest(
                resource,
                r -> {
                    resource.setEnabled(true);
                    resource.setName("modified wtms layer name");
                    resource.setDescription("new description");
                },
                id -> catalog.getResource(id, WMTSLayerInfo.class));
    }

    public @Test void findResourceInfoById() {
        testFindById(testData.featureTypeA);
        testFindById(testData.coverageA);
        testFindById(testData.wmsLayerA);
        testFindById(testData.wmtsLayerA);
    }

    public @Test void findResourceInfoById_SubtypeMismatch() throws IOException {
        CatalogTestClient<ResourceInfo> client = client();
        client.findById(testData.featureTypeA.getId(), CoverageInfo.class)
                .expectStatus()
                .isNotFound();
        client.findById(testData.coverageA.getId(), FeatureTypeInfo.class)
                .expectStatus()
                .isNotFound();
        client.findById(testData.wmsLayerA.getId(), WMTSLayerInfo.class)
                .expectStatus()
                .isNotFound();
        client.findById(testData.wmtsLayerA.getId(), WMSLayerInfo.class)
                .expectStatus()
                .isNotFound();
    }
}
