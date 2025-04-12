/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.app;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Auto configuration to log basic application info at {@link ApplicationReadyEvent app startup}
 *
 * <p>Expects the following properties be present in the {@link Environment}: {@literal
 * spring.application.name}, {@literal info.instance-id}.
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@ConditionalOnClass(javax.servlet.Filter.class)
@ConditionalOnProperty(name = "geoserver.debug.instanceId", havingValue = "true", matchIfMissing = false)
public class ServiceIdFilterAutoConfiguration {

    static class ServiceIdFilter implements javax.servlet.Filter {

        @Value("${info.instance-id:}")
        String instanceId;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            if (response instanceof HttpServletResponse httpServletResponse) {
                httpServletResponse.addHeader("X-gs-cloud-service-id", instanceId);
            }
            chain.doFilter(request, response);
        }
    }

    @Bean
    ServiceIdFilter serviceIdFilter() {
        return new ServiceIdFilter();
    }
}
