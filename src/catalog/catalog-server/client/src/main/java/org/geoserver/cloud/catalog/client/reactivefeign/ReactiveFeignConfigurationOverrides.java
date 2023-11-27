/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import feign.Contract;
import feign.MethodMetadata;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ReactiveFeignConfigurationOverrides {

    @Bean
    Contract reactiveFeignClientContract() {
        return new FallbackContract(new SpringMvcContract(), new Contract.Default());
    }

    @RequiredArgsConstructor
    private static class FallbackContract implements Contract {

        private final @NonNull Contract primary;
        private final @NonNull Contract fallback;

        @Override
        public List<MethodMetadata> parseAndValidateMetadata(Class<?> targetType) {
            try {
                return primary.parseAndValidateMetadata(targetType);
            } catch (RuntimeException e) {
                return fallback.parseAndValidateMetadata(targetType);
            }
        }
    }
}
