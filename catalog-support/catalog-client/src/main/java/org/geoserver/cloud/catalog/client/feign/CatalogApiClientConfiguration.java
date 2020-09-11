/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import feign.Logger;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackageClasses = CatalogApiClient.class)
public class CatalogApiClientConfiguration {

    private ObjectFactory<HttpMessageConverters> messageConverters = HttpMessageConverters::new;

    public @Bean Logger.Level feignLoggerLevel(
            @Value("${feign.logger.level:BASIC}") Logger.Level level) {
        return level;
    }

    public @Bean Encoder feignEncoder() {
        return new SpringEncoder(messageConverters);
    }

    public @Bean Decoder feignDecoder() {
        return new SpringDecoder(messageConverters);
    }
}
