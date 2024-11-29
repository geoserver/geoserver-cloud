/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto;

import lombok.Data;
import org.geoserver.config.CoverageAccessInfo;

/** DTO for {@link CoverageAccessInfo} */
@Data
public class CoverageAccess {
    public enum QueueType {
        UNBOUNDED,
        DIRECT
    }

    private int corePoolSize;
    private int keepAliveTime;
    private int maxPoolSize;
    private QueueType queueType;
    private long imageIOCacheThreshold;
}
