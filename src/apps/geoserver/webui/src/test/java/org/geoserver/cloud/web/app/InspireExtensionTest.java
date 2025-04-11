/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.web.app;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.wicket.markup.html.form.CheckBox;
import org.geoserver.gwc.web.wmts.WMTSAdminPage;
import org.geoserver.wcs.web.WCSAdminPage;
import org.geoserver.wfs.web.WFSAdminPage;
import org.geoserver.wms.web.WMSAdminPage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {"geoserver.extension.inspire.enabled: true"})
@ActiveProfiles("test") // see bootstrap-test.yml
class InspireExtensionTest extends WebUIApplicationTest {

    private static final String CHECKBOX_PATH = "form:extensions:0:content:createExtendedCapabilities";

    @Test
    void Inspire_AdminPanels_enabled_test() {
        login();
        tester.startPage(WMSAdminPage.class);
        tester.assertRenderedPage(WMSAdminPage.class);
        tester.assertComponent(CHECKBOX_PATH, CheckBox.class);

        tester.startPage(WFSAdminPage.class);
        tester.assertRenderedPage(WFSAdminPage.class);
        tester.assertComponent(CHECKBOX_PATH, CheckBox.class);

        tester.startPage(WCSAdminPage.class);
        tester.assertRenderedPage(WCSAdminPage.class);
        tester.assertComponent(CHECKBOX_PATH, CheckBox.class);

        tester.startPage(WMTSAdminPage.class);
        tester.assertRenderedPage(WMTSAdminPage.class);
        tester.assertComponent(CHECKBOX_PATH, CheckBox.class);
    }

    @Test
    void testExpectedBeansFromWebUiApplicationInspireExtensionAutoConfiguration() {
        assertNotNull(app.getBean("inspireWmsAdminPanel"));
        assertNotNull(app.getBean("inspireWfsAdminPanel"));
        assertNotNull(app.getBean("inspireWcsAdminPanel"));
        assertNotNull(app.getBean("inspireWmtsAdminPanel"));
        assertNotNull(app.getBean("inspireExtension"));
        assertNotNull(app.getBean("languageCallback"));
        assertNotNull(app.getBean("inspireDirManager"));
    }
}
