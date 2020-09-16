package org.geoserver.cloud.autoconfigure.jackson;

import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.geotools.jackson.databind.geojson.GeoToolsGeoJsonModule;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.Module;

/**
 * Spring boot {@link EnableAutoConfiguration @EnableAutoConfiguration} to register GeoTools and
 * jackson databind {@link Module modules}.
 * <p>
 * Configuration enablement is conditional on the presence of {@link GeoToolsFilterModule} on the
 * classpath. Add an explicit dependency on {@code gs-cloud-catalog-support:gt-jackson-bindings} to
 * use it.
 * <p>
 * Spring-boot's default auto configuration does not register all modules in the classpath, despite
 * them being register-able through Jackson's SPI; a configuration like this is needed to set up the
 * application required ones.
 */
@Configuration
@ConditionalOnClass(GeoToolsFilterModule.class)
public class GeoToolsJacksonBindingsAutoConfiguration {


    public @Bean GeoToolsGeoJsonModule geoToolsGeoJsonModule() {
        return new GeoToolsGeoJsonModule();
    }

    public @Bean GeoToolsFilterModule geoToolsFilterModule() {
        return new GeoToolsFilterModule();
    }

}
