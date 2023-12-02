/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.server.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geotools.api.filter.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoConfigureWebTestClient(timeout = "360000")
class StyleControllerTest extends AbstractReactiveCatalogControllerTest<StyleInfo> {

    public StyleControllerTest() {
        super(StyleInfo.class);
    }

    protected @Override void assertPropertriesEqual(StyleInfo expected, StyleInfo actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getFilename(), actual.getFilename());
        assertEquals(expected.getFormat(), actual.getFormat());
        assertEquals(expected.getFormatVersion(), actual.getFormatVersion());
        assertEquals(expected.getWorkspace(), actual.getWorkspace());
        assertLegendEquals(expected.getLegend(), actual.getLegend());
    }

    private void assertLegendEquals(LegendInfo expected, LegendInfo actual) {
        if (expected == null || actual == null) assertEquals(expected, actual);
        else {
            assertEquals(expected.getFormat(), actual.getFormat());
            assertEquals(expected.getHeight(), actual.getHeight());
            assertEquals(expected.getOnlineResource(), actual.getOnlineResource());
            assertEquals(expected.getWidth(), actual.getWidth());
        }
    }

    @Test void testStyleCRUD_NoWorkspace() {
        StyleInfo style = testData.createStyle("styleCRUD", null, "styleCRUD", "styleCRUD.sld");
        ((StyleInfoImpl) style).setFormat(SLDHandler.FORMAT);
        ((StyleInfoImpl) style).setFormatVersion(SLDHandler.VERSION_10);
        crudTest(
                style,
                catalog::getStyle,
                created -> {
                    created.setName(created.getName() + "_modified");
                    // this will be assigned by the catalog as the new file name when it renames the
                    // style,
                    // so change it here too for the equals check to pass
                    created.setFilename(created.getName() + ".sld");
                    created.setFormat(SLDHandler.FORMAT);
                    created.setFormatVersion(SLDHandler.VERSION_11);
                    LegendInfoImpl legend = new LegendInfoImpl();
                    legend.setFormat("test");
                    legend.setHeight(10);
                    legend.setWidth(20);
                    legend.setOnlineResource("http://test.com/style.png");
                    created.setLegend(legend);
                },
                (orig, updated) -> {
                    assertEquals(orig.getName() + "_modified", updated.getName());
                    assertEquals(updated.getName() + ".sld", updated.getFilename());
                    assertEquals(SLDHandler.FORMAT, updated.getFormat());
                    assertEquals(SLDHandler.VERSION_11, updated.getFormatVersion());
                    assertEquals("test", updated.getLegend().getFormat());
                    assertEquals(10, updated.getLegend().getHeight());
                    assertEquals(20, updated.getLegend().getWidth());
                    assertEquals(
                            "http://test.com/style.png", updated.getLegend().getOnlineResource());
                });
    }

    @Test void testStyleCRUD_Workspace() {
        StyleInfo style =
                testData.createStyle(
                        "styleCRUD_Workspace",
                        testData.workspaceB,
                        "styleCRUD_Workspace",
                        "styleCRUD_Workspace.sld");

        style.setFormat(SLDHandler.FORMAT);
        style.setFormatVersion(SLDHandler.VERSION_10);

        crudTest(
                style,
                catalog::getStyle,
                created -> {
                    created.setWorkspace(testData.workspaceC);
                    created.setFormatVersion(SLDHandler.VERSION_11);
                },
                (orig, updated) -> {
                    assertEquals(testData.workspaceC, updated.getWorkspace());
                    assertEquals(SLDHandler.VERSION_11, updated.getFormatVersion());
                });
    }

    public @Test @Override void testFindById() {
        testFindById(testData.style1);
        testFindById(testData.style2);
    }

    public @Test @Override void testFindAll() {
        testFindAll(testData.style1, testData.style2);

        WorkspaceInfo ws1 = testData.workspaceA;
        WorkspaceInfo ws2 = testData.workspaceB;
        StyleInfo ws1s1 = testData.createStyle("s1ws1", ws1);
        StyleInfo ws2s1 = testData.createStyle("s1ws2", ws2);
        catalog.add(ws1s1);
        catalog.add(ws2s1);

        testFindAll(testData.style1, testData.style2, ws1s1, ws2s1);
    }

    public @Test @Override void testFindAllByType() {
        testFindAll(StyleInfo.class, testData.style1, testData.style2);
    }

    public @Test @Override void testQueryFilter() {
        WorkspaceInfo ws1 = testData.workspaceA;
        WorkspaceInfo ws2 = testData.workspaceB;

        StyleInfo ws1s1 = testData.createStyle("s1ws1", ws1);
        StyleInfo ws1s2 = testData.createStyle("s2ws1", ws1);
        StyleInfo ws2s1 = testData.createStyle("s1ws2", ws2);
        StyleInfo ws2s2 = testData.createStyle("s2ws2", ws2);
        catalog.add(ws1s1);
        catalog.add(ws1s2);
        catalog.add(ws2s1);
        catalog.add(ws2s2);

        super.testQueryFilter(
                StyleInfo.class,
                Filter.INCLUDE,
                testData.style1,
                testData.style2,
                ws1s1,
                ws1s2,
                ws2s1,
                ws2s2);
        String cql = "name like '%ws1'";
        super.testQueryFilter(cql, ws1s1, ws1s2);
    }

    @Test void testFindStyleByNameAndNullWorkspace() {
        WorkspaceInfo ws1 = testData.workspaceA;
        StyleInfo ws1s1 = testData.createStyle("s1ws1", ws1);
        catalog.add(ws1s1);

        testFindStyleByNameAndNullWorkspace(testData.style1);
        testFindStyleByNameAndNullWorkspace(testData.style2);

        client().getRelative("/styles/noworkspace/{name}", ws1s1.getName())
                .expectStatus()
                .isNoContent();
    }

    private void testFindStyleByNameAndNullWorkspace(StyleInfo style) {
        client().getRelative("/styles/noworkspace/{name}", style.getName())
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody(StyleInfo.class)
                .consumeWith(r -> assertEquals(style.getId(), r.getResponseBody().getId()));
    }

    @Test void testfindStyleByWorkspaceIdAndName() {
        WorkspaceInfo ws1 = testData.workspaceA;
        WorkspaceInfo ws2 = testData.workspaceB;
        StyleInfo ws1s1 = testData.createStyle("s1ws1", ws1);
        StyleInfo ws2s1 = testData.createStyle("s1ws2", ws2);
        catalog.add(ws1s1);
        catalog.add(ws2s1);

        testfindStyleByWorkspaceIdAndName(ws1, ws1s1);
        testfindStyleByWorkspaceIdAndName(ws2, ws2s1);

        client().getRelative(
                        "/workspaces/{workspaceId}/styles/{name}", ws1.getId(), ws2s1.getName())
                .expectStatus()
                .isNoContent();
    }

    private void testfindStyleByWorkspaceIdAndName(WorkspaceInfo ws, StyleInfo style) {
        client().getRelative("/workspaces/{workspaceId}/styles/{name}", ws.getId(), style.getName())
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody(StyleInfo.class)
                .consumeWith(r -> assertEquals(style.getId(), r.getResponseBody().getId()));
    }

    @Test void testFindStylesByNullWorkspace() {
        StyleInfo ws1s1 = testData.createStyle("s1ws1", testData.workspaceA);
        catalog.add(ws1s1);

        List<StyleInfo> expected = Arrays.asList(testData.style1, testData.style2);

        client().getRelative("/styles/noworkspace")
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .expectBodyList(StyleInfo.class)
                .consumeWith(
                        response -> {
                            Set<String> expectedIds =
                                    expected.stream()
                                            .map(StyleInfo::getId)
                                            .collect(Collectors.toSet());
                            Set<String> returnedIds =
                                    response.getResponseBody().stream()
                                            .map(StyleInfo::getId)
                                            .collect(Collectors.toSet());
                            assertEquals(expectedIds, returnedIds);
                        });
    }

    @Test void testFindStylesByWorkspaceId() {
        WorkspaceInfo ws1 = testData.workspaceA;
        WorkspaceInfo ws2 = testData.workspaceB;

        StyleInfo ws1s1 = testData.createStyle("s1ws1", ws1);
        StyleInfo ws1s2 = testData.createStyle("s2ws1", ws1);
        StyleInfo ws2s1 = testData.createStyle("s1ws2", ws2);
        StyleInfo ws2s2 = testData.createStyle("s2ws2", ws2);
        catalog.add(ws1s1);
        catalog.add(ws1s2);
        catalog.add(ws2s1);
        catalog.add(ws2s2);

        testFindStylesByWorkspaceId(ws1, ws1s1, ws1s2);
        testFindStylesByWorkspaceId(ws2, ws2s1, ws2s2);
        testFindStylesByWorkspaceId(testData.workspaceC);
    }

    private void testFindStylesByWorkspaceId(WorkspaceInfo ws1, StyleInfo... expected) {
        client().getRelative("/workspaces/{workspaceId}/styles", ws1.getId())
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .expectBodyList(StyleInfo.class)
                .consumeWith(
                        response -> {
                            Set<String> expectedIds =
                                    Arrays.stream(expected)
                                            .map(StyleInfo::getId)
                                            .collect(Collectors.toSet());
                            Set<String> returnedIds =
                                    response.getResponseBody().stream()
                                            .map(StyleInfo::getId)
                                            .collect(Collectors.toSet());
                            assertEquals(expectedIds, returnedIds);
                        });
    }
}
