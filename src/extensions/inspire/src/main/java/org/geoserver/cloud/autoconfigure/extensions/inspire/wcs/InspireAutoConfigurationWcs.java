/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire.wcs;

import org.geoserver.configuration.extension.inspire.InspireConfigurationWcs;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(InspireConfigurationWcs.class)
@ConditionalOnInspireWcs
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class InspireAutoConfigurationWcs {}
