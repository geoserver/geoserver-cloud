/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.proxy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.geoserver.cloud.gateway.app.GatewayMvcApplication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.gateway.server.mvc.handler.ProxyExchange;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseExtractor;

/**
 * Verifies the gateway proxies backend responses with a {@code Content-Type} rejected by Spring and forwards the header
 * verbatim.
 *
 * <p>Responses are read with a raw {@link ResponseExtractor}: the convenience methods of {@link TestRestTemplate} parse
 * {@code Content-Type} on the client side and fail on the same media types.
 */
@SpringBootTest(classes = GatewayMvcApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@WireMockTest
class LenientContentTypeProxyExchangeGatewayTest {

    private static final String BACKEND_PATH = "/wfs?service=WFS&version=1.1.0&request=DescribeFeatureType";

    private static final String BACKEND_BODY = "<xsd:schema/>";

    /** saved in {@link #saveWireMock}, to be used on {@link #registerRoutes} */
    private static WireMockRuntimeInfo wmRuntimeInfo;

    @BeforeAll
    static void saveWireMock(WireMockRuntimeInfo runtimeInfo) {
        LenientContentTypeProxyExchangeGatewayTest.wmRuntimeInfo = runtimeInfo;
    }

    /** Route the backend path to the wiremock server; the error dispatch to {@code /error} stays with Spring Boot */
    @DynamicPropertySource
    static void registerRoutes(DynamicPropertyRegistry registry) {
        String targetUrl = wmRuntimeInfo.getHttpBaseUrl();
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].id", () -> "wiremock");
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].uri", () -> targetUrl);
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].predicates[0]", () -> "Path=/wfs");
    }

    @Autowired
    TestRestTemplate testRestTemplate;

    @Test
    void replacesTheDefaultProxyExchange(@Autowired ApplicationContext context) {
        Map<String, ProxyExchange> proxyExchanges = context.getBeansOfType(ProxyExchange.class);

        assertThat(proxyExchanges.values()).singleElement().isInstanceOf(LenientContentTypeProxyExchange.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "text/xml; subtype=gml/3.1.1",
                "text/xml; subtype=gml/2.1.2",
                "text/xml; subtype=wfs-collection/1.0",
                "application/vnd.ogc.gml/3.1.1"
            })
    void proxiesResponseWithContentTypeSpringCannotParse(String contentType) {
        stubBackendResponse(contentType);

        ProxiedResponse response = requestThroughGateway(BACKEND_PATH);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(contentType);
        assertThat(response.body()).isEqualTo(BACKEND_BODY);
    }

    @Test
    void proxiesResponseWithWellFormedContentType() {
        String contentType = "application/gml+xml; version=3.2";
        stubBackendResponse(contentType);

        ProxiedResponse response = requestThroughGateway(BACKEND_PATH);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(contentType);
        assertThat(response.body()).isEqualTo(BACKEND_BODY);
    }

    private void stubBackendResponse(String contentType) {
        ResponseDefinitionBuilder backendResponse = aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, contentType)
                .withBody(BACKEND_BODY);
        MappingBuilder stub = get(urlEqualTo(BACKEND_PATH)).willReturn(backendResponse);
        wmRuntimeInfo.getWireMock().register(stub);
    }

    private ProxiedResponse requestThroughGateway(String pathAndQuery) {
        String url = testRestTemplate.getRootUri() + pathAndQuery;
        return testRestTemplate.getRestTemplate().execute(url, HttpMethod.GET, null, response -> {
            int status = response.getStatusCode().value();
            String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
            return new ProxiedResponse(status, contentType, body);
        });
    }

    private record ProxiedResponse(int status, String contentType, String body) {}
}
