package org.geoserver.cloud.autoconfigure.jdbcconfig;

import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ConditionalOnClass(JDBCConfigProperties.class)
@ConditionalOnProperty(prefix = "geoserver.jdbcconfig", name = "enabled", matchIfMissing = true)
@ImportResource({
    // "jar:file:/home/groldan/.m2/repository/org/geoserver/community/gs-jdbcstore/2.18-SNAPSHOT/gs-jdbcstore-2.18-SNAPSHOT.jar!/applicationContext.xml",//
})
public class JDBCStoreAutoConfiguration {}
