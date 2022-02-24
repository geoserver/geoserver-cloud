/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import org.geoserver.function.IsInstanceOf;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.filter.function.FilterFunction_toWKT;
import org.geotools.filter.function.math.FilterFunction_abs;
import org.geotools.filter.function.math.FilterFunction_acos;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.filter.Filter;
import org.opengis.filter.capability.FunctionName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootTest(classes = CatalogClientRepositoryConfiguration.class)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class CatalogClientRepositoryTest {

    private @MockBean ReactiveCatalogClient mockClient;

    private @Autowired WorkspaceRepository workspaceRepository;
    private @Autowired NamespaceRepository namespaceRepository;
    private @Autowired StoreRepository storeRepository;
    private @Autowired ResourceRepository resourceRepository;
    private @Autowired LayerRepository layerRepository;
    private @Autowired LayerGroupRepository layerGroupRepository;
    private @Autowired StyleRepository styleRepository;

    private static final Catalog fakeCatalog = new CatalogPlugin();
    public @Rule CatalogTestData testData =
            CatalogTestData.empty(() -> fakeCatalog, () -> null).initConfig(false);

    public @Before void before() {
        List<FunctionName> functions =
                Arrays.asList(
                        IsInstanceOf.NAME,
                        FilterFunction_abs.NAME,
                        FilterFunction_acos.NAME,
                        FilterFunction_toWKT.NAME);
        when(mockClient.getSupportedFilterFunctionNames()).thenReturn(Flux.fromIterable(functions));
    }

    public @Test void workspaceRepository_CRUD() {
        crudTest(workspaceRepository, testData.workspaceA);
    }

    public @Test void workspaceRepository_DefaultWorkspace() {

        when(mockClient.getDefaultWorkspace()).thenReturn(Mono.just(testData.workspaceB));
        assertSame(testData.workspaceB, workspaceRepository.getDefaultWorkspace().get());
        verify(mockClient, times(1)).getDefaultWorkspace();

        when(mockClient.setDefaultWorkspace(eq(testData.workspaceA.getId())))
                .thenReturn(Mono.just(testData.workspaceA));
        workspaceRepository.setDefaultWorkspace(testData.workspaceA);
        verify(mockClient, times(1)).setDefaultWorkspace(eq(testData.workspaceA.getId()));

        verifyNoMoreInteractions(mockClient);
        assertThrows(
                NullPointerException.class, () -> workspaceRepository.setDefaultWorkspace(null));
    }

    public @Test void namespaceRepository_CRUD() {
        crudTest(namespaceRepository, testData.namespaceA);
    }

    public @Test void namespaceRepository_DefaultNamespace() {
        when(mockClient.getDefaultNamespace()).thenReturn(Mono.just(testData.namespaceB));
        assertSame(testData.namespaceB, namespaceRepository.getDefaultNamespace().get());
        verify(mockClient, times(1)).getDefaultNamespace();

        when(mockClient.setDefaultNamespace(eq(testData.namespaceA.getId())))
                .thenReturn(Mono.just(testData.namespaceA));
        namespaceRepository.setDefaultNamespace(testData.namespaceA);
        verify(mockClient, times(1)).setDefaultNamespace(eq(testData.namespaceA.getId()));

        verifyNoMoreInteractions(mockClient);
        assertThrows(
                NullPointerException.class, () -> namespaceRepository.setDefaultNamespace(null));
    }

    public @Test void storeRepository_CRUD() {
        crudTest(storeRepository, testData.coverageStoreA);
        crudTest(storeRepository, testData.dataStoreA);
        crudTest(storeRepository, testData.wmsStoreA);
        crudTest(storeRepository, testData.wmtsStoreA);
    }

    public @Test void storeRepository_GetDefaultDataStore() {
        when(mockClient.findDefaultDataStoreByWorkspaceId(eq(testData.workspaceA.getId())))
                .thenReturn(Mono.just(testData.dataStoreA));
        when(mockClient.findDefaultDataStoreByWorkspaceId(eq(testData.workspaceB.getId())))
                .thenReturn(Mono.just(testData.dataStoreB));

        assertSame(
                testData.dataStoreA,
                storeRepository.getDefaultDataStore(testData.workspaceA).get());
        assertSame(
                testData.dataStoreB,
                storeRepository.getDefaultDataStore(testData.workspaceB).get());
        assertThrows(NullPointerException.class, () -> storeRepository.getDefaultDataStore(null));
    }

    public @Test void storeRepository_SetDefaultDataStore() {

        WorkspaceInfo newWs = testData.workspaceC;
        DataStoreInfo ds = testData.dataStoreA;

        when(mockClient.setDefaultDataStoreByWorkspaceId(eq(newWs.getId()), eq(ds.getId())))
                .thenReturn(Mono.just(ds));

        storeRepository.setDefaultDataStore(newWs, ds);
        verify(mockClient, times(1))
                .setDefaultDataStoreByWorkspaceId(eq(newWs.getId()), eq(ds.getId()));
        assertThrows(
                NullPointerException.class, () -> storeRepository.setDefaultDataStore(null, ds));
        assertThrows(
                NullPointerException.class, () -> storeRepository.setDefaultDataStore(newWs, null));
        verifyNoMoreInteractions(mockClient);
    }

    public @Test void storeRepository_GetDefaultDataStores_Empty() {
        when(mockClient.getDefaultDataStores()).thenReturn(Flux.empty());
        assertEquals(0L, storeRepository.getDefaultDataStores().count());
        verify(mockClient, times(1)).getDefaultDataStores();
        verifyNoMoreInteractions(mockClient);
    }

    public @Test void storeRepository_GetDefaultDataStores() {
        when(mockClient.getDefaultDataStores())
                .thenReturn(Flux.just(testData.dataStoreA, testData.dataStoreB));
        assertEquals(2L, storeRepository.getDefaultDataStores().count());
        verify(mockClient, times(1)).getDefaultDataStores();
        verifyNoMoreInteractions(mockClient);
    }

    public @Test void resourceRepository_CRUD() {
        crudTest(resourceRepository, testData.coverageA);
        crudTest(resourceRepository, testData.featureTypeA);
        crudTest(resourceRepository, testData.wmsLayerA);
        crudTest(resourceRepository, testData.wmtsLayerA);
    }

    public @Test void layerRepository_CRUD() {
        crudTest(layerRepository, testData.layerFeatureTypeA);
    }

    public @Test void layerGroupRepository_CRUD() {
        crudTest(layerGroupRepository, testData.layerGroup1);
    }

    public @Test void styleRepository_CRUD() {
        crudTest(styleRepository, testData.style1);
    }

    private <T extends CatalogInfo> void crudTest(CatalogInfoRepository<T> repo, T info) {

        assertCreate(repo, info);

        assertFindById(repo, info);
        assertFindByName(repo, info);
        assertFindAll(repo, info);

        assertUpdate(repo, info);

        assertDelete(repo, info);

        verify(mockClient, atMostOnce()).getSupportedFilterFunctionNames();
        verifyNoMoreInteractions(mockClient);
        clearInvocations(mockClient);
    }

    private <T extends CatalogInfo> void assertDelete(CatalogInfoRepository<T> repo, T info) {
        when(mockClient.deleteById(any(String.class), eq(info.getId())))
                .thenReturn(Mono.just(info));
        repo.remove(info);
        verify(mockClient, times(1)).deleteById(any(String.class), eq(info.getId()));
    }

    private <T extends CatalogInfo> void assertUpdate(CatalogInfoRepository<T> repo, T info) {
        Patch patch = new Patch();
        patch.add(new Patch.Property("name", "newName"));

        when(mockClient.update(any(String.class), eq(info.getId()), eq(patch)))
                .thenReturn(Mono.just(info));
        repo.update(info, patch);
        verify(mockClient, times(1)).update(any(String.class), eq(info.getId()), eq(patch));
    }

    private <T extends CatalogInfo> void assertCreate(CatalogInfoRepository<T> repo, T info) {
        when(mockClient.create(any(String.class), same(info))).thenReturn(Mono.just(info));
        repo.add(info);
        verify(mockClient, times(1)).create(any(String.class), same(info));
    }

    private <T extends CatalogInfo> void assertFindById(CatalogInfoRepository<T> repo, T info) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) info.getClass();
        ClassMappings expectedEnumType = ClassMappings.fromImpl(clazz);
        assertNotNull(expectedEnumType);

        final String id = info.getId();
        when(mockClient.findById(any(String.class), eq(id), eq(expectedEnumType)))
                .thenReturn(Mono.just(info));
        Optional<T> retrieved = repo.findById(id, clazz);
        assertTrue(retrieved.isPresent());
        assertSame(info, retrieved.get());
        verify(mockClient, times(1)).findById(any(String.class), eq(id), eq(expectedEnumType));
    }

    private <T extends CatalogInfo> void assertFindByName(CatalogInfoRepository<T> repo, T info) {

        final @NonNull String name = simpleName(info);
        final @NonNull ClassMappings subType = ClassMappings.fromImpl(info.getClass());
        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) subType.getInterface();

        when(mockClient.findFirstByName(any(String.class), eq(name), eq(subType)))
                .thenReturn(Mono.just(info));

        Optional<T> found = repo.findFirstByName(name, type);
        assertTrue(found.isPresent());
        assertSame(info, found.get());
        verify(mockClient, times(1)).findFirstByName(any(String.class), eq(name), eq(subType));
    }

    public @Test void testFindByNameNullType() {
        testFindByNameNullType(workspaceRepository, testData.workspaceA.getName());
        testFindByNameNullType(namespaceRepository, testData.namespaceA.getName());
        testFindByNameNullType(storeRepository, testData.dataStoreA.getName());
        testFindByNameNullType(resourceRepository, testData.coverageA.getName());
        testFindByNameNullType(layerRepository, testData.layerFeatureTypeA.getName());
        testFindByNameNullType(layerGroupRepository, testData.layerGroup1.getName());
        testFindByNameNullType(styleRepository, testData.style1.getName());
    }

    private void testFindByNameNullType(
            @NonNull CatalogInfoRepository<?> repo, @NonNull String name) {
        assertThrows(NullPointerException.class, () -> repo.findFirstByName(name, null));
    }

    private @NonNull String simpleName(CatalogInfo info) {
        return (@NonNull String) OwsUtils.get(info, "name");
    }

    private <T extends CatalogInfo> void assertFindAll(CatalogInfoRepository<T> repo, T info) {

        final @NonNull ClassMappings genericType = genericType(info);

        when(mockClient.findAll(any(String.class), same(genericType)))
                .thenReturn(Flux.just(info, info));
        assertThat(repo.findAll().count(), equalTo(2L));
        verify(mockClient, times(1)).findAll(any(String.class), same(genericType));

        Query<T> query = Query.all(genericType.getInterface());

        when(mockClient.query(any(String.class), same(query))).thenReturn(Flux.just(info));
        assertThat(repo.findAll(query).count(), equalTo(1L));
        verify(mockClient, times(1)).query(any(String.class), same(query));

        final @NonNull ClassMappings subType = ClassMappings.fromImpl(info.getClass());
        Filter someFilter;
        try {
            someFilter = ECQL.toFilter("name = 'test'");
        } catch (CQLException e) {
            throw new RuntimeException();
        }
        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) subType.getInterface();
        query = Query.valueOf(type, someFilter);
        when(mockClient.query(any(String.class), same(query))).thenReturn(Flux.just(info));
        assertThat(repo.findAll(query).count(), equalTo(1L));
        verify(mockClient, times(1)).query(any(String.class), same(query));
    }

    private @NonNull ClassMappings genericType(CatalogInfo info) {
        ClassMappings type = ClassMappings.fromImpl(info.getClass());
        switch (type) {
            case COVERAGESTORE:
            case DATASTORE:
            case WMSSTORE:
            case WMTSSTORE:
                return ClassMappings.STORE;
            case COVERAGE:
            case FEATURETYPE:
            case WMSLAYER:
            case WMTSLAYER:
                return ClassMappings.RESOURCE;
            default:
                return type;
        }
    }
}
