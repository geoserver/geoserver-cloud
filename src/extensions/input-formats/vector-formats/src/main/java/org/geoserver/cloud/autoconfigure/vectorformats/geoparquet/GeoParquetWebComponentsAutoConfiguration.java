/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.vectorformats.geoparquet;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.web.data.resource.DataStorePanelInfo;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geotools.autoconfigure.vectorformats.DataAccessFactoryFilteringAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for GeoParquet extension that provides a data store
 * configuration panel for the web admin interface.
 *
 * <p>
 * This auto-configuration class enables the GeoParquet extension in GeoServer
 * Cloud. It will be activated when the following conditions are met:
 * <ul>
 * <li>The {@code GeoParquetDataStoreFactory} class is on the classpath</li>
 * <li>The {@literal @ConditionalOnGeoServerWebUI} conditional is satisfied
 * </ul>
 *
 * @since 2.27.0
 */
@AutoConfiguration(after = DataAccessFactoryFilteringAutoConfiguration.class)
@ConditionalOnGeoParquet
@ConditionalOnGeoServerWebUI
@ImportFilteredResource("jar:gs-geoparquet-.*!/applicationContext.xml")
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.vectorformats.geoparquet")
public class GeoParquetWebComponentsAutoConfiguration {

    @PostConstruct
    void log() {
        log.info("GeoParquet WebUI extension installed");
    }

    /**
     * <pre>{@code
     * <bean id="geoParquetDataStorePanel" class="org.geoserver.web.data.resource.DataStorePanelInfo">
     * <property name="id" value="geoParquetDataStorePanel"/>
     * <property name="factoryClass" value="org.geotools.data.geoparquet.GeoParquetDataStoreFactory"/>
     * <property name="iconBase" value="org.geoserver.web.data.store.geoparquet.GeoParquetDataStoreEditPanel"/>
     * <property name="icon" value="geoparquet-icon.svg" />
     * <property name="componentClass" value="org.geoserver.web.data.store.geoparquet.GeoParquetDataStoreEditPanel"/>
     * </bean>
     * }</pre>
     */
    @Bean
    @SuppressWarnings("unchecked")
    DataStorePanelInfo geoParquetDataStorePanel() throws ClassNotFoundException {
        DataStorePanelInfo info = new DataStorePanelInfo();
        info.setId("geoParquetDataStorePanel");
        info.setFactoryClass(org.geotools.data.geoparquet.GeoParquetDataStoreFactory.class);
        info.setIconBase(org.geoserver.web.data.store.geoparquet.GeoParquetDataStoreEditPanel.class);
        info.setIcon("geoparquet-icon.svg");
        info.setComponentClass((Class<StoreEditPanel>)
                Class.forName("org.geoserver.web.data.store.geoparquet.GeoParquetDataStoreEditPanel"));
        return info;
    }
}
