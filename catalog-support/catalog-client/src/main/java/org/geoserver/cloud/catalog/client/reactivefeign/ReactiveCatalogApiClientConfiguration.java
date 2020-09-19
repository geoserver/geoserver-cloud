/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import reactivefeign.spring.config.EnableReactiveFeignClients;

@Configuration
@EnableReactiveFeignClients(clients = ReactiveCatalogClient.class)
public class ReactiveCatalogApiClientConfiguration {

    public @Bean feign.Logger.Level feignLoggerLevel(
            @Value("${feign.logger.level:BASIC}") feign.Logger.Level level) {
        return level;
    }

    public @Bean ObjectMapper catalogServiceClientObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    public @Bean MappingJackson2HttpMessageConverter catalogServiceClientHttpMessageConverter() {
        return new MappingJackson2HttpMessageConverter(catalogServiceClientObjectMapper());
    }

    public @Bean ObjectFactory<HttpMessageConverters> catalogServiceClientObjectFactory() {
        ObjectFactory<HttpMessageConverters> objectFactory =
                () -> new HttpMessageConverters(catalogServiceClientHttpMessageConverter());
        return objectFactory;
    }

    // public @Bean HttpMessageConverters catalogServiceClientObjectFactory() {
    // return new HttpMessageConverters(catalogServiceClientHttpMessageConverter());
    // }

    public @Bean Decoder feignDecoder() {
        // return new ResponseEntityDecoder(new SpringDecoder(catalogServiceClientObjectFactory()));
        return new SpringDecoder(catalogServiceClientObjectFactory());
    }

    public @Bean Encoder feignEncoder() {
        return new SpringEncoder(catalogServiceClientObjectFactory());
    }
}
