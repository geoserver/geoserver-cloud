/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wms.app;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** See {@code src/test/resources/bootstrap-testjdbcconfig.yml} */
@SpringBootTest(properties = "gwc.wms-integration=true")
@ActiveProfiles({"test", "testjdbcconfig"})
class WmsApplicationJdbcconfigTest extends WmsApplicationTest {}
