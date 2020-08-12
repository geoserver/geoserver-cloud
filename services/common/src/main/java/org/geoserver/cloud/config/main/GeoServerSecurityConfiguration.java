package org.geoserver.cloud.config.main;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource({"classpath*:/applicationSecurityContext.xml"})
public class GeoServerSecurityConfiguration {}
