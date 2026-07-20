/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Regression test for the gateway failing to start when {@code geoserver.base-path} is empty.
 *
 * <p>The shipped gateway config expands {@code ${basepath}} into every route, producing the shortcut
 * {@code StripBasePath=} with no arguments when the base path is empty. Spring Cloud Gateway Server MVC resolves
 * shortcut filters by reflection, matching methods by argument count; without a zero-argument {@code stripBasePath()}
 * overload the context fails with "Unable to find operation ... for stripBasePath with args {}".
 *
 * <p>The route properties below replicate the shapes used in the shipped {@code gateway.yml}: the root redirect route
 * and a proxy route with {@code StripBasePath=${basepath}}.
 *
 * @since 3.1.0
 */
@SpringBootTest(
        classes = GatewayMvcApplication.class,
        properties = {
            "geoserver.base-path=",
            "basepath=${geoserver.base-path}",
            "spring.cloud.gateway.server.webmvc.routes[0].id=root-redirect-to-webui",
            "spring.cloud.gateway.server.webmvc.routes[0].uri=no://op",
            "spring.cloud.gateway.server.webmvc.routes[0].predicates[0]=Path=/,${basepath},${basepath}/",
            "spring.cloud.gateway.server.webmvc.routes[0].filters[0]=RedirectTo=302, ${basepath}/web/",
            "spring.cloud.gateway.server.webmvc.routes[1].id=echo",
            "spring.cloud.gateway.server.webmvc.routes[1].uri=http://localhost:1",
            "spring.cloud.gateway.server.webmvc.routes[1].predicates[0]=Path=${basepath}/echo/**",
            "spring.cloud.gateway.server.webmvc.routes[1].filters[0]=StripBasePath=${basepath}",
            "spring.cloud.gateway.server.webmvc.routes[1].filters[1]=SecureHeaders"
        })
@ActiveProfiles("test")
class GatewayMvcEmptyBasePathTest {

    @Autowired
    Environment environment;

    @Test
    void contextStarts_withEmptyBasePath() {
        assertThat(environment.getProperty("basepath")).isEmpty();
    }
}
