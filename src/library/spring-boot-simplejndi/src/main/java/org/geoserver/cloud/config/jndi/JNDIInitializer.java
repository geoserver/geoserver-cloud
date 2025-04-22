/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.jndi;

import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.jdbc.support.DatabaseStartupValidator;
import org.springframework.util.StringUtils;

/**
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.config.jndidatasource")
public class JNDIInitializer implements InitializingBean, DisposableBean {

    private JNDIDataSourcesConfigurationProperties config;

    JNDIInitializer(JNDIDataSourcesConfigurationProperties config) {
        this.config = config;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        Map<String, JNDIDatasourceConfig> configs = config.getDatasources();

        if (null == configs || configs.isEmpty()) {
            log.info("No JNDI datasources configured");
            return;
        }

        // Assign the datasource a name from the mappings key
        configs.entrySet().forEach(e -> {
            var name = e.getKey();
            var props = e.getValue();
            if (null == props.getName()) {
                props.setName(name);
            }
        });
        try {
            configs.entrySet().forEach(e -> setUpDataSource(toJndiDatasourceName(e.getKey()), e.getValue()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void destroy() throws Exception {
        Map<String, JNDIDatasourceConfig> configs = config.getDatasources();
        if (null == configs || configs.isEmpty()) {
            return;
        }
        Context initialContext;
        try {
            initialContext = getInitialContext();
        } catch (Exception e) {
            log.error("Error getting initial context during destroy", e);
            return;
        }

        configs.entrySet().stream().filter(e -> e.getValue().isEnabled()).forEach(e -> {
            String jndiName = toJndiDatasourceName(e.getKey());
            try {
                HikariDataSource dataSource = (HikariDataSource) initialContext.lookup(jndiName);
                dataSource.close();
            } catch (NamingException ex) {
                log.warn("Error looking up data source {}", jndiName, ex);
            }
        });
    }

    String toJndiDatasourceName(String dsname) {
        final String prefix = "java:comp/env/jdbc/";
        if (!dsname.startsWith(prefix)) {
            if (dsname.contains("/")) {
                throw new IllegalArgumentException(
                        "The datasource name '%s' is invalid. Provide either a simple name, or a full name like java:comp/env/jdbc/mydatasource"
                                .formatted(dsname));
            }
            return prefix + dsname;
        }
        return dsname;
    }

    void setUpDataSource(String jndiName, JNDIDatasourceConfig props) {
        if (props.isEnabled()) {
            log.info("Creating JNDI datasoruce {} on {}", jndiName, props.getUrl());
        } else {
            log.info("Ignoring disabled JNDI datasource " + jndiName);
            return;
        }

        Context initialContext = getInitialContext();

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

    private Context getInitialContext() {
        Context initialContext;
        try {
            initialContext = NamingManager.getInitialContext(null);
        } catch (NamingException e) {
            throw new ApplicationContextException("No JNDI initial context bound", e);
        }
        return initialContext;
    }

    private void waitForIt(String jndiName, DataSource dataSource, JNDIDatasourceConfig props) {
        if (props.isWaitForIt()) {
            log.info("Waiting up to {} seconds for datasource {}", props.getWaitTimeout(), jndiName);
            DatabaseStartupValidator validator = new DatabaseStartupValidator();
            validator.setDataSource(dataSource);
            validator.setTimeout(props.getWaitTimeout());
            validator.afterPropertiesSet();
        }
    }

    protected HikariDataSource createDataSource(JNDIDatasourceConfig props) {
        HikariDataSource dataSource = props.initializeDataSourceBuilder() //
                .type(HikariDataSource.class)
                .build();

        String dataSourceName = props.getName();
        if (null != dataSourceName) {
            dataSource.setPoolName("HikariPool %s".formatted(dataSourceName));
        }
        dataSource.setMaximumPoolSize(props.getMaximumPoolSize());
        dataSource.setMinimumIdle(props.getMinimumIdle());
        dataSource.setConnectionTimeout(props.getConnectionTimeout());
        dataSource.setIdleTimeout(props.getIdleTimeout());
        if (StringUtils.hasLength(props.getSchema())) {
            dataSource.setSchema(props.getSchema());
        }
        return dataSource;
    }
}
