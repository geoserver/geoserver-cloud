/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.wfs;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Transpiled XML configuration from {@literal jar:gs-wfs-.*!/applicationContext.xml}
 *
 * <p>{@code wfsXStreamPersisterInitializer} is excluded: every service reads and writes the cascaded stored query
 * configuration it describes, so it is contributed unconditionally by {@code WfsXStreamPersisterAutoConfiguration}
 * instead of by this service-scoped configuration.
 *
 * @see WFSCoreConfiguration_Generated
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-wfs-.*!/applicationContext.xml",
        publicAccess = true,
        excludes = "wfsXStreamPersisterInitializer")
@Import(WFSCoreConfiguration_Generated.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class WFSCoreConfiguration {}
