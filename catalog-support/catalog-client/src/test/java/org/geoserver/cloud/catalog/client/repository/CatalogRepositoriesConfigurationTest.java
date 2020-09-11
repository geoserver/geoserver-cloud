package org.geoserver.cloud.catalog.client.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
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
import lombok.NonNull;
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
import org.geoserver.cloud.catalog.client.feign.CatalogApiClient;
import org.geoserver.cloud.catalog.client.feign.LayerClient;
import org.geoserver.cloud.catalog.client.feign.LayerGroupClient;
import org.geoserver.cloud.catalog.client.feign.MapClient;
import org.geoserver.cloud.catalog.client.feign.NamespaceClient;
import org.geoserver.cloud.catalog.client.feign.ResourceClient;
import org.geoserver.cloud.catalog.client.feign.StoreClient;
import org.geoserver.cloud.catalog.client.feign.StyleClient;
import org.geoserver.cloud.catalog.client.feign.WorkspaceClient;
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

@SpringBootTest(classes = CatalogRepositoriesConfiguration.class)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class CatalogRepositoriesConfigurationTest {

    private @MockBean WorkspaceClient workspaceClient;
    private @MockBean NamespaceClient namespaceClient;
    private @MockBean StoreClient storeClient;
    private @MockBean ResourceClient resourceClient;
    private @MockBean LayerClient layerClient;
    private @MockBean LayerGroupClient layerGroupClient;
    private @MockBean StyleClient styleClient;
    private @MockBean MapClient mapClient;

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
        assertWiring(workspaceRepository, workspaceClient, WorkspaceInfo.class);
        crudTest(workspaceRepository, workspaceClient, testData.workspaceA);
    }

    public @Test void workspaceRepository_DefaultWorkspace() {

        when(workspaceClient.getDefault()).thenReturn(testData.workspaceB);
        assertSame(testData.workspaceB, workspaceRepository.getDefaultWorkspace());
        verify(workspaceClient, times(1)).getDefault();

        workspaceRepository.setDefaultWorkspace(testData.workspaceA);
        verify(workspaceClient, times(1)).setDefault(same(testData.workspaceA));

        verifyNoMoreInteractions(workspaceClient);
        assertThrows(
                NullPointerException.class, () -> workspaceRepository.setDefaultWorkspace(null));
    }

    public @Test void namespaceRepository_CRUD() {
        assertWiring(namespaceRepository, namespaceClient, NamespaceInfo.class);
        crudTest(namespaceRepository, namespaceClient, testData.namespaceA);
    }

    public @Test void namespaceRepository_DefaultNamespace() {
        when(namespaceClient.getDefault()).thenReturn(testData.namespaceB);
        assertSame(testData.namespaceB, namespaceRepository.getDefaultNamespace());
        verify(namespaceClient, times(1)).getDefault();

        namespaceRepository.setDefaultNamespace(testData.namespaceA);
        verify(namespaceClient, times(1)).setDefault(same(testData.namespaceA));

        verifyNoMoreInteractions(namespaceClient);
        assertThrows(
                NullPointerException.class, () -> namespaceRepository.setDefaultNamespace(null));
    }

    public @Test void storeRepository_CRUD() {
        assertWiring(storeRepository, storeClient, StoreInfo.class);
        crudTest(storeRepository, storeClient, testData.coverageStoreA);
        crudTest(storeRepository, storeClient, testData.dataStoreA);
        crudTest(storeRepository, storeClient, testData.wmsStoreA);
        crudTest(storeRepository, storeClient, testData.wmtsStoreA);
    }

    // List<DataStoreInfo> getDefaultDataStores();
    // @Nullable
    // <T extends StoreInfo> T findOneByName(@NonNull String name, @Nullable Class<T> clazz);
    // <T extends StoreInfo> List<T> findAllByWorkspace(
    // @NonNull WorkspaceInfo workspace, @Nullable Class<T> clazz);
    // <T extends StoreInfo> List<T> findAllByType(@Nullable Class<T> clazz);
    public @Test void storeRepository_GetDefaultDataStore() {
        when(storeClient.findDefaultDataStoreByWorkspaceId(eq(testData.workspaceA.getName())))
                .thenReturn(testData.dataStoreA);
        when(storeClient.findDefaultDataStoreByWorkspaceId(eq(testData.workspaceB.getName())))
                .thenReturn(testData.dataStoreB);

        assertSame(testData.dataStoreA, storeRepository.getDefaultDataStore(testData.workspaceA));
        assertSame(testData.dataStoreB, storeRepository.getDefaultDataStore(testData.workspaceB));
        assertThrows(NullPointerException.class, () -> storeRepository.getDefaultDataStore(null));
    }

    public @Test void storeRepository_SetDefaultDataStore() {
        storeRepository.setDefaultDataStore(testData.workspaceC, testData.dataStoreA);
        verify(storeClient, times(1))
                .setDefaultDataStoreByWorkspaceId(
                        eq(testData.workspaceC.getName()), eq(testData.dataStoreA.getId()));
        assertThrows(
                NullPointerException.class,
                () -> storeRepository.setDefaultDataStore(null, testData.dataStoreA));
        assertThrows(
                NullPointerException.class,
                () -> storeRepository.setDefaultDataStore(testData.workspaceC, null));
        verifyNoMoreInteractions(storeClient);
    }

    public @Test void storeRepository_GetDefaultDataStores() {
        List<DataStoreInfo> expected = Collections.emptyList();
        when(storeClient.getDefaultDataStores()).thenReturn(expected);
        assertSame(expected, storeRepository.getDefaultDataStores());
        verify(storeClient, times(1)).getDefaultDataStores();
        verifyNoMoreInteractions(storeClient);
    }

    public @Test void resourceRepository_CRUD() {
        assertWiring(resourceRepository, resourceClient, ResourceInfo.class);
        crudTest(resourceRepository, resourceClient, testData.coverageA);
        crudTest(resourceRepository, resourceClient, testData.featureTypeA);
        crudTest(resourceRepository, resourceClient, testData.wmsLayerA);
        crudTest(resourceRepository, resourceClient, testData.wmtsLayerA);
    }

    public @Test void layerRepository_CRUD() {
        assertWiring(layerRepository, layerClient, LayerInfo.class);
        crudTest(layerRepository, layerClient, testData.layerFeatureTypeA);
    }

    public @Test void layerGroupRepository_CRUD() {
        assertWiring(layerGroupRepository, layerGroupClient, LayerGroupInfo.class);
        crudTest(layerGroupRepository, layerGroupClient, testData.layerGroup1);
    }

    public @Test void styleRepository_CRUD() {
        assertWiring(styleRepository, styleClient, StyleInfo.class);
        crudTest(styleRepository, styleClient, testData.style1);
    }

    public @Test void mapRepository_Wiring() {
        assertWiring(mapRepository, mapClient, MapInfo.class);
    }

    private <T extends CatalogInfo> void assertWiring(
            CatalogInfoRepository<T> repository, CatalogApiClient<T> client, Class<T> infoType) {

        assertThat(repository, instanceOf(CatalogServiceClientRepository.class));
        assertEquals(infoType, ((CatalogServiceClientRepository<?, ?>) repository).getInfoType());
        assertSame(client, ((CatalogServiceClientRepository<?, ?>) repository).client());
    }

    private <T extends CatalogInfo> void crudTest(
            CatalogInfoRepository<T> repo, CatalogApiClient<T> mockClient, T info) {

        assertCreate(repo, mockClient, info);

        assertFindById(repo, mockClient, info);
        assertFindByName(repo, mockClient, info);
        assertFindAll(repo, mockClient, info);

        assertUpdate(repo, mockClient, info);

        assertDelete(repo, mockClient, info);

        verifyNoMoreInteractions(mockClient);
        clearInvocations(mockClient);
    }

    private <T extends CatalogInfo> void assertDelete(
            CatalogInfoRepository<T> repo, CatalogApiClient<T> mockClient, T info) {
        repo.remove(info);
        verify(mockClient, times(1)).delete(same(info));
    }

    private <T extends CatalogInfo> void assertUpdate(
            CatalogInfoRepository<T> repo, CatalogApiClient<T> mockClient, T info) {
        repo.update(info);
        verify(mockClient, times(1)).update(same(info));
    }

    private <T extends CatalogInfo> void assertCreate(
            CatalogInfoRepository<T> repo, CatalogApiClient<T> mockClient, T info) {
        repo.add(info);
        verify(mockClient, times(1)).create(same(info));
    }

    private <T extends CatalogInfo> void assertFindById(
            CatalogInfoRepository<T> repo, CatalogApiClient<T> mockClient, T info) {
        when(mockClient.findById(eq(info.getId()), isNull())).thenReturn(info);
        assertSame(info, repo.findById(info.getId(), null));
        verify(mockClient, times(1)).findById(eq(info.getId()), isNull());

        final @NonNull ClassMappings expectedEnumType = ClassMappings.fromImpl(info.getClass());
        final @NonNull Class<T> actualInterfaceArg =
                ClassMappings.fromImpl(info.getClass()).getInterface();

        when(mockClient.findById(eq(info.getId()), eq(expectedEnumType))).thenReturn(info);
        assertSame(info, repo.findById(info.getId(), actualInterfaceArg));
        verify(mockClient, times(1)).findById(eq(info.getId()), same(expectedEnumType));
    }

    private <T extends CatalogInfo> void assertFindByName(
            CatalogInfoRepository<T> repo, CatalogApiClient<T> mockClient, T info) {

        final @NonNull String name = simpleName(info);
        final @NonNull ClassMappings subType = ClassMappings.fromImpl(info.getClass());

        // null subtype
        when(mockClient.findByFirstByName(eq(name), isNull())).thenReturn(info);
        assertSame(info, repo.findFirstByName(name, (Class<T>) null));
        verify(mockClient, times(1)).findByFirstByName(eq(name), isNull());

        // non-null subtype
        when(mockClient.findByFirstByName(eq(name), eq(subType))).thenReturn(info);
        assertSame(info, repo.findFirstByName(name, (Class<T>) info.getClass()));
        verify(mockClient, times(1)).findByFirstByName(eq(name), eq(subType));
    }

    private @NonNull String simpleName(CatalogInfo info) {
        return (@NonNull String) OwsUtils.get(info, "name");
    }

    @SuppressWarnings("unchecked")
    private <T extends CatalogInfo> void assertFindAll(
            CatalogInfoRepository<T> repo, CatalogApiClient<T> mockClient, T info) {

        final @NonNull ClassMappings genericType = genericType(info);

        repo.findAll();
        verify(mockClient, times(1)).query(same(genericType), same(Filter.INCLUDE));

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
