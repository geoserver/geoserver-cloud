/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON;

import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.cloud.catalog.app.CatalogServiceApplicationConfiguration;
import org.geoserver.cloud.catalog.test.WebTestClientSupportConfiguration;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wps.WPSInfo;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest(
        classes = {
            CatalogServiceApplicationConfiguration.class,
            WebTestClientSupportConfiguration.class
        })
@ActiveProfiles("test") // see bootstrap-test.yml
@RunWith(SpringRunner.class)
@AutoConfigureWebTestClient(timeout = "360000")
public class ReactiveConfigControllerTest {

    private @Autowired WebTestClient testClient;

    protected @SpyBean @Qualifier("geoServer") GeoServer geoServer;

    protected @Autowired ReactiveConfigController controller;

    private final String baseURI = ReactiveConfigController.BASE_URI;

    private CatalogTestData testData;

    public @Before void before() {
        testData =
                CatalogTestData.empty(() -> geoServer.getCatalog(), () -> geoServer).initialize();
        geoServer.getCatalog().add(testData.workspaceA);
        geoServer.getCatalog().add(testData.workspaceB);
    }

    public @After void after() {
        testData.deleteAll(geoServer.getCatalog());
        WorkspaceInfo workspace = testData.workspaceA;
        SettingsInfo settings = geoServer.getSettings(workspace);
        if (settings != null) geoServer.remove(settings);

        geoServer.getServices().forEach(geoServer::remove);
        geoServer.getServices(workspace).forEach(geoServer::remove);
    }

    // GET /global
    public @Test void getGlobal() {
        GeoServerInfo global = controller.getGlobal().block();
        assertNotNull(global);
        GeoServerInfo responseBody =
                get("/global")
                        .expectStatus()
                        .isOk()
                        .expectBody(GeoServerInfo.class)
                        .returnResult()
                        .getResponseBody();
        assertEquals(global, responseBody);

        Mockito.doReturn(null).when(geoServer).getGlobal();

        assertNull(geoServer.getGlobal());
        get("/global").expectStatus().isNoContent();
    }

    // PUT /global
    public @Test void setGlobal() {
        GeoServerInfo newGlobal = testData.global;
        newGlobal.setAdminPassword("testme");
        newGlobal.setAdminUsername("me");
        newGlobal.getSettings().setCharset("ISO-8859-1");
        newGlobal.getSettings().setTitle("title set through api");

        put(newGlobal, "/global").expectStatus().isOk().expectBody(GeoServerInfo.class);

        GeoServerInfo returned = geoServer.getGlobal();
        assertEquals("testme", returned.getAdminPassword());
        assertEquals("me", returned.getAdminUsername());
        assertEquals(newGlobal.getSettings(), returned.getSettings());
    }

    // GET /workspaces/{workspaceId}/settings
    public @Test void getSettingsByWorkspace() {
        WorkspaceInfo workspace = testData.workspaceA;

        get("/workspaces/{workspaceId}/settings", workspace.getId()).expectStatus().isNoContent();

        SettingsInfo settings = testData.workspaceASettings;
        settings.setWorkspace(workspace);
        geoServer.add(settings);

        final SettingsInfo expected = geoServer.getSettings(workspace);
        get("/workspaces/{workspaceId}/settings", workspace.getId())
                .expectStatus()
                .isOk()
                .expectBody(SettingsInfo.class)
                .consumeWith(result -> assertEquals(expected, result.getResponseBody()));
    }

    // POST /workspaces/{workspaceId}/settings
    public @Test void createSettings() {
        WorkspaceInfo workspace = testData.workspaceA;
        assertNull(geoServer.getSettings(workspace));
        SettingsInfo settings = testData.workspaceASettings;
        settings.setWorkspace(null); // should work even if workspace is not set beforehand
        post(settings, "/workspaces/{workspaceId}/settings", workspace.getId())
                .expectStatus()
                .isCreated();

        assertNotNull(geoServer.getSettings(workspace));
    }

    // PATCH /workspaces/{workspaceId}/settings
    public @Test void updateSettings() {
        WorkspaceInfo workspace = testData.workspaceA;
        SettingsInfo settings = testData.workspaceASettings;
        settings.setWorkspace(workspace);
        geoServer.add(settings);
        settings = geoServer.getSettings(workspace);

        settings.setTitle("new title set through api");
        Patch patch = PropertyDiff.valueOf(ModificationProxy.handler(settings)).toPatch();

        patch(patch, "/workspaces/{workspaceId}/settings", workspace.getId())
                .expectStatus()
                .isOk()
                .expectBody(SettingsInfo.class)
                .value(s -> s.getTitle(), equalTo("new title set through api"));
        assertEquals(settings.getTitle(), geoServer.getSettings(workspace).getTitle());
    }

    // DELETE /workspaces/{workspaceId}/settings
    public @Test void deleteSettings() {
        WorkspaceInfo workspace = testData.workspaceA;
        SettingsInfo settings = testData.workspaceASettings;
        settings.setWorkspace(workspace);
        geoServer.add(settings);
        assertNotNull(geoServer.getSettings(workspace));
        delete("/workspaces/{workspaceId}/settings", workspace.getId())
                .expectStatus()
                .isOk()
                .expectBody()
                .isEmpty();
        assertNull(geoServer.getSettings(workspace));
        delete("/workspaces/{workspaceId}/settings", workspace.getId())
                .expectStatus()
                .isNoContent();
    }

    // get /logging
    public @Test void getLogging() {
        geoServer.setLogging(testData.logging);
        LoggingInfo logging = geoServer.getLogging();
        assertNotNull(logging);
        LoggingInfo responseBody =
                get("/logging")
                        .expectStatus()
                        .isOk()
                        .expectBody(LoggingInfo.class)
                        .returnResult()
                        .getResponseBody();
        assertEquals(logging, responseBody);
    }

    // PUT /logging
    public @Test void setLogging() {
        LoggingInfo logging = testData.logging;
        logging.setLevel("apiLevel");
        logging.setLocation("/right/there");
        logging.setStdOutLogging(false);

        put(logging, "/logging").expectStatus().isOk().expectBody(LoggingInfo.class);
        assertEquals(logging, geoServer.getLogging());
    }

    // POST /services
    public @Test void createService() {
        if (null != geoServer.getService(WMSInfo.class))
            geoServer.remove(geoServer.getService(WMSInfo.class));

        assertNull(geoServer.getService(WMSInfo.class));

        WMSInfo service = testData.wmsService;
        service.setWorkspace(null);
        service.setTitle("created from api");
        post(service, "/services").expectStatus().isCreated().expectBody().isEmpty();
        WMSInfo stored = geoServer.getService(WMSInfo.class);
        assertNotNull(stored);
        assertEquals(service.getTitle(), stored.getTitle());
    }

    // POST /workspaces/{workspaceId}/services
    public @Test void createServiceByWorkspace() {
        WorkspaceInfo workspace = testData.workspaceA;
        if (null != geoServer.getService(workspace, WMSInfo.class))
            geoServer.remove(geoServer.getService(workspace, WMSInfo.class));
        assertNull(geoServer.getService(workspace, WMSInfo.class));

        WMSInfo service = testData.wmsService;
        service.setWorkspace(null); // should work regardless
        service.setTitle("created from api for workspace");
        post(service, "/workspaces/{workspaceId}/services", workspace.getId())
                .expectStatus()
                .isCreated()
                .expectBody()
                .isEmpty();

        WMSInfo stored = geoServer.getService(workspace, WMSInfo.class);
        assertNotNull(stored);
        assertEquals(service.getTitle(), stored.getTitle());
    }

    // DELETE /services/{serviceId}
    public @Test void deleteServiceById() {
        WMSInfo service = testData.wmsService;
        if (null == geoServer.getService(WMSInfo.class)) geoServer.add(service);
        assertNotNull(geoServer.getService(WMSInfo.class));

        delete("/services/{serviceId}", service.getId())
                .expectStatus()
                .isOk()
                .expectBody()
                .isEmpty();

        assertNull(geoServer.getService(WMSInfo.class));
    }

    // GET /services/{serviceId}
    public @Test void getServiceById() {
        WorkspaceInfo ws = testData.workspaceA;
        WMSInfo wmsService = testData.wmsService;
        WCSInfo wcsService = testData.wcsService;
        WFSInfo wfsService = testData.wfsService;
        WPSInfo wpsService = testData.wpsService;

        wfsService.setWorkspace(ws);
        wpsService.setWorkspace(ws);

        geoServer.add(wmsService);
        geoServer.add(wcsService);
        geoServer.add(wfsService);
        geoServer.add(wpsService);

        get("/services/{serviceId}", wmsService.getId())
                .expectStatus()
                .isOk()
                .expectBody(WMSInfo.class)
                .value(s -> s.getId(), Matchers.equalTo(wmsService.getId()));
        get("/services/{serviceId}", wcsService.getId())
                .expectStatus()
                .isOk()
                .expectBody(WCSInfo.class)
                .value(s -> s.getId(), Matchers.equalTo(wcsService.getId()));
        get("/services/{serviceId}", wfsService.getId())
                .expectStatus()
                .isOk()
                .expectBody(WFSInfo.class)
                .value(s -> s.getId(), Matchers.equalTo(wfsService.getId()));
        get("/services/{serviceId}", wpsService.getId())
                .expectStatus()
                .isOk()
                .expectBody(WPSInfo.class)
                .value(s -> s.getId(), Matchers.equalTo(wpsService.getId()));
    }

    // PATCH /services/{id}
    public @Test void updateService() {
        geoServer.add(testData.wmsService);
        geoServer.add(testData.wcsService);
        geoServer.add(testData.wfsService);
        geoServer.add(testData.wpsService);
        WMSInfo wmsService = geoServer.getService(WMSInfo.class);
        WCSInfo wcsService = geoServer.getService(WCSInfo.class);
        WFSInfo wfsService = geoServer.getService(WFSInfo.class);
        WPSInfo wpsService = geoServer.getService(WPSInfo.class);
        testUpdateService(wcsService);
        testUpdateService(wmsService);
        testUpdateService(wfsService);
        testUpdateService(wpsService);
    }

    // PATCH /services/{id}
    public @Test void updateServiceByWorkspace() {
        addServicesToWorkspaceA();
        WorkspaceInfo ws = testData.workspaceA;

        WMSInfo wmsService = geoServer.getService(ws, WMSInfo.class);
        WCSInfo wcsService = geoServer.getService(ws, WCSInfo.class);
        WFSInfo wfsService = geoServer.getService(ws, WFSInfo.class);
        WPSInfo wpsService = geoServer.getService(ws, WPSInfo.class);

        assertEquals(ws.getId(), testUpdateService(wcsService).getWorkspace().getId());
        assertEquals(ws.getId(), testUpdateService(wmsService).getWorkspace().getId());
        assertEquals(ws.getId(), testUpdateService(wfsService).getWorkspace().getId());
        assertEquals(ws.getId(), testUpdateService(wpsService).getWorkspace().getId());
    }

    private <S extends ServiceInfo> S testUpdateService(S service) {
        service.setTitle("title updated");
        MetadataLinkInfoImpl metadataLink = new MetadataLinkInfoImpl();
        metadataLink.setAbout("md about");
        metadataLink.setContent("http://test.com/md-" + service.getId() + ".xml");
        metadataLink.setId(service.getId() + "-md-id");
        metadataLink.setMetadataType("testmd");
        metadataLink.setType("test");
        service.setMetadataLink(metadataLink);

        Patch patch = asPatch(service);
        ServiceInfo returned =
                patch(patch, "/services/{id}", service.getId())
                        .expectStatus()
                        .isOk()
                        .expectBody(ServiceInfo.class)
                        .returnResult()
                        .getResponseBody();

        @SuppressWarnings("unchecked")
        Class<S> origType = (Class<S>) ModificationProxy.unwrap(service).getClass();
        assertThat(returned, instanceOf(origType));
        assertEquals(metadataLink, returned.getMetadataLink());
        assertEquals("title updated", returned.getTitle());
        return origType.cast(returned);
    }

    private Patch asPatch(Info modified) {
        ModificationProxy handler = ModificationProxy.handler(modified);
        Patch patch = PropertyDiff.valueOf(handler).toPatch();
        return patch;
    }

    // GET /workspaces/{workspaceId}/services
    public @Test void getServicesByWorkspace() {
        WorkspaceInfo ws = testData.workspaceA;
        geoServer.getServices(ws).forEach(geoServer::remove);

        get("/workspaces/{workspaceId}/services", ws.getId())
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(APPLICATION_STREAM_JSON)
                .expectStatus()
                .isOk()
                .expectBody()
                .isEmpty();

        testData.wmsService.setWorkspace(ws);
        geoServer.add(testData.wmsService);
        testGetServicesByWorkspace(ws, testData.wmsService);

        testData.wfsService.setWorkspace(ws);
        geoServer.add(testData.wfsService);
        testGetServicesByWorkspace(ws, testData.wmsService, testData.wfsService);

        testData.wcsService.setWorkspace(ws);
        geoServer.add(testData.wcsService);
        testGetServicesByWorkspace(
                ws, testData.wmsService, testData.wfsService, testData.wcsService);

        testData.wpsService.setWorkspace(ws);
        geoServer.add(testData.wpsService);
        testGetServicesByWorkspace(
                ws,
                testData.wmsService,
                testData.wfsService,
                testData.wcsService,
                testData.wpsService);
    }

    private void testGetServicesByWorkspace(WorkspaceInfo ws, ServiceInfo... expected) {

        Set<String> expectedIds =
                Arrays.stream(expected).map(ServiceInfo::getId).collect(Collectors.toSet());

        get("/workspaces/{workspaceId}/services", ws.getId())
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(APPLICATION_STREAM_JSON)
                .expectBodyList(ServiceInfo.class)
                .value(
                        l -> l.stream().map(ServiceInfo::getId).collect(Collectors.toSet()),
                        Matchers.equalTo(expectedIds));
    }

    // GET /services
    public @Test void getGlobalServices() {
        geoServer.getServices().forEach(geoServer::remove);

        get("/services")
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(APPLICATION_STREAM_JSON)
                .expectStatus()
                .isOk()
                .expectBody()
                .isEmpty();

        geoServer.add(testData.wmsService);
        testGetServices(testData.wmsService);

        geoServer.add(testData.wfsService);
        testGetServices(testData.wmsService, testData.wfsService);

        geoServer.add(testData.wcsService);
        testGetServices(testData.wmsService, testData.wfsService, testData.wcsService);

        geoServer.add(testData.wpsService);
        testGetServices(
                testData.wmsService, testData.wfsService, testData.wcsService, testData.wpsService);
    }

    private void testGetServices(ServiceInfo... expected) {

        Set<String> expectedIds =
                Arrays.stream(expected).map(ServiceInfo::getId).collect(Collectors.toSet());

        get("/services")
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(APPLICATION_STREAM_JSON)
                .expectBodyList(ServiceInfo.class)
                .value(
                        l -> l.stream().map(ServiceInfo::getId).collect(Collectors.toSet()),
                        Matchers.equalTo(expectedIds));
    }

    // GET @GetMapping("/services/type/{type}
    public @Test void getGlobalService() {
        testGetGlobalService(testData.wmsService, WMSInfo.class);
        testGetGlobalService(testData.wfsService, WFSInfo.class);
        testGetGlobalService(testData.wcsService, WCSInfo.class);
        testGetGlobalService(testData.wpsService, WPSInfo.class);
    }

    private <S extends ServiceInfo> void testGetGlobalService(S service, Class<S> type) {
        S existing = geoServer.getService(type);
        if (existing != null) geoServer.remove(existing);

        String typeName = type.getName();
        get("/services/type/{type}", typeName).expectStatus().isNoContent();
        geoServer.add(service);
        get("/services/type/{type}", typeName)
                .expectStatus()
                .isOk()
                .expectBody(type)
                .value(s -> s.getId(), equalTo(service.getId()));
    }

    // @GetMapping("/workspaces/{workspaceId}/services/type/{type}")
    public @Test void getServiceByWorkspaceAndType() {
        WorkspaceInfo ws = testData.workspaceA;
        geoServer.getServices(ws).forEach(geoServer::remove);
        testGetServiceByWorkspaceAndType(ws, WMSInfo.class, testData.wmsService);
        testGetServiceByWorkspaceAndType(ws, WFSInfo.class, testData.wfsService);
        testGetServiceByWorkspaceAndType(ws, WCSInfo.class, testData.wcsService);
        testGetServiceByWorkspaceAndType(ws, WPSInfo.class, testData.wpsService);
    }

    private <S extends ServiceInfo> void testGetServiceByWorkspaceAndType(
            WorkspaceInfo ws, Class<S> type, S service) {

        String workspaceId = ws.getId();
        String typeParam = type.getName();

        get("/workspaces/{workspaceId}/services/type/{type}", workspaceId, typeParam)
                .expectStatus()
                .isNoContent()
                .expectHeader()
                .contentType(APPLICATION_JSON);

        service.setWorkspace(ws);
        geoServer.add(service);

        get("/workspaces/{workspaceId}/services/type/{type}", workspaceId, typeParam)
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(APPLICATION_JSON)
                .expectBody(type)
                .value(ServiceInfo::getId, equalTo(service.getId()));
    }

    // GET /services/name/{name}
    public @Test void getGlobalServiceByName() {
        geoServer.getServices().forEach(geoServer::remove);
        testGetGlobalServiceByName(testData.wmsService);
        testGetGlobalServiceByName(testData.wfsService);
        testGetGlobalServiceByName(testData.wcsService);
        testGetGlobalServiceByName(testData.wpsService);
    }

    private void testGetGlobalServiceByName(ServiceInfo service) {

        final String name = service.getName();

        get("/services/name/{name}", name)
                .expectHeader()
                .contentType(APPLICATION_JSON)
                .expectStatus()
                .isNoContent();

        geoServer.add(service);

        get("/services/name/{name}", name)
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(APPLICATION_JSON)
                .expectBody(ServiceInfo.class)
                .value(ServiceInfo::getId, equalTo(service.getId()))
                .value(ServiceInfo::getName, equalTo(service.getName()));

        service = geoServer.getService(service.getId(), ServiceInfo.class);
        service.setName(service.getName() + "-changed");
        geoServer.save(service);

        get("/services/name/{name}", name)
                .expectHeader()
                .contentType(APPLICATION_JSON)
                .expectStatus()
                .isNoContent();
    }

    // GET /workspaces/{workspaceId}/services/name/{name}
    public @Test void getServiceByWorkspaceAndName() {
        addServicesToWorkspaceA();

        testGetServiceByWorkspaceAndName(testData.wmsService);
        testGetServiceByWorkspaceAndName(testData.wfsService);
        testGetServiceByWorkspaceAndName(testData.wcsService);
        testGetServiceByWorkspaceAndName(testData.wpsService);
    }

    protected void addServicesToWorkspaceA() {
        WorkspaceInfo ws = testData.workspaceA;
        geoServer.getServices(ws).forEach(geoServer::remove);

        testData.wmsService.setWorkspace(ws);
        testData.wcsService.setWorkspace(ws);
        testData.wfsService.setWorkspace(ws);
        testData.wpsService.setWorkspace(ws);
        geoServer.add(testData.wmsService);
        geoServer.add(testData.wcsService);
        geoServer.add(testData.wfsService);
        geoServer.add(testData.wpsService);
    }

    private void testGetServiceByWorkspaceAndName(ServiceInfo service) {
        WorkspaceInfo ws = testData.workspaceA;

        get("/workspaces/{workspaceId}/services/name/{name}", ws.getId(), service.getName())
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(APPLICATION_JSON)
                .expectBody(ServiceInfo.class)
                .value(s -> s.getId(), equalTo(service.getId()));

        String emptyWsId = testData.workspaceB.getId();
        get("/workspaces/{workspaceId}/services/name/{name}", emptyWsId, service.getName())
                .expectStatus()
                .isNoContent()
                .expectHeader()
                .contentType(APPLICATION_JSON);
    }

    private <T extends Info> ResponseSpec put(
            Object requestBody, String uri, Object... uriVariables) {
        return testClient
                .put()
                .uri(toAbsoluteURI(uri), uriVariables)
                .bodyValue(requestBody)
                .accept(APPLICATION_JSON, APPLICATION_STREAM_JSON)
                .exchange();
    }

    private <T extends Info> ResponseSpec post(
            Object requestBody, String uri, Object... uriVariables) {
        return testClient
                .post()
                .uri(toAbsoluteURI(uri), uriVariables)
                .bodyValue(requestBody)
                .accept(APPLICATION_JSON, APPLICATION_STREAM_JSON)
                .exchange();
    }

    private <T extends Info> ResponseSpec patch(
            Object requestBody, String uri, Object... uriVariables) {
        return testClient
                .patch()
                .uri(toAbsoluteURI(uri), uriVariables)
                .bodyValue(requestBody)
                .accept(APPLICATION_JSON, APPLICATION_STREAM_JSON)
                .exchange();
    }

    private <T extends Info> ResponseSpec get(String uri, Object... uriVariables) {
        return testClient
                .get()
                .uri(toAbsoluteURI(uri), uriVariables)
                .accept(APPLICATION_JSON, APPLICATION_STREAM_JSON)
                .exchange();
    }

    private <T extends Info> ResponseSpec delete(String uri, Object... uriVariables) {
        return testClient
                .delete()
                .uri(toAbsoluteURI(uri), uriVariables)
                .accept(APPLICATION_JSON, APPLICATION_STREAM_JSON)
                .exchange();
    }

    private String toAbsoluteURI(String relative) {
        return baseURI + relative;
    }
}
