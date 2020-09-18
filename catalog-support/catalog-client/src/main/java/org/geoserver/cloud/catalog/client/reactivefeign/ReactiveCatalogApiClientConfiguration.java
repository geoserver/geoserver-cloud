/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import org.springframework.context.annotation.Configuration;
import reactivefeign.spring.config.EnableReactiveFeignClients;

@Configuration
@EnableReactiveFeignClients(basePackageClasses = ReactiveCatalogClient.class)
public class ReactiveCatalogApiClientConfiguration {
//
//    public @Bean Logger.Level feignLoggerLevel(
//            @Value("${feign.logger.level:BASIC}") Logger.Level level) {
//        return level;
//    }
//
//    public @Bean ObjectMapper catalogServiceClientObjectMapper() {
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.findAndRegisterModules();
//        return objectMapper;
//    }
//
//    public @Bean MappingJackson2HttpMessageConverter catalogServiceClientHttpMessageConverter() {
//        return new MappingJackson2HttpMessageConverter(catalogServiceClientObjectMapper());
//    }

//    public @Bean ObjectFactory<HttpMessageConverters> catalogServiceClientObjectFactory() {
//        ObjectFactory<HttpMessageConverters> objectFactory =
//                () -> new HttpMessageConverters(catalogServiceClientHttpMessageConverter());
//        return objectFactory;
//    }

//    public @Bean HttpMessageConverters catalogServiceClientObjectFactory() {
//        return new HttpMessageConverters(catalogServiceClientHttpMessageConverter());
//    }
//
//    public @Bean Decoder feignDecoder() {
//        // return new ResponseEntityDecoder(new SpringDecoder(catalogServiceClientObjectFactory()));
//        return new SpringDecoder(catalogServiceClientObjectFactory());
//    }
//
//    public @Bean Encoder feignEncoder() {
//        return new SpringEncoder(catalogServiceClientObjectFactory());
//    }
}
