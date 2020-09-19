/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import org.geoserver.cloud.test.CatalogTestData;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootTest(classes = CatalogRepositoriesConfiguration.class)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class CatalogRepositoriesConfigurationTest {

    private @MockBean ReactiveCatalogClient mockClient;

    private @Autowired WorkspaceRepository workspaceRepository;
    private @Autowired NamespaceRepository namespaceRepository;
    private @Autowired StoreRepository storeRepository;
    private @Autowired ResourceRepository resourceRepository;
    private @Autowired LayerRepository layerRepository;
    private @Autowired LayerGroupRepository layerGroupRepository;
    private @Autowired StyleRepository styleRepository;
    private @Autowired MapRepository mapRepository;

    private static final Catalog fakeCatalog = new CatalogImpl();
    public @Rule CatalogTestData testData = CatalogTestData.empty(() -> fakeCatalog);

    public @Test void workspaceRepository_CRUD() {
        assertWiring(workspaceRepository, WorkspaceInfo.class);
        crudTest(workspaceRepository, testData.workspaceA);
    }

    public @Test void workspaceRepository_DefaultWorkspace() {

        when(mockClient.getDefaultWorkspace()).thenReturn(Mono.just(testData.workspaceB));
        assertSame(testData.workspaceB, workspaceRepository.getDefaultWorkspace());
        verify(mockClient, times(1)).getDefaultWorkspace();

        workspaceRepository.setDefaultWorkspace(testData.workspaceA);
        verify(mockClient, times(1)).setDefaultWorkspace(eq(testData.workspaceA.getId()));

        verifyNoMoreInteractions(mockClient);
        assertThrows(NullPointerException.class,
                () -> workspaceRepository.setDefaultWorkspace(null));
    }

    public @Test void namespaceRepository_CRUD() {
        assertWiring(namespaceRepository, NamespaceInfo.class);
        crudTest(namespaceRepository, testData.namespaceA);
    }

    public @Test void namespaceRepository_DefaultNamespace() {
        when(mockClient.getDefaultNamespace()).thenReturn(Mono.just(testData.namespaceB));
        assertSame(testData.namespaceB, namespaceRepository.getDefaultNamespace());
        verify(mockClient, times(1)).getDefaultNamespace();

        namespaceRepository.setDefaultNamespace(testData.namespaceA);
        verify(mockClient, times(1)).setDefaultNamespace(eq(testData.namespaceA.getId()));

        verifyNoMoreInteractions(mockClient);
        assertThrows(NullPointerException.class,
                () -> namespaceRepository.setDefaultNamespace(null));
    }

    public @Test void storeRepository_CRUD() {
        assertWiring(storeRepository, StoreInfo.class);
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

        assertSame(testData.dataStoreA, storeRepository.getDefaultDataStore(testData.workspaceA));
        assertSame(testData.dataStoreB, storeRepository.getDefaultDataStore(testData.workspaceB));
        assertThrows(NullPointerException.class, () -> storeRepository.getDefaultDataStore(null));
    }

    public @Test void storeRepository_SetDefaultDataStore() {
        storeRepository.setDefaultDataStore(testData.workspaceC, testData.dataStoreA);
        verify(mockClient, times(1)).setDefaultDataStoreByWorkspaceId(
                eq(testData.workspaceC.getId()), eq(testData.dataStoreA.getId()));
        assertThrows(NullPointerException.class,
                () -> storeRepository.setDefaultDataStore(null, testData.dataStoreA));
        assertThrows(NullPointerException.class,
                () -> storeRepository.setDefaultDataStore(testData.workspaceC, null));
        verifyNoMoreInteractions(mockClient);
    }

    public @Test void storeRepository_GetDefaultDataStores() {
        List<DataStoreInfo> expected = Collections.emptyList();
        when(mockClient.getDefaultDataStores()).thenReturn(Flux.empty());
        assertSame(expected, storeRepository.getDefaultDataStores());
        verify(mockClient, times(1)).getDefaultDataStores();
        verifyNoMoreInteractions(mockClient);
    }

    public @Test void resourceRepository_CRUD() {
        assertWiring(resourceRepository, ResourceInfo.class);
        crudTest(resourceRepository, testData.coverageA);
        crudTest(resourceRepository, testData.featureTypeA);
        crudTest(resourceRepository, testData.wmsLayerA);
        crudTest(resourceRepository, testData.wmtsLayerA);
    }

    public @Test void layerRepository_CRUD() {
        assertWiring(layerRepository, LayerInfo.class);
        crudTest(layerRepository, testData.layerFeatureTypeA);
    }

    public @Test void layerGroupRepository_CRUD() {
        assertWiring(layerGroupRepository, LayerGroupInfo.class);
        crudTest(layerGroupRepository, testData.layerGroup1);
    }

    public @Test void styleRepository_CRUD() {
        assertWiring(styleRepository, StyleInfo.class);
        crudTest(styleRepository, testData.style1);
    }

    public @Test void mapRepository_Wiring() {
        assertWiring(mapRepository, MapInfo.class);
    }

    private <T extends CatalogInfo> void assertWiring(CatalogInfoRepository<T> repository,
            Class<T> infoType) {

        assertThat(repository, instanceOf(CatalogServiceClientRepository.class));
        assertEquals(infoType, ((CatalogServiceClientRepository<?>) repository).getInfoType());
        assertSame(mockClient, ((CatalogServiceClientRepository<?>) repository).client());
    }

    private <T extends CatalogInfo> void crudTest(CatalogInfoRepository<T> repo, T info) {

        assertCreate(repo, info);

        assertFindById(repo, info);
        assertFindByName(repo, info);
        assertFindAll(repo, info);

        assertUpdate(repo, info);

        assertDelete(repo, info);

        verifyNoMoreInteractions(mockClient);
        clearInvocations(mockClient);
    }

    private <T extends CatalogInfo> void assertDelete(CatalogInfoRepository<T> repo, T info) {
        repo.remove(info);
        verify(mockClient, times(1)).delete(same(info));
    }

    private <T extends CatalogInfo> void assertUpdate(CatalogInfoRepository<T> repo, T info) {
        repo.update(info);
        verify(mockClient, times(1)).update(same(info));
    }

    private <T extends CatalogInfo> void assertCreate(CatalogInfoRepository<T> repo, T info) {
        repo.add(info);
        verify(mockClient, times(1)).create(same(info));
    }

    private <T extends CatalogInfo> void assertFindById(CatalogInfoRepository<T> repo, T info) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) info.getClass();
        ClassMappings classMappings = ClassMappings.fromImpl(clazz);
        assertNotNull(classMappings);

        final String id = info.getId();
        when(mockClient.findById(eq(id), same(classMappings))).thenReturn(Mono.just(info));
        assertSame(info, repo.findById(id, clazz));
        verify(mockClient, times(1)).findById(eq(id), isNull());

        final @NonNull ClassMappings expectedEnumType = ClassMappings.fromImpl(info.getClass());
        final @NonNull Class<T> actualInterfaceArg =
                ClassMappings.fromImpl(info.getClass()).getInterface();

        when(mockClient.findById(eq(id), eq(expectedEnumType))).thenReturn(Mono.just(info));
        assertSame(info, repo.findById(id, actualInterfaceArg));
        verify(mockClient, times(1)).findById(eq(id), same(expectedEnumType));
    }

    private <T extends CatalogInfo> void assertFindByName(CatalogInfoRepository<T> repo, T info) {

        final @NonNull String name = simpleName(info);
        final @NonNull ClassMappings subType = ClassMappings.fromImpl(info.getClass());

        // null subtype
        when(mockClient.findByFirstByName(eq(name), isNull())).thenReturn(Mono.just(info));
        assertSame(info, repo.findFirstByName(name, (Class<T>) null));
        verify(mockClient, times(1)).findByFirstByName(eq(name), isNull());

        // non-null subtype
        when(mockClient.findByFirstByName(eq(name), eq(subType))).thenReturn(Mono.just(info));
        assertSame(info, repo.findFirstByName(name, (Class<T>) info.getClass()));
        verify(mockClient, times(1)).findByFirstByName(eq(name), eq(subType));
    }

    private @NonNull String simpleName(CatalogInfo info) {
        return (@NonNull String) OwsUtils.get(info, "name");
    }

    @SuppressWarnings("unchecked")
    private <T extends CatalogInfo> void assertFindAll(CatalogInfoRepository<T> repo, T info) {

        final @NonNull ClassMappings genericType = genericType(info);

        repo.findAll();
        verify(mockClient, times(1)).findAll(same(genericType));

        repo.findAll(Filter.EXCLUDE);
        verify(mockClient, times(1)).query(same(genericType), same(Filter.EXCLUDE));

        final @NonNull ClassMappings subType = ClassMappings.fromImpl(info.getClass());
        Filter someFilter;
        try {
            someFilter = ECQL.toFilter("name = 'test'");
        } catch (CQLException e) {
            throw new RuntimeException();
        }
        repo.findAll(someFilter, (Class<T>) info.getClass());
        verify(mockClient, times(1)).query(same(subType), same(someFilter));
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
