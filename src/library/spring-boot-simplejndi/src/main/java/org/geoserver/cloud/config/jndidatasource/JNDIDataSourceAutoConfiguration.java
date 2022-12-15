/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jndidatasource;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.DatabaseStartupValidator;

import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;

/**
 * @since 1.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(JNDIDataSourcesConfigurationProperties.class)
public class JNDIDataSourceAutoConfiguration implements InitializingBean {

    private @Autowired JNDIDataSourcesConfigurationProperties config;

    @Override
    public void afterPropertiesSet() throws Exception {

        Map<String, JNDIDatasourceConfig> configs = config.getDatasources();

        if (null == configs || configs.isEmpty()) {
            log.info("No JNDI datasources configured");
            return;
        }

        configs.entrySet()
                .forEach(e -> setUpDataSource(toJndiDatasourceName(e.getKey()), e.getValue()));
    }

    String toJndiDatasourceName(String dsname) {
        final String prefix = "java:comp/env/jdbc/";
        if (!dsname.startsWith(prefix)) {
            if (dsname.contains("/")) {
                throw new IllegalArgumentException(
                        "The datasource name '"
                                + dsname
                                + "' is invalid. Provide either a simple name, or a full name like java:comp/env/jdbc/mydatasource");
            }
            return prefix + dsname;
        }
        return dsname;
    }

    void setUpDataSource(String jndiName, JNDIDatasourceConfig props) {
        if (props.isEnabled()) {
            log.info("Creating JNDI datasoruce " + jndiName + " on " + props.getUrl());
        } else {
            log.info("Ignoring disabled JNDI datasource " + jndiName);
            return;
        }

        Context initialContext;
        try {
            initialContext = NamingManager.getInitialContext(null);
        } catch (NamingException e) {
            throw new ApplicationContextException("No JNDI initial context bound", e);
        }

        DataSource dataSource = createDataSource(props);
        waitForIt(jndiName, dataSource, props);
        try {
            initialContext.bind(jndiName, dataSource);
            log.info(
                    "Bound JNDI datasource {}: url: {}, user: {}, max size: {}, min size: {}, connection timeout: {}, idle timeout: {}",
                    jndiName,
                    props.getUrl(),
                    props.getUsername(),
                    props.getMaximumPoolSize(),
                    props.getMinimumIdle(),
                    props.getConnectionTimeout(),
                    props.getIdleTimeout());
        } catch (NamingException e) {
            throw new ApplicationContextException("Error binding JNDI datasource " + jndiName, e);
        }
    }

    private void waitForIt(String jndiName, DataSource dataSource, JNDIDatasourceConfig props) {
        if (props.isWaitForIt()) {
            log.info(
                    "Waiting up to {} seconds for datasource {}", props.getWaitTimeout(), jndiName);
            DatabaseStartupValidator validator = new DatabaseStartupValidator();
            validator.setDataSource(dataSource);
            validator.setTimeout(props.getWaitTimeout());
            validator.afterPropertiesSet();
        }
    }

    protected DataSource createDataSource(JNDIDatasourceConfig props) {
        HikariDataSource dataSource =
                props.initializeDataSourceBuilder() //
                        .type(HikariDataSource.class)
                        .build();

        dataSource.setMaximumPoolSize(props.getMaximumPoolSize());
        dataSource.setMinimumIdle(props.getMinimumIdle());
        dataSource.setConnectionTimeout(props.getConnectionTimeout());
        dataSource.setIdleTimeout(props.getIdleTimeout());
        return dataSource;
    }
}
