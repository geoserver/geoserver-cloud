/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.jndi;

import org.geoserver.cloud.config.jndi.JNDIDataSourceConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * @since 1.0
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Import(JNDIDataSourceConfiguration.class)
public class JNDIDataSourceAutoConfiguration {}
