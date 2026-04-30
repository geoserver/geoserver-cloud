/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.simplejndi;

import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.StructuredTaskScope;
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
 * Bean that builds a {@link HikariDataSource} for each enabled {@link JNDIDatasourceProperties} entry, binds it to the
 * JNDI initial context under {@code java:comp/env/jdbc/<name>}, and closes the pools on shutdown.
 *
 * <p>Datasources are initialized concurrently using a {@link StructuredTaskScope} so that
 * {@link JNDIDatasourceProperties#isWaitForIt() waitForIt} delays do not serialize across configured datasources.
 *
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.config.jndidatasource")
class JNDIInitializer implements InitializingBean, DisposableBean {

    private JNDIDataSourcesConfigurationProperties config;

    JNDIInitializer(JNDIDataSourcesConfigurationProperties config) {
        this.config = config;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, JNDIDatasourceProperties> configs = config.getDatasources();

        if (null == configs || configs.isEmpty()) {
            log.info("No JNDI datasources configured");
            return;
        }

        // Assign the datasource a name from the mappings key
        configs.forEach((name, props) -> {
            if (null == props.getName()) {
                props.setName(name);
            }
        });

        Context initialContext = getInitialContext();

        try (@SuppressWarnings("preview")
                StructuredTaskScope<Object, Void> scope = StructuredTaskScope.open()) {
            for (JNDIDatasourceProperties props : configs.values()) {
                scope.fork(() -> setUpDataSource(initialContext, props));
            }
            scope.join();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void destroy() throws Exception {
        Map<String, JNDIDatasourceProperties> configs = config.getDatasources();
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

    void setUpDataSource(Context initialContext, JNDIDatasourceProperties props) {
        final String jndiName = toJndiDatasourceName(Objects.requireNonNull(props.getName()));
        if (props.isEnabled()) {
            log.info("Creating JNDI datasoruce {} on {}", jndiName, props.getUrl());
        } else {
            log.info("Ignoring disabled JNDI datasource " + jndiName);
            return;
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

    private Context getInitialContext() {
        Context initialContext;
        try {
            initialContext = NamingManager.getInitialContext(null);
        } catch (NamingException e) {
            throw new ApplicationContextException("No JNDI initial context bound", e);
        }
        return initialContext;
    }

    private void waitForIt(String jndiName, DataSource dataSource, JNDIDatasourceProperties props) {
        if (props.isWaitForIt()) {
            log.info("Waiting up to {} seconds for datasource {}", props.getWaitTimeout(), jndiName);
            DatabaseStartupValidator validator = new DatabaseStartupValidator();
            validator.setDataSource(dataSource);
            validator.setTimeout(props.getWaitTimeout());
            validator.afterPropertiesSet();
        }
    }

    protected HikariDataSource createDataSource(JNDIDatasourceProperties props) {
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
