/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

/**
 * Simple test configuration to refer to in {@link SpringBootTest @SpringBootTest} annotations, does
 * nothing as we rely in auto-configuration by default. Individual tests can enable/disable the
 * desired auto configurations as needed
 */
@Configuration
@EnableAutoConfiguration(
        exclude = { //
            DataSourceAutoConfiguration.class, //
            DataSourceTransactionManagerAutoConfiguration.class, //
        })
@TestPropertySource(properties = {"geoserver.backend.data-directory.enabled=true"})
public class TestConfiguration {}
