package org.geowebcache;

import static org.assertj.core.api.Assertions.assertThat;

import org.geowebcache.config.ServerConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

public class GSCloudGeoWebCacheDispatcherTest {

    private final GSCloudGeoWebCacheDispatcher toTest =
            new GSCloudGeoWebCacheDispatcher(
                    null, null, null, null, Mockito.mock(ServerConfiguration.class), null);

    private final GeoWebCacheDispatcher regularDispatcher =
            new GSCloudGeoWebCacheDispatcher(
                    null, null, null, null, Mockito.mock(ServerConfiguration.class), null);

    public @Test void testNormalizeUrl() {
        List<String> testedPaths =
                List.of(
                        "/gwc/demo/flup:top",
                        "/flup/gwc/demo/top",
                        "/path/flup/gwc/demo/top",
                        "/path/flup/gwc/demo/flup:top",
                        "/gwc/home",
                        "/gwc/service/aaa",
                        "/some/path/ns/gwc/service/aaa",
                        "/",
                        "",
                        "/flup/gwc/home",
                        "/flup/gwc/service",
                        "/ne/gwc/service/wmts",
                        "/ne/gwc/demo",
                        "/ne/gwc/demo/boundary_lines");
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        List results =
                testedPaths.stream()
                        .map(
                                i -> {
                                    Mockito.when(request.getRequestURI()).thenReturn(i);
                                    return toTest.normalizeURL(request);
                                })
                        .collect(Collectors.toList());

        assertThat(results)
                .isEqualTo(
                        List.of(
                                "/demo/flup:top",
                                "/demo/flup:top",
                                "/demo/flup:top",
                                "/demo/flup:top",
                                "/home",
                                "/service/aaa",
                                "/service/aaa",
                                "/",
                                "/",
                                "/home",
                                "/service",
                                "/service/wmts",
                                "/demo",
                                "/demo/ne:boundary_lines"));
    }
}
