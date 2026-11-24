/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.ogcapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * OGC API Features requests shared by the role combinations that run the extension, each hitting a different Spring MVC
 * owner. The OGC API dispatcher serves both documents through the handler adapter and message converters it builds for
 * itself; answering them proves the extension works without owning the context's Spring MVC infrastructure.
 */
final class OgcApiFeaturesRequests {

    private static final String LANDING_PAGE = "/ogc/features/v1?f=application/json";
    private static final String CONFORMANCE = "/ogc/features/v1/conformance?f=application/json";

    private OgcApiFeaturesRequests() {
        // static assertions only
    }

    static void assertLandingPageAnswersJson(TestRestTemplate restTemplate) {
        JsonObject landingPage = getJsonObject(restTemplate, LANDING_PAGE);
        assertThat(landingPage.has("links")).as("landing page: %s", landingPage).isTrue();
    }

    static void assertConformanceAnswersJson(TestRestTemplate restTemplate) {
        JsonObject conformance = getJsonObject(restTemplate, CONFORMANCE);
        assertThat(conformance.has("conformsTo"))
                .as("conformance: %s", conformance)
                .isTrue();
    }

    private static JsonObject getJsonObject(TestRestTemplate restTemplate, String uri) {
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        assertThat(response.getStatusCode())
                .as("%s -> %s", uri, response.getBody())
                .isEqualTo(OK);

        MediaType contentType = response.getHeaders().getContentType();
        assertThat(contentType).as("%s content type", uri).isNotNull();
        assertThat(contentType.isCompatibleWith(APPLICATION_JSON))
                .as("%s: expected %s got %s", uri, APPLICATION_JSON, contentType)
                .isTrue();

        return JsonParser.parseString(response.getBody()).getAsJsonObject();
    }
}
