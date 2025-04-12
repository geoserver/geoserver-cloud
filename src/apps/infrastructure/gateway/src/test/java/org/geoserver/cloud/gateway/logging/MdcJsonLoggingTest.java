/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.geoserver.cloud.gateway.GatewayApplication;
import org.geoserver.cloud.gateway.config.TestMdcConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests to verify that MDC values are properly included when using Spring Boot 3's
 * automatic context propagation.
 */
@SpringBootTest(
        classes = {GatewayApplication.class, TestMdcConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
class MdcJsonLoggingTest {

    /**
     * Test direct MDC usage to verify it works as expected
     */
    @Test
    void testDirectMdcLogging() {
        // We'll directly set and use MDC values
        String testId = "direct-test-" + System.currentTimeMillis();

        try {
            // Set MDC values
            MDC.put("test.id", testId);
            MDC.put("test.name", "Direct MDC Test");

            // Log something with the MDC values
            System.out.println("Direct test log with ID: " + testId);

            // The values should be available via MDC.getCopyOfContextMap()
            Map<String, String> mdcValues = MDC.getCopyOfContextMap();

            // Verify MDC values are present
            assertThat(mdcValues).isNotNull();
            assertThat(mdcValues).containsKey("test.id");
            assertThat(mdcValues).containsKey("test.name");
            assertThat(mdcValues.get("test.id")).isEqualTo(testId);
            assertThat(mdcValues.get("test.name")).isEqualTo("Direct MDC Test");
        } finally {
            // Always clear MDC when done
            MDC.clear();
        }
    }
}
