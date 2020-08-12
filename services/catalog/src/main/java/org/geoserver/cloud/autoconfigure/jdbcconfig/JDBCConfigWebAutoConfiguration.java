package org.geoserver.cloud.autoconfigure.jdbcconfig;

import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.jdbcconfig.web.JDBCConfigStatusProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.geoserver.web.GeoServerHomePageContentProvider")
public class JDBCConfigWebAutoConfiguration {

    @Bean("JDBCConfigStatusProvider")
    public JDBCConfigStatusProvider jdbcConfigStatusProvider(JDBCConfigProperties config) {
        return new JDBCConfigStatusProvider(config);
    }
}
