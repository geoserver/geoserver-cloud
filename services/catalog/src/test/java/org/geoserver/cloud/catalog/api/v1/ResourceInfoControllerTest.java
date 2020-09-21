/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.cloud.catalog.test.CatalogTestClient;
import org.geoserver.ows.util.OwsUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;

@AutoConfigureWebTestClient(timeout = "360000")
public class ResourceInfoControllerTest
        extends AbstractReactiveCatalogControllerTest<ResourceInfo> {

    public ResourceInfoControllerTest() {
        super(ResourceInfo.class);
    }

    protected @Override void assertPropertriesEqual(ResourceInfo expected, ResourceInfo actual) {
        assertEquals(expected.getAbstract(), actual.getAbstract());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.isEnabled(), actual.isEnabled());
        assertEquals(expected.isAdvertised(), actual.isAdvertised());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getStore().getId(), actual.getStore().getId());
    }

    public @Override @Test void testFindAll() {
        super.testFindAll(
                testData.featureTypeA, testData.coverageA, testData.wmsLayerA, testData.wmtsLayerA);
    }

    public @Override @Test void testFindById() {
        super.testFindById(testData.featureTypeA);
        super.testFindById(testData.coverageA);
        super.testFindById(testData.wmsLayerA);
        super.testFindById(testData.wmtsLayerA);
    }

    public @Override @Test void testFindAllByType() {
        super.testFindAll(
                ResourceInfo.class,
                testData.featureTypeA,
                testData.coverageA,
                testData.wmsLayerA,
                testData.wmtsLayerA);
        super.testFindAll(FeatureTypeInfo.class, testData.featureTypeA);
        super.testFindAll(CoverageInfo.class, testData.coverageA);
        super.testFindAll(WMSLayerInfo.class, testData.wmsLayerA);
        super.testFindAll(WMTSLayerInfo.class, testData.wmtsLayerA);
    }

    public @Override @Test void testQueryFilter() {
        FeatureTypeInfo ft =
                catalog.getResource(testData.featureTypeA.getId(), FeatureTypeInfo.class);
        CoverageInfo cv = catalog.getResource(testData.coverageA.getId(), CoverageInfo.class);
        WMSLayerInfo wms = catalog.getResource(testData.wmsLayerA.getId(), WMSLayerInfo.class);
        WMTSLayerInfo wmts = catalog.getResource(testData.wmtsLayerA.getId(), WMTSLayerInfo.class);

        wms.setEnabled(false);
        wmts.setEnabled(false);
        cv.setEnabled(true);
        catalog.save(wms);
        catalog.save(wmts);
        catalog.save(cv);

        super.testQueryFilter(ResourceInfo.class, Filter.INCLUDE, ft, cv, wms, wmts);
        super.testQueryFilter(CoverageInfo.class, Filter.INCLUDE, cv);
        super.testQueryFilter("enabled = true", ft, cv);
        super.testQueryFilter("enabled = false", wms, wmts);
    }

    public @Test void testResourceInfoCRUD_FeatureTypeInfo() {
        FeatureTypeInfo toCreate =
                testData.createFeatureType(
                        "featureTypeCRUD",
                        testData.dataStoreC,
                        testData.namespaceC,
                        "featureTypeCRUD_name",
                        "featureTypeCRUD abs",
                        "featureTypeCRUD desc",
                        true);
        crudTest(
                toCreate,
                catalog::getFeatureType,
                created -> {
                    created.setEnabled(!created.isEnabled());
                    created.setName("modified ft name");
                    created.setDescription("new description");
                },
                (orig, updated) -> {
                    assertFalse(updated.isEnabled());
                    assertEquals("modified ft name", updated.getName());
                    assertEquals("new description", updated.getDescription());
                });
    }

    public @Test void testResourceInfoCRUD_CoverageInfo() {
        CoverageInfo toCreate =
                testData.createCoverage(
                        "coverageCRUD", testData.coverageStoreA, "coverageCRUD_name");
        crudTest(
                toCreate,
                catalog::getCoverage,
                created -> {
                    created.setEnabled(false);
                    created.setName("modified coverage name");
                    created.setDescription("new description");
                },
                (orig, updated) -> {
                    assertFalse(updated.isEnabled());
                    assertEquals("modified coverage name", updated.getName());
                    assertEquals("new description", updated.getDescription());
                });
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
    public @Test void testResourceInfoCRUD_WMSLayerInfo() {
        WMSLayerInfo toCreate =
                testData.createWMSLayer(
                        "wmsLayerCRUD",
                        testData.wmsStoreA,
                        testData.namespaceA,
                        "wmsLayerCRUD_name",
                        false);
        crudTest(
                toCreate,
                id -> catalog.getResource(id, WMSLayerInfo.class),
                created -> {
                    created.setEnabled(false);
                    created.setName("modified wms layer name");
                    created.setDescription("new description");
                },
                (orig, updated) -> {
                    assertFalse(updated.isEnabled());
                    assertEquals("modified wms layer name", updated.getName());
                    assertEquals("new description", updated.getDescription());
                });
    }

    public @Test void testResourceInfoCRUD_WMTSLayerInfo() {
        WMTSLayerInfo toCreate =
                testData.createWMTSLayer(
                        "wmtsLayerCRUD",
                        testData.wmtsStoreA,
                        testData.namespaceA,
                        "wmtsLayerCRUD_name",
                        false);
        crudTest(
                toCreate,
                id -> catalog.getResource(id, WMTSLayerInfo.class),
                created -> {
                    created.setEnabled(false);
                    created.setName("modified wtms layer name");
                    created.setDescription("new description");
                },
                (orig, updated) -> {
                    assertFalse(updated.isEnabled());
                    assertEquals("modified wtms layer name", updated.getName());
                    assertEquals("new description", updated.getDescription());
                });
    }

    public @Test void testFindResourceInfoById() {
        testFindById(testData.featureTypeA);
        testFindById(testData.coverageA);
        testFindById(testData.wmsLayerA);
        testFindById(testData.wmtsLayerA);
    }

    public @Test void testFindResourceInfoById_SubtypeMismatch() throws IOException {
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

    public @Test void testFindResourceByNamespaceIdAndName() {
        NamespaceInfo ns = testData.namespaceA;
        ResourceInfo ftA = testData.featureTypeA;

        client().getRelative(
                        "/namespaces/{namespaceId}/resources/name/{name}",
                        ns.getId(),
                        ftA.getName())
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody(FeatureTypeInfo.class)
                .consumeWith(result -> assertEquals(ftA.getId(), result.getResponseBody().getId()));
    }

    public @Test void testFindAllBySubtype() {
        List<ResourceInfo> all = super.findAll(ClassMappings.fromInterface(FeatureTypeInfo.class));
        assertEquals(catalog.getResources(FeatureTypeInfo.class).size(), all.size());

        all = super.findAll(ClassMappings.fromInterface(CoverageInfo.class));
        assertEquals(catalog.getResources(CoverageInfo.class).size(), all.size());

        all = super.findAll(ClassMappings.fromInterface(WMSLayerInfo.class));
        assertEquals(catalog.getResources(WMSLayerInfo.class).size(), all.size());

        all = super.findAll(ClassMappings.fromInterface(WMTSLayerInfo.class));
        assertEquals(catalog.getResources(WMTSLayerInfo.class).size(), all.size());
    }
}
