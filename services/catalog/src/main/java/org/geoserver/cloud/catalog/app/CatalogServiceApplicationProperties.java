/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.app;

import lombok.Data;

/**
 * {@code catalog-service} specific configuration properties bean, expected to match the {@code
 * geoserver.catalog-service} properties prefix
 */
public @Data class CatalogServiceApplicationProperties {

    private SchedulerConfig ioThreads = new SchedulerConfig();

    public static @Data class SchedulerConfig {
        public static final int DEFAULT_MAX_SIZE =
                Math.max(4, 4 * Runtime.getRuntime().availableProcessors());
        public static final int DEFAULT_MAX_QUEUED = Integer.MAX_VALUE;

        Integer maxSize = DEFAULT_MAX_SIZE;
        Integer maxQueued = DEFAULT_MAX_QUEUED;

        public Integer getMaxSize() {
            return maxSize == null ? DEFAULT_MAX_SIZE : maxSize;
        }

        public Integer getMaxQueued() {
            return maxQueued == null ? DEFAULT_MAX_QUEUED : maxQueued;
        }

        public static String buildInvalidMaxSizeMessage(int providedMaxThreadsValue) {
            return String.format(
                    "Ivalid value for geoserver.catalog-service.io-threads.max-size=%d, using default value of 4*cores (%d)",
                    providedMaxThreadsValue, SchedulerConfig.DEFAULT_MAX_SIZE);
        }

        public static String buildInvalidMaxQueuedMessage(int maxQueued) {
            return String.format(
                    "Ivalid value for geoserver.catalog-service.io-threads.max-queued=%d, using default unbounded queue",
                    maxQueued);
        }
    }
}
