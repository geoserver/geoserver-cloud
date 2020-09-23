/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.ows.util.OwsUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@EnableAutoConfiguration
@Accessors(fluent = true)
public class ResourceRepositoryTest
        extends AbstractCatalogServiceClientRepositoryTest<ResourceInfo, ResourceRepository> {

    private @Autowired @Getter ResourceRepository repository;

    public ResourceRepositoryTest() {
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
        super.testFindAllIncludeFilter(
                ResourceInfo.class,
                testData.featureTypeA,
                testData.coverageA,
                testData.wmsLayerA,
                testData.wmtsLayerA);
        super.testFindAllIncludeFilter(FeatureTypeInfo.class, testData.featureTypeA);
        super.testFindAllIncludeFilter(CoverageInfo.class, testData.coverageA);
        super.testFindAllIncludeFilter(WMSLayerInfo.class, testData.wmsLayerA);
        super.testFindAllIncludeFilter(WMTSLayerInfo.class, testData.wmtsLayerA);
    }

    public @Test void testFindAllByNamespace() {
        testFind(
                () -> repository().findAllByNamespace(testData.namespaceA, ResourceInfo.class),
                testData.featureTypeA,
                testData.coverageA,
                testData.wmsLayerA,
                testData.wmtsLayerA);

        testFind(
                () -> repository.findAllByNamespace(testData.namespaceA, FeatureTypeInfo.class),
                testData.featureTypeA);
    }

    public @Test void testFindByStoreAndName() {
        ResourceRepository repository = repository();

        DataStoreInfo ds = testData.dataStoreA;
        FeatureTypeInfo ft = testData.featureTypeA;
        CoverageStoreInfo cs = testData.coverageStoreA;
        CoverageInfo cv = testData.coverageA;

        assertNotNull(repository.findByStoreAndName(ds, ft.getName(), ResourceInfo.class));
        assertNotNull(repository.findByStoreAndName(ds, ft.getName(), FeatureTypeInfo.class));
        assertNull(repository.findByStoreAndName(ds, ft.getName(), CoverageInfo.class));

        assertNotNull(repository.findByStoreAndName(cs, cv.getName(), ResourceInfo.class));
        assertNotNull(repository.findByStoreAndName(cs, cv.getName(), CoverageInfo.class));
        assertNull(repository.findByStoreAndName(cs, cv.getName(), FeatureTypeInfo.class));
    }

    public @Test void testFindAllByStore() {
        FeatureTypeInfo ftA2 = testData.createFeatureType("ftA2");
        CoverageInfo cvA2 = testData.createCoverage("cvA2");
        serverCatalog.add(ftA2);
        serverCatalog.add(cvA2);

        testFind(
                () -> repository().findAllByStore(testData.dataStoreA, FeatureTypeInfo.class),
                testData.featureTypeA,
                ftA2);
        testFind(() -> repository().findAllByStore(testData.dataStoreA, CoverageInfo.class));

        testFind(
                () -> repository().findAllByStore(testData.coverageStoreA, CoverageInfo.class),
                testData.coverageA,
                cvA2);
        testFind(() -> repository().findAllByStore(testData.coverageStoreA, FeatureTypeInfo.class));
    }

    public @Override @Test void testQueryFilter() {
        FeatureTypeInfo ft = serverCatalog.getFeatureType(testData.featureTypeA.getId());
        CoverageInfo cv = serverCatalog.getCoverage(testData.coverageA.getId());
        WMSLayerInfo wms =
                serverCatalog.getResource(testData.wmsLayerA.getId(), WMSLayerInfo.class);
        WMTSLayerInfo wmts =
                serverCatalog.getResource(testData.wmtsLayerA.getId(), WMTSLayerInfo.class);

        wms.setEnabled(false);
        wmts.setEnabled(false);
        cv.setEnabled(true);
        serverCatalog.save(wms);
        serverCatalog.save(wmts);
        serverCatalog.save(cv);

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
                serverCatalog::getFeatureType,
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
                serverCatalog::getCoverage,
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
                id -> serverCatalog.getResource(id, WMSLayerInfo.class),
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
                id -> serverCatalog.getResource(id, WMTSLayerInfo.class),
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
        ResourceRepository client = repository();
        assertNull(client.findById(testData.featureTypeA.getId(), CoverageInfo.class));
        assertNull(client.findById(testData.coverageA.getId(), FeatureTypeInfo.class));
        assertNull(client.findById(testData.wmsLayerA.getId(), WMTSLayerInfo.class));
        assertNull(client.findById(testData.wmtsLayerA.getId(), WMSLayerInfo.class));
    }

    public @Test void testFindResourceByNamespaceIdAndName() {
        NamespaceInfo ns = testData.namespaceA;
        ResourceInfo ftA = testData.featureTypeA;

        ResourceRepository client = repository();
        String name = ftA.getName();

        assertNotNull(client.findByNameAndNamespace(name, ns, ResourceInfo.class));
        assertNotNull(client.findByNameAndNamespace(name, ns, FeatureTypeInfo.class));
        assertNull(client.findByNameAndNamespace(name, ns, CoverageInfo.class));
    }

    public @Test void testFindAllBySubtype() {
        ResourceRepository client = repository();

        List<ResourceInfo> all =
                client.findAll(Filter.INCLUDE, FeatureTypeInfo.class).collect(Collectors.toList());
        assertEquals(serverCatalog.getResources(FeatureTypeInfo.class).size(), all.size());

        all = client.findAll(Filter.INCLUDE, CoverageInfo.class).collect(Collectors.toList());
        assertEquals(serverCatalog.getResources(CoverageInfo.class).size(), all.size());

        all = client.findAll(Filter.INCLUDE, WMSLayerInfo.class).collect(Collectors.toList());
        assertEquals(serverCatalog.getResources(WMSLayerInfo.class).size(), all.size());

        all = client.findAll(Filter.INCLUDE, WMTSLayerInfo.class).collect(Collectors.toList());
        assertEquals(serverCatalog.getResources(WMTSLayerInfo.class).size(), all.size());
    }
}
