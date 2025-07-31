/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.community.flatgeobuf;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@TranspileXmlConfig(locations = "jar:gs-flatgeobuf-.*!/applicationContext.xml")
@Import(FlatGeobufConfiguration_Generated.class)
public class FlatGeobufConfiguration {}
