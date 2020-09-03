/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

@AutoConfigureWebTestClient(timeout = "360000")
public class StyleControllerTest extends AbstractCatalogInfoControllerTest<StyleInfo> {

    public StyleControllerTest() {
        super(StyleController.BASE_URI, StyleInfo.class);
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

    public @Test void styleCRUD_NoWorkspace() {
        StyleInfo style = testData.createStyle("styleCRUD", null, "styleCRUD", "styleCRUD.sld");
        ((StyleInfoImpl) style).setFormat(SLDHandler.FORMAT);
        ((StyleInfoImpl) style).setFormatVersion(SLDHandler.VERSION_10);
        crudTest(
                style,
                s -> {
                    s.setName(s.getName() + "_modified");
                    // this will be assigned by the catalog as the new file name when it renames the
                    // style,
                    // so change it here too for the equals check to pass
                    s.setFilename(s.getName() + ".sld");
                    s.setFormat(SLDHandler.FORMAT);
                    s.setFormatVersion(SLDHandler.VERSION_11);
                    LegendInfoImpl legend = new LegendInfoImpl();
                    legend.setFormat("test");
                    legend.setHeight(10);
                    legend.setWidth(20);
                    legend.setOnlineResource("http://test.com/style.png");
                    s.setLegend(legend);
                },
                catalog::getStyle);
    }

    public @Test void styleCRUD_Workspace() {
        StyleInfo style =
                testData.createStyle(
                        "styleCRUD_Workspace",
                        testData.workspaceB,
                        "styleCRUD_Workspace",
                        "styleCRUD_Workspace.sld");

        crudTest(
                style,
                s -> {
                    s.setWorkspace(testData.workspaceC);
                },
                catalog::getStyle);
    }
}
