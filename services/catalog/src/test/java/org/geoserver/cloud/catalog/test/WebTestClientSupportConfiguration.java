/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.test;

import org.geoserver.catalog.CatalogInfo;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Configures the {@link WebTestClient} to be able of encoding and decoding {@link CatalogInfo}
 * obejcts using {@link CatalogInfoXmlEncoder} and {@link CatalogInfoXmlDecoder}
 */
@AutoConfigureWebTestClient(timeout = "360000")
public class WebTestClientSupportConfiguration {

    public @Bean WebTestClientSupport webTestClientSupport() {
        return new WebTestClientSupport();
    }
}
