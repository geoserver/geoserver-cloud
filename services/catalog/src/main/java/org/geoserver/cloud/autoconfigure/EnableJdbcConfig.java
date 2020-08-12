package org.geoserver.cloud.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.cloud.autoconfigure.jdbcconfig.JDBCConfigAutoConfiguration;
import org.geoserver.cloud.autoconfigure.jdbcconfig.JDBCStoreAutoConfiguration;
import org.springframework.context.annotation.Import;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({JDBCConfigAutoConfiguration.class, JDBCStoreAutoConfiguration.class})
public @interface EnableJdbcConfig {}
