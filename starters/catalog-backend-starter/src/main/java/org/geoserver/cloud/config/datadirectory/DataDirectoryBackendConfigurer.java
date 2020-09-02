package org.geoserver.cloud.config.datadirectory;

import java.io.File;
import java.nio.file.Path;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.config.catalog.GeoServerBackendConfigurer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** */
@Configuration(proxyBeanMethods = true)
@Slf4j
public class DataDirectoryBackendConfigurer implements GeoServerBackendConfigurer {

    private @Autowired @Getter ApplicationContext context;

    public @PostConstruct void log() {
        log.info("Loading geoserver config backend with {}", getClass().getSimpleName());
    }

    @ConfigurationProperties(prefix = "geoserver.backend.data-directory")
    public @Bean DataDirectoryProperties dataDirectoryProperties() {
        DataDirectoryProperties dataDirectoryProperties = new DataDirectoryProperties();
        return dataDirectoryProperties;
    }

    public @Override @Bean GeoServerResourceLoader resourceLoader() {
        ResourceStore resourceStore = resourceStoreImpl();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(resourceStore);
        DataDirectoryProperties props = dataDirectoryProperties();
        log.debug("geoserver.backend.data-directory.location:" + props.getLocation());
        Path path = props.getLocation();
        File dataDirectory = path.toFile();
        resourceLoader.setBaseDirectory(dataDirectory);
        return resourceLoader;
    }

    public @Override @Bean ResourceStore resourceStoreImpl() {
        DataDirectoryProperties props = dataDirectoryProperties();
        Path path = props.getLocation();
        File dataDirectory = path.toFile();
        return new NoServletContextDataDirectoryResourceStore(dataDirectory);
    }
}
