/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Simple test configuration to refer to in {@link SpringBootTest @SpringBootTest} annotations, does
 * nothing as we rely in auto-configuration by default. Individual tests can enable/disable the
 * desired auto configurations as needed
 */
@Configuration
public class TestConfiguration {}
