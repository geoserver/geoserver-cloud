/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class DiscoveryApplicationTests {

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void getApps(@LocalServerPort int servicePort) throws JSONException {
        String url = "http://localhost:%d/eureka/apps".formatted(servicePort);
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JSONAssert.assertEquals(
                """
                {
                  "applications": {
                    "versions__delta": "1",
                    "apps__hashcode": "",
                    "application": []
                  }
                }
                """,
                response.getBody(),
                false);
    }
}
