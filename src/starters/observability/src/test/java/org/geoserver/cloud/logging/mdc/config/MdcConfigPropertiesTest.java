/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.logging.mdc.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Tests for the MDC configuration properties classes.
 * <p>
 * This test class covers the configuration properties classes that control MDC behavior:
 * <ul>
 *   <li>{@link HttpRequestMdcConfigProperties}</li>
 *   <li>{@link SpringEnvironmentMdcConfigProperties}</li>
 *   <li>{@link AuthenticationMdcConfigProperties}</li>
 *   <li>{@link GeoServerMdcConfigProperties}</li>
 * </ul>
 */
class MdcConfigPropertiesTest {

    @BeforeEach
    void setup() {
        // Clear MDC before each test
        MDC.clear();
    }

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void testHttpRequestMdcConfigProperties() {
        // Create config and enable properties
        HttpRequestMdcConfigProperties config = new HttpRequestMdcConfigProperties();
        config.setMethod(true);
        config.setUrl(true);
        config.setQueryString(true);
        config.setRemoteAddr(true);
        config.setRemoteHost(true);
        config.setSessionId(true);
        config.setId(true);
        config.setHeaders(true);
        config.setHeadersPattern(java.util.regex.Pattern.compile(".*"));
        config.setCookies(true);
        config.setParameters(true);

        // Create sample values
        Supplier<String> method = () -> "GET";
        Supplier<String> url = () -> "/test-path";
        Supplier<String> queryString = () -> "param1=value1&param2=value2";
        Supplier<String> remoteAddr = () -> "127.0.0.1";
        Supplier<String> remoteHost = () -> "localhost";
        Supplier<String> sessionId = () -> "test-session-id";

        // Create headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0");
        headers.add("Accept", "application/json");
        Supplier<HttpHeaders> headersSupplier = () -> headers;

        // Create cookies
        MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
        cookies.add("test-cookie", new HttpCookie("test-cookie", "cookie-value"));
        Supplier<MultiValueMap<String, HttpCookie>> cookiesSupplier = () -> cookies;

        // Create parameters
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("param1", "value1");
        parameters.add("param2", "value2");
        Supplier<MultiValueMap<String, String>> parametersSupplier = () -> parameters;

        // Apply configuration
        config.method(method)
                .url(url)
                .queryString(queryString)
                .remoteAddr(remoteAddr)
                .remoteHost(remoteHost)
                .sessionId(sessionId)
                .id(headersSupplier)
                .headers(headersSupplier)
                .cookies(cookiesSupplier)
                .parameters(parametersSupplier);

        // Verify MDC properties
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        assertThat(mdcMap)
                .isNotNull()
                .containsEntry("http.request.method", "GET")
                .containsEntry("http.request.url", "/test-path")
                .containsEntry("http.request.query-string", "param1=value1&param2=value2")
                .containsEntry("http.request.remote-addr", "127.0.0.1")
                .containsEntry("http.request.remote-host", "localhost")
                .containsEntry("http.request.session.id", "test-session-id")
                .containsKey("http.request.id")
                .containsKey("http.request.header.User-Agent")
                .containsKey("http.request.header.Accept")
                .containsKey("http.request.cookie.test-cookie")
                .containsKey("http.request.parameter.param1")
                .containsKey("http.request.parameter.param2");
    }

    @Test
    void testSpringEnvironmentMdcConfigProperties() {
        // Create config and enable properties
        SpringEnvironmentMdcConfigProperties config = new SpringEnvironmentMdcConfigProperties();
        config.setName(true);
        config.setVersion(true);
        config.setInstanceId(true);
        config.setActiveProfiles(true);

        // Mock Environment and BuildProperties
        Environment env = mock(Environment.class);
        when(env.getProperty("spring.application.name")).thenReturn("test-application");
        when(env.getProperty("info.instance-id")).thenReturn("test-instance-1");
        when(env.getActiveProfiles()).thenReturn(new String[] {"test", "dev"});

        BuildProperties buildProps = mock(BuildProperties.class);
        when(buildProps.getVersion()).thenReturn("1.0.0");
        Optional<BuildProperties> optionalBuildProps = Optional.of(buildProps);

        // Apply configuration
        config.addEnvironmentProperties(env, optionalBuildProps);

        // Verify MDC properties
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        assertThat(mdcMap)
                .isNotNull()
                .containsEntry("application.name", "test-application")
                .containsEntry("application.version", "1.0.0")
                .containsEntry("application.instance.id", "test-instance-1")
                .containsEntry("spring.profiles.active", "test,dev");
    }

    @Test
    void testSpringEnvironmentMdcConfigPropertiesWithoutBuildProperties() {
        // Create config and enable properties
        SpringEnvironmentMdcConfigProperties config = new SpringEnvironmentMdcConfigProperties();
        config.setName(true);
        config.setVersion(true);

        // Mock Environment without BuildProperties
        Environment env = mock(Environment.class);
        when(env.getProperty("spring.application.name")).thenReturn("test-application");
        Optional<BuildProperties> emptyBuildProps = Optional.empty();

        // Apply configuration
        config.addEnvironmentProperties(env, emptyBuildProps);

        // Verify MDC properties
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        assertThat(mdcMap)
                .isNotNull()
                .containsEntry("application.name", "test-application")
                .doesNotContainKey("application.version");
    }

    @Test
    void testAuthenticationMdcConfigProperties() {
        // Create config
        AuthenticationMdcConfigProperties config = new AuthenticationMdcConfigProperties();

        // Verify default values
        assertThat(config.isId()).isFalse();
        assertThat(config.isRoles()).isFalse();

        // Enable properties
        config.setId(true);
        config.setRoles(true);

        // Verify updated values
        assertThat(config.isId()).isTrue();
        assertThat(config.isRoles()).isTrue();
    }

    @Test
    void testGeoServerMdcConfigProperties() {
        // Create config
        GeoServerMdcConfigProperties config = new GeoServerMdcConfigProperties();
        GeoServerMdcConfigProperties.OWSMdcConfigProperties owsConfig = config.getOws();

        // Verify default values - these are all true by default in the class
        assertThat(owsConfig.isServiceName()).isTrue();
        assertThat(owsConfig.isServiceVersion()).isTrue();
        assertThat(owsConfig.isServiceFormat()).isTrue();
        assertThat(owsConfig.isOperationName()).isTrue();

        // Enable properties
        owsConfig.setServiceName(true);
        owsConfig.setServiceVersion(true);
        owsConfig.setServiceFormat(true);
        owsConfig.setOperationName(true);

        // Verify updated values
        assertThat(owsConfig.isServiceName()).isTrue();
        assertThat(owsConfig.isServiceVersion()).isTrue();
        assertThat(owsConfig.isServiceFormat()).isTrue();
        assertThat(owsConfig.isOperationName()).isTrue();
    }
}
