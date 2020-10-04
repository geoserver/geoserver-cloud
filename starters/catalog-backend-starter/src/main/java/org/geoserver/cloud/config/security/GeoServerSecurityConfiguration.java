/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.security;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityEnabled;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Loads geoserver security bean definitions from {@code
 * classpath*:/applicationSecurityContext.xml}.
 *
 * <p>Note that if spring boot auto-configuration is enabled, at the very least {@link
 * SecurityAutoConfiguration} or {@link UserDetailsServiceAutoConfiguration} must be disabled, for
 * example using the following annotation in a {@link Configuration @Configuration} class:
 *
 * <pre>{@code
 * &#64;EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
 * }</pre>
 *
 * <p>The {@code geoserver.security.enabled=false} config property can be used as a flag to disable
 * this configuration. Defaults to {@code true}.
 */
@Configuration
@ImportResource(
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = {"classpath*:/applicationSecurityContext.xml"}
)
@Slf4j
@ConditionalOnGeoServerSecurityEnabled
public class GeoServerSecurityConfiguration {

    private @Value("${geoserver.security.enabled:#{null}}") Boolean enabled;

    public @PostConstruct void log() {
        log.info(
                "GeoServer security being configured through classpath*:/applicationSecurityContext.xml");
    }

    //    @Bean(name = "accessRulesDao")
    //    @DependsOn({"extensions", "rawCatalog"})
    //    public DataAccessRuleDAO accessRulesDao(
    //            GeoServerDataDirectory dataDirectory, @Qualifier("rawCatalog") Catalog rawCatalog)
    //            throws Exception {
    //        return new DataAccessRuleDAO(dataDirectory, rawCatalog);
    //    }

    // // <bean id="authenticationManager" class="org.geoserver.security.GeoServerSecurityManager"
    // // depends-on="extensions">
    // // <constructor-arg ref="dataDirectory"/>
    // // </bean>
    // // <alias name="authenticationManager" alias="geoServerSecurityManager"/>
    //    @Bean(name = {"authenticationManager", "geoServerSecurityManager"})
    //    public GeoServerSecurityManager geoServerSecurityManager(GeoServerDataDirectory
    // dataDirectory)
    //            throws Exception {
    //        return new GeoServerSecurityManager(dataDirectory);
    //    }
}
